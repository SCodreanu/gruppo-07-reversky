package it.downfall.reversky.api.ruleset.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.factionbonus.dto.FactionBonusSummaryResponse;
import it.downfall.reversky.api.factionbonus.service.FactionBonusService;
import it.downfall.reversky.api.ruleset.dto.RuleSetPublicResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetSummaryResponse;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.exception.NoActiveRuleSetException;
import it.downfall.reversky.api.ruleset.exception.RuleSetActivationConflictException;
import it.downfall.reversky.api.ruleset.exception.RuleSetInUseException;
import it.downfall.reversky.api.ruleset.exception.RuleSetNotFoundException;
import it.downfall.reversky.api.ruleset.exception.RuleSetRulesViolatedException;
import it.downfall.reversky.api.ruleset.exception.RuleSetVersionAlreadyUsedException;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import lombok.RequiredArgsConstructor;

/**
 * Regole di gioco versionate (docs/backoffice, "Regole di gioco — rule_sets").
 * <p>
 * I controlli ripetono i CHECK del database, che restano come rete di sicurezza: version univoca,
 * punti vita e round ≥ 1 e crediti ≥ 0 (annotazioni del DTO), somma delle parti del mazzo uguale a
 * deckSize (qui). Una versione già usata da una partita non si modifica.
 * <p>
 * {@code findActive} allega alla risposta anche la matrice corrente dei bonus di fazione
 * (vedi {@link FactionBonusService}): GET /rules è il punto unico in cui client ed engine leggono
 * regole e bonus insieme.
 */
@Service
@RequiredArgsConstructor
public class RuleSetService {

	private final RuleSetRepository ruleSetRepository;
	private final FactionBonusService factionBonusService;
	private final Clock clock;

	/**
	 * Tutte le versioni, la più recente per prima (GET /admin/rule-sets).
	 */
	@Transactional(readOnly = true)
	public List<RuleSetSummaryResponse> findAll() {
		return ruleSetRepository.findAllByOrderByIdDesc()
			.stream()
			.map(RuleSetSummaryResponse::from)
			.toList();
	}

	/**
	 * La versione attiva, per la pagina del regolamento (GET /rules). Include la matrice dei
	 * bonus di fazione corrente.
	 *
	 * @throws NoActiveRuleSetException se nessuna versione è attiva
	 */
	@Transactional(readOnly = true)
	public RuleSetPublicResponse findActive() {
		RuleSet active = ruleSetRepository.findByIsActiveTrue()
			.orElseThrow(() -> new NoActiveRuleSetException());

		List<FactionBonusSummaryResponse> factionBonuses = factionBonusService.findAllSummaries();

		return RuleSetPublicResponse.from(active, factionBonuses);
	}

	/**
	 * Crea una nuova versione, disattivata.
	 *
	 * @param createdById admin che la crea; per ora sempre nullo (il backoffice non registra ancora l'autore)
	 * @throws RuleSetRulesViolatedException      se le parti del mazzo non sommano a deckSize
	 * @throws RuleSetVersionAlreadyUsedException se la versione è già usata
	 */
	@Transactional
	public RuleSetResponse create(RuleSetRequest request, Integer createdById) {
		validateDeckParts(request);

		if (ruleSetRepository.existsByVersion(request.version())) {
			throw new RuleSetVersionAlreadyUsedException(request.version());
		}

		RuleSet createdRuleSet = ruleSetRepository.save(new RuleSet(
			request.version(),
			request.startingLifePoints(),
			request.maxRounds(),
			request.deckSize(),
			request.deckCreatures(),
			request.deckSpells(),
			request.deckHidden(),
			request.deckItems(),
			request.deckTerrains(),
			request.maxHiddenSurvival(),
			request.maxHiddenDamage(),
			request.signupBonus(),
			request.winReward(),
			request.drawReward(),
			Instant.now(clock),
			createdById));

		return RuleSetResponse.from(createdRuleSet);
	}

	/**
	 * Modifica una versione mai usata da una partita. Anche la versione attiva si può modificare,
	 * finché nessuna partita l'ha usata.
	 *
	 * @throws RuleSetNotFoundException           se il rule set non esiste
	 * @throws RuleSetInUseException              se una partita l'ha già usato
	 * @throws RuleSetRulesViolatedException      se le parti del mazzo non sommano a deckSize
	 * @throws RuleSetVersionAlreadyUsedException se la versione è già usata da un altro rule set
	 */
	@Transactional
	public RuleSetResponse update(Integer id, RuleSetRequest request) {
		RuleSet selectedRuleSet = ruleSetRepository.findById(id)
			.orElseThrow(() -> new RuleSetNotFoundException());

		if (ruleSetRepository.isUsedByBattle(id)) {
			throw new RuleSetInUseException();
		}

		validateDeckParts(request);

		if (ruleSetRepository.existsByVersionAndIdNot(request.version(), id)) {
			throw new RuleSetVersionAlreadyUsedException(request.version());
		}

		selectedRuleSet.update(
			request.version(),
			request.startingLifePoints(),
			request.maxRounds(),
			request.deckSize(),
			request.deckCreatures(),
			request.deckSpells(),
			request.deckHidden(),
			request.deckItems(),
			request.deckTerrains(),
			request.maxHiddenSurvival(),
			request.maxHiddenDamage(),
			request.signupBonus(),
			request.winReward(),
			request.drawReward());

		return RuleSetResponse.from(selectedRuleSet);
	}

	/**
	 * Rende attiva una versione e disattiva la precedente, nella stessa transazione.
	 * Attivare la versione già attiva non cambia niente.
	 * <p>
	 * La versione attiva viene bloccata: due attivazioni contemporanee si mettono in fila e vince
	 * l'ultima. Se nessuna versione era attiva non c'è niente da bloccare, e la seconda attivazione
	 * viola {@code uq_rule_sets_one_active}: diventa un 409 invece di un 500.
	 *
	 * @throws RuleSetNotFoundException           se il rule set non esiste
	 * @throws RuleSetActivationConflictException se un'altra attivazione è avvenuta nello stesso momento
	 */
	@Transactional
	public RuleSetResponse activate(Integer id) {
		RuleSet ruleSetToActivate = ruleSetRepository.findById(id)
			.orElseThrow(() -> new RuleSetNotFoundException());

		if (ruleSetToActivate.isActive()) {
			return RuleSetResponse.from(ruleSetToActivate);
		}

		// La disattivazione va scritta prima dell'attivazione, altrimenti per un attimo
		// ci sarebbero due righe attive e l'indice parziale rifiuterebbe l'UPDATE.
		ruleSetRepository.findActiveForUpdate().ifPresent(previousRuleSet -> {
			previousRuleSet.deactivate();
			ruleSetRepository.flush();
		});

		ruleSetToActivate.activate();
		try {
			ruleSetRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new RuleSetActivationConflictException();
		}

		return RuleSetResponse.from(ruleSetToActivate);
	}

	private void validateDeckParts(RuleSetRequest request) {
		int partsSum = request.deckCreatures() + request.deckSpells() + request.deckHidden()
			+ request.deckItems() + request.deckTerrains();

		if (partsSum != request.deckSize()) {
			throw new RuleSetRulesViolatedException(
				"Creatures + Spells + Hidden + Items + Terrains (" + partsSum
					+ ") must equal the deck size (" + request.deckSize() + ")");
		}
	}
}