package it.downfall.reversky.engine.core.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.model.Side;

/**
 * Test di {@link MatchEvent}: {@code data} è immutabile e conserva l'ordine di inserimento delle chiavi,
 * così il JSON del log è identico a ogni esecuzione con lo stesso seed (AGENTS.md, regola 2; docs/log-partita).
 */
class MatchEventTest {

	@Test
	@DisplayName("data conserva l'ordine di inserimento: source, target, amount, targetDefense, attackBonus, cause")
	void data_keysInsertedInOrder_keepsInsertionOrder() {
		// Arrange: con queste chiavi HashMap darebbe [amount, targetDefense, cause, source, target, attackBonus]
		// e TreeMap [amount, attackBonus, cause, source, target, targetDefense]; Map.copyOf cambia a ogni JVM.
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("source", "Hounds");
		data.put("target", "Kappa");
		data.put("amount", 11);
		data.put("targetDefense", -2);
		data.put("attackBonus", 2);
		data.put("cause", "ATTACK");

		// Act
		MatchEvent event = damageEvent(data);

		// Assert
		assertThat(event.data().keySet())
				.containsExactly("source", "target", "amount", "targetDefense", "attackBonus", "cause");
	}

	@Test
	@DisplayName("data è una copia: cambiare amount nella mappa originale (11 → 99) lascia 11 nell'evento")
	void data_sourceMapChangedAfterConstruction_eventUnchanged() {
		// Arrange
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("amount", 11);
		MatchEvent event = damageEvent(data);

		// Act
		data.put("amount", 99);

		// Assert
		assertThat(event.data()).containsEntry("amount", 11);
	}

	@Test
	@DisplayName("data restituita non si può modificare")
	void data_modifiedFromOutside_throwsUnsupportedOperation() {
		// Arrange: mappa modificabile, così il test fallisce se il record la tiene così com'è
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("amount", 11);
		MatchEvent event = damageEvent(data);

		// Act + Assert
		assertThatThrownBy(() -> event.data().put("amount", 99))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private static MatchEvent damageEvent(Map<String, Object> data) {
		return new MatchEvent(42, 7, EventType.DAMAGE_DEALT, Side.CHALLENGER, data,
				"Hounds deals 11 damage to Kappa");
	}
}
