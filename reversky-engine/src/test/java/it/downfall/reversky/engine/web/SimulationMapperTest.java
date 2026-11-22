package it.downfall.reversky.engine.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.Faction;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.PlayerResult;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;
import it.downfall.reversky.engine.web.dto.CardRequest;
import it.downfall.reversky.engine.web.dto.FactionBonusRequest;
import it.downfall.reversky.engine.web.dto.FactionRequest;
import it.downfall.reversky.engine.web.dto.MatchEventResponse;
import it.downfall.reversky.engine.web.dto.PlayerRequest;
import it.downfall.reversky.engine.web.dto.PlayerResultResponse;
import it.downfall.reversky.engine.web.dto.RulesRequest;
import it.downfall.reversky.engine.web.dto.SimulationRequest;
import it.downfall.reversky.engine.web.dto.SimulationResponse;

/**
 * Test di {@link SimulationMapper}: conversione tra i DTO del contratto (docs/api-engine) e gli oggetti del core.
 * JUnit puro, senza contesto Spring.
 */
class SimulationMapperTest {

	private final SimulationMapper mapper = new SimulationMapper();

	@Test
	@DisplayName("toCore porta seed, regole, fazioni, bonus e mazzi; atk e def diventano attack e defense")
	void toCore_mapsEveryField() {
		// Arrange
		var crawlers = new CardRequest(4, "Crawlers", CardType.CREATURE, "Common", 1, 1, 4, 4, 5, null, null);
		var veil = new CardRequest(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2, null, null, null, null,
				CardSubtype.SHIELD, "VEIL_OF_ANCHORED");
		var kodama = new CardRequest(41, "Kodama", CardType.CREATURE, "Common", 1, 4, null, 4, 6, null, null);
		var request = new SimulationRequest(8812734650912L, new RulesRequest("1.0", 30, 75),
				List.of(new FactionRequest(1, "Theory of the Wound"), new FactionRequest(4, "Theory of the Dream")),
				List.of(new FactionBonusRequest(1, 4, 2, 0), new FactionBonusRequest(4, 1, -1, 0)),
				new PlayerRequest(7, List.of(crawlers, veil)),
				new PlayerRequest(12, List.of(kodama)));

		// Act
		SimulationInput input = mapper.toCore(request);

		// Assert
		assertThat(input.seed()).isEqualTo(8812734650912L);
		assertThat(input.rules()).isEqualTo(new RuleConfig("1.0", 30, 75));
		assertThat(input.factions()).containsExactly(new Faction(1, "Theory of the Wound"),
				new Faction(4, "Theory of the Dream"));
		assertThat(input.factionBonuses()).containsExactly(new FactionBonus(1, 4, 2, 0), new FactionBonus(4, 1, -1, 0));
		assertThat(input.challenger().userId()).isEqualTo(7);
		assertThat(input.challenger().deck()).containsExactly(
				new CardDefinition(4, "Crawlers", CardType.CREATURE, "Common", 1, 1, 4, 4, 5, null, null),
				new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2, null, null, null, null,
						CardSubtype.SHIELD, "VEIL_OF_ANCHORED"));
		assertThat(input.opponent().userId()).isEqualTo(12);
		assertThat(input.opponent().deck()).containsExactly(
				new CardDefinition(41, "Kodama", CardType.CREATURE, "Common", 1, 4, null, 4, 6, null, null));
	}

	@Test
	@DisplayName("toResponse porta risultato, giocatori, seed, versione delle regole e log nello stesso ordine")
	void toResponse_mapsEveryField() {
		// Arrange
		Map<String, Object> damageData = new LinkedHashMap<>();
		damageData.put("damage", 3);
		damageData.put("lifePoints", 27);
		var started = new MatchEvent(1, 0, EventType.MATCH_STARTED, null, Map.of("startingLifePoints", 30),
				"Match started");
		var damaged = new MatchEvent(2, 1, EventType.PLAYER_DAMAGED, Side.OPPONENT, damageData,
				"Opponent loses 3 life points (27 left)");
		var result = new MatchResult(BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1,
				new PlayerResult(7, 30, 1), new PlayerResult(12, 27, 0), 1L, "1.0", List.of(started, damaged));

		// Act
		SimulationResponse response = mapper.toResponse(result);

		// Assert
		assertThat(response).isEqualTo(new SimulationResponse(BattleResult.CHALLENGER_WIN,
				EndReason.NO_CREATURES_LEFT, 1, new PlayerResultResponse(7, 30, 1), new PlayerResultResponse(12, 27, 0),
				1L, "1.0", List.of(
						new MatchEventResponse(1, 0, EventType.MATCH_STARTED, null, Map.of("startingLifePoints", 30),
								"Match started"),
						new MatchEventResponse(2, 1, EventType.PLAYER_DAMAGED, Side.OPPONENT, damageData,
								"Opponent loses 3 life points (27 left)"))));
		assertThat(response.log().get(1).data().keySet()).containsExactly("damage", "lifePoints");
	}
}
