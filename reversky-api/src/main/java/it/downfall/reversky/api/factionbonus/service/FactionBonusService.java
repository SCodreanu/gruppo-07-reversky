package it.downfall.reversky.api.factionbonus.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusRequest;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusResponse;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusSummaryResponse;
import it.downfall.reversky.api.factionbonus.entity.FactionBonus;
import it.downfall.reversky.api.factionbonus.entity.FactionBonusId;
import it.downfall.reversky.api.factionbonus.exception.FactionBonusDuplicateException;
import it.downfall.reversky.api.factionbonus.exception.FactionBonusFactionNotFoundException;
import it.downfall.reversky.api.factionbonus.exception.FactionBonusRulesViolatedException;
import it.downfall.reversky.api.factionbonus.exception.FactionBonusSelfMatchupException;
import it.downfall.reversky.api.factionbonus.repository.FactionBonusRepository;
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

		factionBonusRepository.deleteAllInBatch();
		factionBonusRepository.flush();

		List<FactionBonus> toSave = requests.stream()
			.map(r -> new FactionBonus(r.originFactionId(), r.targetFactionId(), r.atkBonus(), r.defBonus()))
			.toList();

		List<FactionBonus> saved = factionBonusRepository.saveAll(toSave);

		return saved.stream()
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