package it.downfall.reversky.api.ruleset.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.ruleset.dto.RuleSetCreateRequest;
import it.downfall.reversky.api.ruleset.dto.RuleSetPublicResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetSummaryResponse;
import it.downfall.reversky.api.ruleset.dto.RuleSetUpdateRequest;
import it.downfall.reversky.api.ruleset.entity.RuleSet;
import it.downfall.reversky.api.ruleset.exception.NoActiveRuleSetException;
import it.downfall.reversky.api.ruleset.exception.RuleSetInUseException;
import it.downfall.reversky.api.ruleset.exception.RuleSetNotFoundException;
import it.downfall.reversky.api.ruleset.exception.RuleSetRulesViolatedException;
import it.downfall.reversky.api.ruleset.exception.RuleSetVersionAlreadyUsedException;
import it.downfall.reversky.api.ruleset.repository.RuleSetRepository;
import lombok.RequiredArgsConstructor;

/**
 * Regole di gioco versionate (docs/backoffice, "Regole di gioco — rule_sets").
 * <p>
 * Validazioni (equivalenti ai CHECK del database, che comunque restano come rete di sicurezza):
 * <ul>
 *   <li>{@code version} univoca ({@link RuleSetVersionAlreadyUsedException});</li>
 *   <li>{@code startingLifePoints}, {@code maxRounds} ≥ 1 (annotazioni DTO);</li>
 *   <li>creature + spell + hidden + items + terrains = {@code deckSize}
 *       ({@link RuleSetRulesViolatedException});</li>
 *   <li>crediti ≥ 0 (annotazioni DTO);</li>
 *   <li>PUT solo su un rule set mai usato ({@link RuleSetInUseException}).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RuleSetService {

	private final RuleSetRepository ruleSetRepository;

	/** GET /admin/rule-sets: tutte le versioni, la più recente in cima. */
	@Transactional(readOnly = true)
	public List<RuleSetSummaryResponse> findAll() {
		return ruleSetRepository.findAllByOrderByIdDesc()
			.stream()
			.map(RuleSetSummaryResponse::from)
			.toList();
	}

	/** POST /admin/rule-sets: nasce disattivato. */
	@Transactional
	public RuleSetResponse create(RuleSetCreateRequest request, Integer createdById) {
		if (ruleSetRepository.existsByVersion(request.version())) {
			throw new RuleSetVersionAlreadyUsedException(request.version());
		}

		validateDeckSum(
			request.deckCreatures(),
			request.deckSpells(),
			request.deckHidden(),
			request.deckItems(),
			request.deckTerrains(),
			request.deckSize());

		RuleSet created = ruleSetRepository.save(new RuleSet(
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
			createdById));

		return RuleSetResponse.from(created);
	}

	/** PUT /admin/rule-sets/{id}: solo se non è mai stato usato da una partita. */
	@Transactional
	public RuleSetResponse update(Integer id, RuleSetUpdateRequest request) {
		RuleSet selected = ruleSetRepository.findById(id)
			.orElseThrow(RuleSetNotFoundException::new);

		if (ruleSetRepository.isUsedByBattle(id)) {
			throw new RuleSetInUseException();
		}

		if (ruleSetRepository.existsByVersionAndIdNot(request.version(), id)) {
			throw new RuleSetVersionAlreadyUsedException(request.version());
		}

		validateDeckSum(
			request.deckCreatures(),
			request.deckSpells(),
			request.deckHidden(),
			request.deckItems(),
			request.deckTerrains(),
			request.deckSize());

		selected.update(
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

		return RuleSetResponse.from(selected);
	}

	/**
	 * POST /admin/rule-sets/{id}/activate: in una sola transazione disattiva l'eventuale
	 * attivo precedente e attiva il nuovo, così l'indice parziale non viene mai violato.
	 * Se il rule set è già attivo, l'operazione è idempotente.
	 */
	@Transactional
	public RuleSetResponse activate(Integer id) {
		RuleSet toActivate = ruleSetRepository.findById(id)
			.orElseThrow(RuleSetNotFoundException::new);

		if (Boolean.TRUE.equals(toActivate.getIsActive())) {
			return RuleSetResponse.from(toActivate);
		}

		ruleSetRepository.deactivateAllActiveExcept(id);
		toActivate.activate();

		return RuleSetResponse.from(toActivate);
	}

	/** GET /rules: la versione attiva. */
	@Transactional(readOnly = true)
	public RuleSetPublicResponse findActive() {
		return ruleSetRepository.findByIsActiveTrue()
			.map(RuleSetPublicResponse::from)
			.orElseThrow(NoActiveRuleSetException::new);
	}

	// --- supporto ---

	private void validateDeckSum(int creatures, int spells, int hidden, int items, int terrains, int deckSize) {
		int sum = creatures + spells + hidden + items + terrains;
		if (sum != deckSize) {
			throw new RuleSetRulesViolatedException(
				"Creatures + Spells + Hidden + Items + Terrains (" + sum
					+ ") must equal the deck size (" + deckSize + ")");
		}
	}
}