package it.downfall.reversky.api.catalog.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.FactionBonusRequest;
import it.downfall.reversky.api.catalog.dto.FactionBonusResponse;
import it.downfall.reversky.api.catalog.dto.FactionBonusSummaryResponse;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.FactionBonus;
import it.downfall.reversky.api.catalog.entity.FactionBonusId;
import it.downfall.reversky.api.catalog.exception.FactionBonusDuplicateException;
import it.downfall.reversky.api.catalog.exception.FactionBonusFactionNotFoundException;
import it.downfall.reversky.api.catalog.exception.FactionBonusRulesViolatedException;
import it.downfall.reversky.api.catalog.exception.FactionBonusSelfMatchupException;
import it.downfall.reversky.api.catalog.repository.FactionBonusRepository;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Matrice dei bonus/malus tra fazioni (docs/backoffice, "Dati di riferimento").
 * <p>
 * Il PUT è collection-level: sostituisce l'intera matrice in una sola transazione.
 * Validazioni (equivalenti ai CHECK del database, che comunque restano come rete di sicurezza):
 * <ul>
 *   <li>{@code origin != target} ({@link FactionBonusSelfMatchupException});</li>
 *   <li>{@code defBonus >= 0} ({@link FactionBonusRulesViolatedException});</li>
 *   <li>le due fazioni esistono ({@link FactionBonusFactionNotFoundException});</li>
 *   <li>nessuna coppia duplicata nel body ({@link FactionBonusDuplicateException}).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FactionBonusService {

	private final FactionBonusRepository factionBonusRepository;
	private final FactionRepository factionRepository;

	/** GET /admin/faction-bonuses: matrice completa, con i nomi delle fazioni. */
	@Transactional(readOnly = true)
	public List<FactionBonusResponse> findAll() {
		Map<Integer, String> names = loadFactionNames();
		return factionBonusRepository.findAllOrdered()
			.stream()
			.map(b -> FactionBonusResponse.from(
				b,
				names.get(b.getOriginFactionId()),
				names.get(b.getTargetFactionId())))
			.toList();
	}

	/**
	 * PUT /admin/faction-bonuses: sostituisce l'intera matrice. Il body vuoto (o {@code null})
	 * svuota la tabella.
	 * <p>
	 * Tutte le righe si controllano prima di toccare la tabella: se una è sbagliata la matrice resta
	 * quella di prima. Due sostituzioni contemporanee si mettono in fila e vince l'ultima
	 * (vedi {@link FactionBonusRepository#lockForReplace()}).
	 *
	 * @throws FactionBonusRulesViolatedException   se una riga è {@code null} o ha {@code defBonus} negativo
	 * @throws FactionBonusSelfMatchupException     se una riga abbina una fazione a se stessa
	 * @throws FactionBonusFactionNotFoundException se una fazione non esiste
	 * @throws FactionBonusDuplicateException       se la stessa coppia compare due volte
	 */
	@Transactional
	public List<FactionBonusResponse> replaceAll(List<FactionBonusRequest> requests) {
		if (requests == null) {
			requests = List.of();
		}

		Map<Integer, String> names = loadFactionNames();

		Set<FactionBonusId> seen = new HashSet<>();
		for (FactionBonusRequest request : requests) {
			validateOne(request, names, seen);
		}

		factionBonusRepository.lockForReplace();
		factionBonusRepository.deleteAllInBatch();
		factionBonusRepository.flush();

		List<FactionBonus> toSave = requests.stream()
			.map(r -> new FactionBonus(r.originFactionId(), r.targetFactionId(), r.atkBonus(), r.defBonus()))
			.toList();

		List<FactionBonus> saved = factionBonusRepository.saveAll(toSave);

		// Stesso ordine di GET /admin/faction-bonuses, non quello del body
		return saved.stream()
			.sorted(Comparator.comparing(FactionBonus::getOriginFactionId).thenComparing(FactionBonus::getTargetFactionId))
			.map(b -> FactionBonusResponse.from(
				b,
				names.get(b.getOriginFactionId()),
				names.get(b.getTargetFactionId())))
			.toList();
	}

	/**
	 * Vista compatta per GET /rules: solo id e valori, niente nomi (l'engine li join-a con
	 * le fazioni quando serve).
	 */
	@Transactional(readOnly = true)
	public List<FactionBonusSummaryResponse> findAllSummaries() {
		return factionBonusRepository.findAllOrdered()
			.stream()
			.map(FactionBonusSummaryResponse::from)
			.toList();
	}

	// --- supporto ---

	private Map<Integer, String> loadFactionNames() {
		Map<Integer, String> names = new HashMap<>();
		for (Faction faction : factionRepository.findAll()) {
			names.put(faction.getId(), faction.getName());
		}
		return names;
	}

	private void validateOne(FactionBonusRequest request, Map<Integer, String> names, Set<FactionBonusId> seen) {
		// Dal controller non arriva mai (List<@NotNull @Valid ...>): serve a chi chiama il service
		// direttamente, che altrimenti riceverebbe un NullPointerException
		if (request == null || request.originFactionId() == null || request.targetFactionId() == null
				|| request.atkBonus() == null || request.defBonus() == null) {
			throw new FactionBonusRulesViolatedException("Every faction bonus row needs origin, target, ATK and DEF bonus");
		}
		if (request.originFactionId().equals(request.targetFactionId())) {
			throw new FactionBonusSelfMatchupException(request.originFactionId());
		}
		if (request.defBonus() < 0) {
			throw new FactionBonusRulesViolatedException(
				"DEF bonus must not be negative (got " + request.defBonus() + ")");
		}
		if (!names.containsKey(request.originFactionId())) {
			throw new FactionBonusFactionNotFoundException(request.originFactionId());
		}
		if (!names.containsKey(request.targetFactionId())) {
			throw new FactionBonusFactionNotFoundException(request.targetFactionId());
		}

		FactionBonusId id = new FactionBonusId(request.originFactionId(), request.targetFactionId());
		if (!seen.add(id)) {
			throw new FactionBonusDuplicateException(request.originFactionId(), request.targetFactionId());
		}
	}
}
