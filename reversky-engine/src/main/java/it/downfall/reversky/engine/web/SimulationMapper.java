package it.downfall.reversky.engine.web;

import java.util.List;

import org.springframework.stereotype.Component;

import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.Faction;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.PlayerResult;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;
import it.downfall.reversky.engine.web.dto.CardRequest;
import it.downfall.reversky.engine.web.dto.FactionBonusRequest;
import it.downfall.reversky.engine.web.dto.MatchEventResponse;
import it.downfall.reversky.engine.web.dto.PlayerRequest;
import it.downfall.reversky.engine.web.dto.PlayerResultResponse;
import it.downfall.reversky.engine.web.dto.RulesRequest;
import it.downfall.reversky.engine.web.dto.SimulationRequest;
import it.downfall.reversky.engine.web.dto.SimulationResponse;

/**
 * Converte i DTO del contratto REST (docs/api-engine) negli oggetti del core e viceversa. Copia i campi e basta:
 * nessuna regola di gioco e nessun controllo, che stanno nel core.
 * <p>
 * {@link #toCore(SimulationRequest)} si chiama solo su una richiesta già validata ({@code @Valid}): i campi
 * obbligatori non sono {@code null}.
 */
@Component
public class SimulationMapper {

	/**
	 * Richiesta → input del core.
	 *
	 * @param request richiesta validata
	 * @return l'input della simulazione
	 * @throws it.downfall.reversky.engine.core.validation.InvalidSimulationInputException se le regole non sono
	 *                                                                                     valide
	 */
	public SimulationInput toCore(SimulationRequest request) {
		return new SimulationInput(
				request.seed(),
				toCore(request.rules()),
				request.factions().stream().map(faction -> new Faction(faction.id(), faction.name())).toList(),
				request.factionBonuses().stream().map(SimulationMapper::toCore).toList(),
				toCore(request.challenger()),
				toCore(request.opponent()));
	}

	/**
	 * Risultato del core → risposta.
	 *
	 * @param result risultato della simulazione
	 * @return la risposta, con il log nello stesso ordine
	 */
	public SimulationResponse toResponse(MatchResult result) {
		return new SimulationResponse(
				result.result(),
				result.endReason(),
				result.totalRounds(),
				toResponse(result.challenger()),
				toResponse(result.opponent()),
				result.seed(),
				result.rulesVersion(),
				result.log().stream().map(SimulationMapper::toResponse).toList());
	}

	private static RuleConfig toCore(RulesRequest rules) {
		return new RuleConfig(rules.rulesVersion(), rules.startingLifePoints(), rules.maxRounds());
	}

	private static FactionBonus toCore(FactionBonusRequest bonus) {
		return new FactionBonus(bonus.originFactionId(), bonus.targetFactionId(), bonus.atkBonus(),
				bonus.defBonus());
	}

	private static PlayerInput toCore(PlayerRequest player) {
		List<CardDefinition> deck = player.deck().stream().map(SimulationMapper::toCore).toList();
		return new PlayerInput(player.userId(), deck);
	}

	private static CardDefinition toCore(CardRequest card) {
		return new CardDefinition(card.cardId(), card.name(), card.type(), card.rarity(), card.rarityOrder(),
				card.factionId(), card.secondaryFactionId(), card.atk(), card.def(), card.subtype(),
				card.effectCode());
	}

	private static PlayerResultResponse toResponse(PlayerResult player) {
		return new PlayerResultResponse(player.userId(), player.finalLifePoints(), player.creaturesDefeated());
	}

	private static MatchEventResponse toResponse(MatchEvent event) {
		return new MatchEventResponse(event.sequence(), event.round(), event.type(), event.side(), event.data(),
				event.message());
	}
}
