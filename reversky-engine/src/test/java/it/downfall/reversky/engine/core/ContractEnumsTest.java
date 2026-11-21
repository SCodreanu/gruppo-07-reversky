package it.downfall.reversky.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.Side;

/**
 * I valori degli enum del core sono le stringhe del contratto JSON: lo strato {@code web} (ENG-12) li converte
 * per nome, e l'API li salva nel database. Un valore rinominato o mancante rompe il contratto senza errori di
 * compilazione, per questo è controllato qui contro docs/api-engine, docs/log-partita e docs/regole-base.
 */
class ContractEnumsTest {

	@Test
	@DisplayName("CardType ha i 5 tipi del contratto: CREATURE, SPELL, HIDDEN, TERRAIN, ITEM")
	void cardType_matchesContract() {
		assertThat(names(CardType.values()))
				.containsExactlyInAnyOrder("CREATURE", "SPELL", "HIDDEN", "TERRAIN", "ITEM");
	}

	@Test
	@DisplayName("CardSubtype ha i 4 sottotipi di Spell e Hidden: DAMAGE, SHIELD, BURN, SURVIVAL")
	void cardSubtype_matchesContract() {
		assertThat(names(CardSubtype.values()))
				.containsExactlyInAnyOrder("DAMAGE", "SHIELD", "BURN", "SURVIVAL");
	}

	@Test
	@DisplayName("Side ha 2 lati, CHALLENGER e OPPONENT: un evento di entrambi ha side null, non un terzo valore")
	void side_matchesContract() {
		assertThat(names(Side.values())).containsExactlyInAnyOrder("CHALLENGER", "OPPONENT");
	}

	@Test
	@DisplayName("BattleResult ha i 3 esiti dell'enum del database: CHALLENGER_WIN, OPPONENT_WIN, DRAW")
	void battleResult_matchesContract() {
		assertThat(names(BattleResult.values()))
				.containsExactlyInAnyOrder("CHALLENGER_WIN", "OPPONENT_WIN", "DRAW");
	}

	@Test
	@DisplayName("EndReason ha i 5 motivi di fine partita di docs/regole-base")
	void endReason_matchesContract() {
		assertThat(names(EndReason.values())).containsExactlyInAnyOrder(
				"LIFE_POINTS_ZERO", "BOTH_LIFE_POINTS_ZERO", "NO_CREATURES_LEFT", "BOTH_DECKS_EMPTY", "ROUND_LIMIT");
	}

	@Test
	@DisplayName("EventType ha i 19 tipi di evento di docs/log-partita")
	void eventType_matchesContract() {
		assertThat(names(EventType.values())).containsExactlyInAnyOrder(
				"MATCH_STARTED", "CARD_DRAWN", "CREATURE_ENTERED", "SPELL_RESOLVED", "BURN_REVEALED",
				"HIDDEN_SET", "HIDDEN_TRIGGERED", "TERRAIN_PLACED", "TERRAIN_REMOVED", "DECK_REORDERED",
				"ITEM_PLACED", "ITEM_CONSUMED", "ROUND_STARTED", "ATTACK_COMPUTED", "DAMAGE_DEALT",
				"EFFECT_TRIGGERED", "CREATURE_DIED", "PLAYER_DAMAGED", "MATCH_ENDED");
	}

	private static List<String> names(Enum<?>[] values) {
		return Arrays.stream(values).map(Enum::name).toList();
	}
}
