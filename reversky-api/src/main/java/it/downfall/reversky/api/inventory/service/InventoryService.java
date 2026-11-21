package it.downfall.reversky.api.inventory.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.inventory.dto.InventoryResponse;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.entity.CreditTransaction;
import it.downfall.reversky.api.inventory.entity.Inventory;
import it.downfall.reversky.api.inventory.exception.InsufficientCreditsException;
import it.downfall.reversky.api.inventory.exception.InventoryNotFoundException;
import it.downfall.reversky.api.inventory.repository.CreditTransactionRepository;
import it.downfall.reversky.api.inventory.repository.InventoryRepository;
import it.downfall.reversky.api.ruleset.exception.NoActiveRuleSetException;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Inventario e crediti (docs/crediti): l'unico punto che modifica {@code inventories.credits}.
 * <p>
 * Ogni modifica del saldo blocca prima la riga dell'inventario e scrive la sua riga di {@code credit_transactions}
 * nella stessa transazione. Un'operazione che tocca più utenti chiama prima {@link #lockInventories}, che blocca le
 * righe in ordine di {@code user_id} crescente, così due operazioni incrociate non vanno in deadlock.
 * <p>
 * Non dipende da {@code UserService}: è {@code UserService} a chiamare questo service per i crediti di /users/me.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

	private final InventoryRepository inventoryRepository;
	private final CreditTransactionRepository creditTransactionRepository;
	private final RuleSetService ruleSetService;
	private final Clock clock;

	/**
	 * Crea l'inventario di un nuovo utente con i crediti di benvenuto del rule set attivo e la riga {@code SIGNUP}.
	 * <p>
	 * Gira nella transazione della registrazione: se fallisce, l'utente non viene creato.
	 *
	 * @param userId id dell'utente appena salvato
	 * @throws NoActiveRuleSetException se nessun rule set è attivo
	 */
	@Transactional
	public void createInventory(Integer userId) {
		int signupBonus = ruleSetService.getActiveRuleSet().getSignupBonus();

		inventoryRepository.save(new Inventory(userId, signupBonus));
		creditTransactionRepository.save(new CreditTransaction(userId, signupBonus, CreditReason.SIGNUP, Instant.now(clock)));
	}

	/**
	 * Inventario dell'utente autenticato (GET /api/v1/inventory).
	 *
	 * @throws InventoryNotFoundException se l'utente non ha un inventario
	 */
	@Transactional(readOnly = true)
	public InventoryResponse getInventory(Integer userId) {
		return InventoryResponse.from(findInventory(userId));
	}

	/**
	 * Saldo dell'utente, senza bloccare la riga (per /users/me).
	 *
	 * @throws InventoryNotFoundException se l'utente non ha un inventario
	 */
	@Transactional(readOnly = true)
	public int getCredits(Integer userId) {
		return findInventory(userId).getCredits();
	}

	/**
	 * Blocca gli inventari di tutti gli utenti coinvolti in un'operazione, in ordine di {@code user_id} crescente.
	 * <p>
	 * Va chiamato all'inizio di ogni operazione che sposta crediti, carte o pacchetti tra più utenti (scambi, pareggi
	 * in classificata). I blocchi durano fino alla fine della transazione del chiamante.
	 *
	 * @throws InventoryNotFoundException se uno degli utenti non ha un inventario
	 */
	@Transactional
	public void lockInventories(Collection<Integer> userIds) {
		List<Integer> orderedIds = userIds.stream().distinct().sorted().toList();
		for (Integer userId : orderedIds) {
			lockInventory(userId);
		}
	}

	/**
	 * Aggiunge crediti e scrive il movimento.
	 *
	 * @param amount crediti da aggiungere, 0 o più
	 * @return il nuovo saldo
	 * @throws IllegalArgumentException   se l'importo è negativo (errore di programmazione)
	 * @throws InventoryNotFoundException se l'utente non ha un inventario
	 */
	@Transactional
	public int addCredits(Integer userId, int amount, CreditReason reason) {
		requireNotNegative(amount);

		Inventory inventory = lockInventory(userId);
		inventory.addCredits(amount);
		creditTransactionRepository.save(new CreditTransaction(userId, amount, reason, Instant.now(clock)));

		return inventory.getCredits();
	}

	/**
	 * Scala crediti e scrive il movimento con importo negativo (0 per un pacchetto gratuito).
	 *
	 * @param amount crediti da togliere, 0 o più
	 * @return il nuovo saldo
	 * @throws IllegalArgumentException      se l'importo è negativo (errore di programmazione)
	 * @throws InsufficientCreditsException  se il saldo non basta: niente viene modificato
	 * @throws InventoryNotFoundException    se l'utente non ha un inventario
	 */
	@Transactional
	public int spendCredits(Integer userId, int amount, CreditReason reason) {
		requireNotNegative(amount);

		Inventory inventory = lockInventory(userId);
		if (inventory.getCredits() < amount) {
			throw new InsufficientCreditsException(amount, inventory.getCredits());
		}
		inventory.removeCredits(amount);
		creditTransactionRepository.save(new CreditTransaction(userId, -amount, reason, Instant.now(clock)));

		return inventory.getCredits();
	}

	/**
	 * Correzione del saldo dal backoffice (POST /api/v1/admin/users/{id}/credits), con riga {@code ADMIN_ADJUSTMENT}.
	 *
	 * @param amount positivo per aggiungere, negativo per togliere; mai 0 (lo controlla la richiesta)
	 * @param reason motivo scritto dall'admin
	 * @return il saldo dopo la correzione
	 * @throws InsufficientCreditsException se la correzione porterebbe il saldo sotto zero
	 * @throws InventoryNotFoundException   se l'utente non esiste
	 */
	@Transactional
	public InventoryResponse adjustCredits(Integer userId, int amount, String reason) {
		if (amount >= 0) {
			addCredits(userId, amount, CreditReason.ADMIN_ADJUSTMENT);
		} else {
			spendCredits(userId, -amount, CreditReason.ADMIN_ADJUSTMENT);
		}

		// TODO(@tommasofatticcioni): scrivere il motivo in audit_logs.new_values con API-52 (#122), al posto del log
		log.info("Credits of user {} adjusted by {}: {}", userId, amount, reason);

		return InventoryResponse.from(findInventory(userId));
	}

	private Inventory findInventory(Integer userId) {
		return inventoryRepository.findById(userId)
			.orElseThrow(() -> new InventoryNotFoundException(userId));
	}

	private Inventory lockInventory(Integer userId) {
		return inventoryRepository.findByUserIdForUpdate(userId)
			.orElseThrow(() -> new InventoryNotFoundException(userId));
	}

	private void requireNotNegative(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Amount must not be negative: " + amount);
		}
	}
}
