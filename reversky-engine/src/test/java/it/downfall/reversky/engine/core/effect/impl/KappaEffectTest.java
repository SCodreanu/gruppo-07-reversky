package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Kappa: "+3 ATK nel primo round", contato dall'entrata in campo (D-31).
 */
class KappaEffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Kappa 6/10: attacca con 9 nel primo round, con 6 dal secondo")
	void firstRoundOnly() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, kappa(), creature("B", 2, 30));
		CreatureInPlay b = state.opponent().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(9);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("Kappa: +3 ATK in its first round (ATK 9)"));
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(21);
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(15);
	}

	@Test
	@DisplayName("Una creatura sopravvissuta non rientra: contro la nuova avversaria Kappa non ha il +3 (D-31)")
	void survivor_noNewBonus() {
		// Arrange: B muore al round 1
		MatchState state = TestStates.inCombat(registry, kappa(), creature("B", 2, 9));
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();

		// Act
		state.opponent().placeCreature(new CreatureInPlay(creature("C", 2, 30), Side.OPPONENT));
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(6);
	}

	@Test
	@DisplayName("Se una Hidden uccide l'avversaria prima dell'entrata, Kappa entra allo scontro dopo (18 novembre)")
	void hiddenKillsFirst_entersNextCombat() {
		// Arrange: Breaking Point dello sfidante porta a 0 la DEF 5 di B
		MatchState state = TestStates.inCombat(registry, kappa(), creature("B", 2, 5));
		state.challenger().setHidden(new SetHidden(
				TestCards.hidden(107, "Breaking Point", CardSubtype.DAMAGE, "BREAKING_POINT"), 1));
		CreatureInPlay kappa = state.challenger().creature().orElseThrow();

		// Act + Assert: lo scontro finisce prima dell'entrata
		resolver.startCombat(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(kappa.hasEntered()).isFalse();
		assertThat(kappa.attack()).isEqualTo(6);

		// Act + Assert: al nuovo scontro Kappa entra e ha il +3 nel suo primo round
		state.opponent().placeCreature(new CreatureInPlay(creature("C", 2, 30), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(kappa.hasEntered()).isTrue();
		assertThat(kappa.attack()).isEqualTo(9);
	}

	private static CardDefinition kappa() {
		return TestCards.creature(0, "Kappa", TestFactions.DREAM, null, "Uncommon", 6, 10, "KAPPA");
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
