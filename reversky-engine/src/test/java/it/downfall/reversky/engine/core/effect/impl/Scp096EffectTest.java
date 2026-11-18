package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * SCP-096: "la prima creatura avversaria che lo ferisce viene marchiata; +3 ATK contro quella creatura".
 */
class Scp096EffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("A lo ferisce al round 1: A è marchiata e SCP-096 attacca con 8 invece di 5; il bonus non si somma")
	void firstWound_marksAttacker() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, scp096(), creature("A", 4, 40));
		CreatureInPlay scp = state.challenger().creature().orElseThrow();
		CreatureInPlay a = state.opponent().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert: round 1 attacca ancora con 5, poi viene marchiata
		resolver.resolve(state);
		assertThat(a.defense()).isEqualTo(35);
		assertThat(scp.attack()).isEqualTo(8);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("SCP-096: marks A: +3 ATK against it (ATK 8)"));

		// Act + Assert: round 2 con 8, e una seconda ferita non dà altro
		resolver.resolve(state);
		assertThat(a.defense()).isEqualTo(27);
		assertThat(scp.attack()).isEqualTo(8);
	}

	@Test
	@DisplayName("Un attacco tutto assorbito dagli scudi non ferisce: nessun marchio")
	void shieldAbsorbsAll_noMark() {
		// Arrange: scudo di 10
		MatchState state = TestStates.inCombat(registry, scp096(), creature("A", 4, 40));
		CreatureInPlay scp = state.challenger().creature().orElseThrow();
		scp.addModifier(StatModifier.shield(ModifierSource.SPELL, Side.CHALLENGER, 10,
				ModifierDuration.whileInPlay()));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(scp.attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("Morta la marchiata, il bonus finisce con lo scontro; la nuova avversaria non si marchia (D-31)")
	void markedDies_bonusEndsAndNoNewMark() {
		// Arrange: A ferisce SCP-096 e muore nello stesso round
		MatchState state = TestStates.inCombat(registry, scp096(), creature("A", 4, 5));
		CreatureInPlay scp = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(scp.attack()).isEqualTo(5);

		// Act: B lo ferisce
		state.opponent().placeCreature(new CreatureInPlay(creature("B", 4, 40), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(scp.attack()).isEqualTo(5);
	}

	private static CardDefinition scp096() {
		return TestCards.creature(0, "SCP-096", TestFactions.ARCHIVE, null, "Uncommon", 5, 30, "SCP_096");
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
