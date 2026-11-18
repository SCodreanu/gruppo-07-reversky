package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Smiler: "per ogni round in cui sopravvive, +2 ATK (massimo +6) e l'avv. −2 ATK (massimo −4)".
 */
class SmilerEffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Ogni round +2 ATK fino a +6 e l'avversaria −2 fino a −4: ATK 7, 9, 11, 11; A 1, −1, −1")
	void bonusAndPenalty_capped() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, smiler(), creature("A", 3, 60));
		CreatureInPlay smiler = state.challenger().creature().orElseThrow();
		CreatureInPlay a = state.opponent().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(smiler.attack()).isEqualTo(7);
		assertThat(a.attack()).isEqualTo(1);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("Smiler: +2 ATK (ATK 7), A −2 ATK (ATK 1)"));
		resolver.resolve(state);
		assertThat(smiler.attack()).isEqualTo(9);
		assertThat(a.attack()).isEqualTo(-1);
		resolver.resolve(state);
		resolver.resolve(state);
		assertThat(smiler.attack()).isEqualTo(11);
		assertThat(a.attack()).isEqualTo(-1);
	}

	@Test
	@DisplayName("Uccide A nel round: +2 ATK lo stesso (18 novembre); contro B il malus riparte da 0, il bonus resta")
	void killsInRound_stillGainsAndPenaltyRestarts() {
		// Arrange: A muore al round 1
		MatchState state = TestStates.inCombat(registry, smiler(), creature("A", 3, 5));
		CreatureInPlay smiler = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(smiler.attack()).isEqualTo(7);

		// Act
		state.opponent().placeCreature(new CreatureInPlay(creature("B", 3, 60), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);
		resolver.resolve(state);
		resolver.resolve(state);

		// Assert: bonus al massimo, B −4
		assertThat(smiler.attack()).isEqualTo(11);
		assertThat(state.opponent().creature().orElseThrow().attack()).isEqualTo(-1);
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0 dell'avversario: annulla il primo malus, non il bonus; poi il malus c'è")
	void sealedFile_cancelsFirstPenalty() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, smiler(), creature("A", 3, 60));
		state.opponent().placeItem(TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
				"SEALED_FILE_ZERO"));
		CreatureInPlay a = state.opponent().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(7);
		assertThat(a.attack()).isEqualTo(3);
		assertThat(state.opponent().items()).isEmpty();
		resolver.resolve(state);
		assertThat(a.attack()).isEqualTo(1);
	}

	private static CardDefinition smiler() {
		return TestCards.creature(0, "Smiler", TestFactions.WOUND, null, "Legendary", 5, 60, "SMILER");
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
