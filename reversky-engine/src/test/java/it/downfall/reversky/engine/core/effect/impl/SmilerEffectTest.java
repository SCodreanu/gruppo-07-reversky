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
 * Smiler: "per ogni round in cui sopravvive, +3 ATK (massimo +9) e l'avv. −3 ATK (massimo −6)" (ENG-42, 21 novembre:
 * erano +2, massimo +6, e −2, massimo −4).
 */
class SmilerEffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Ogni round +3 ATK fino a +9 e l'avversaria −3 fino a −6: ATK 8, 11, 14, 14; A 0, −3, −3")
	void bonusAndPenalty_capped() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, smiler(), creature("A", 3, 60));
		CreatureInPlay smiler = state.challenger().creature().orElseThrow();
		CreatureInPlay a = state.opponent().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(smiler.attack()).isEqualTo(8);
		assertThat(a.attack()).isEqualTo(0);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("Smiler: +3 ATK (ATK 8), A −3 ATK (ATK 0)"));
		resolver.resolve(state);
		assertThat(smiler.attack()).isEqualTo(11);
		assertThat(a.attack()).isEqualTo(-3);
		resolver.resolve(state);
		resolver.resolve(state);
		assertThat(smiler.attack()).isEqualTo(14);
		assertThat(a.attack()).isEqualTo(-3);
	}

	@Test
	@DisplayName("Uccide A nel round: +3 ATK lo stesso (18 novembre); contro B il malus riparte da 0, il bonus resta")
	void killsInRound_stillGainsAndPenaltyRestarts() {
		// Arrange: A muore al round 1
		MatchState state = TestStates.inCombat(registry, smiler(), creature("A", 3, 5));
		CreatureInPlay smiler = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(smiler.attack()).isEqualTo(8);

		// Act
		state.opponent().placeCreature(new CreatureInPlay(creature("B", 3, 60), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);
		resolver.resolve(state);
		resolver.resolve(state);

		// Assert: bonus al massimo (+9), B −6
		assertThat(smiler.attack()).isEqualTo(14);
		assertThat(state.opponent().creature().orElseThrow().attack()).isEqualTo(-3);
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
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(8);
		assertThat(a.attack()).isEqualTo(3);
		assertThat(state.opponent().items()).isEmpty();
		resolver.resolve(state);
		assertThat(a.attack()).isEqualTo(0);
	}

	private static CardDefinition smiler() {
		return TestCards.creature(0, "Smiler", TestFactions.WOUND, null, "Legendary", 5, 60, "SMILER");
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
