package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
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
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Hidebehind e Nurikabe: "previene 2 danni dal primo attacco di ogni creatura avversaria" (D-33, stato "ultima
 * avversaria"). Si provano attraverso il {@link RoundResolver}, che chiama il passo 3.
 */
class FirstAttackShieldEffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Esempio 09: Hidebehind 4/6 contro Faceling 7/9; round 1 previene 2 (DEF 1), round 2 muore a −6")
	void hidebehind_example09() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, creature("Hidebehind", 4, 6, "HIDEBEHIND"),
				creature("Faceling", 7, 9, null));
		resolver.startCombat(state);
		CreatureInPlay hidebehind = state.challenger().creature().orElseThrow();

		// Act + Assert: round 1, 7 − 2 = 5 danni
		resolver.resolve(state);
		assertThat(hidebehind.defense()).isEqualTo(1);
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.EFFECT_TRIGGERED);
			assertThat(event.message()).isEqualTo("Hidebehind: prevents 2 damage from the first attack of Faceling");
		});

		// Act + Assert: round 2, nessuna prevenzione
		resolver.resolve(state);
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 6);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(1);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "HIDEBEHIND", "NURIKABE", "LIVING_WAITING_ROOM" })
	@DisplayName("Contro una nuova avversaria la prevenzione riparte")
	void newEnemy_preventsAgain(String code) {
		// Arrange: A muore al round 1, poi entra B
		MatchState state = TestStates.inCombat(registry, creature("Shield", 4, 20, code), creature("A", 5, 1, null));
		resolver.startCombat(state);
		resolver.resolve(state);
		CreatureInPlay shield = state.challenger().creature().orElseThrow();
		assertThat(shield.defense()).isEqualTo(17);

		// Act
		state.opponent().placeCreature(new CreatureInPlay(creature("B", 5, 30, null), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: di nuovo 5 − 2
		assertThat(shield.defense()).isEqualTo(14);
	}

	@Test
	@DisplayName("D-33: un attacco da 0 non consuma la prevenzione, che vale per il primo attacco con danno")
	void zeroAttack_doesNotConsume() {
		// Arrange: A attacca con 0 al round 1
		MatchState state = TestStates.inCombat(registry, creature("Hidebehind", 4, 20, "HIDEBEHIND"),
				creature("A", 0, 30, null));
		resolver.startCombat(state);
		resolver.resolve(state);
		CreatureInPlay hidebehind = state.challenger().creature().orElseThrow();
		assertThat(hidebehind.defense()).isEqualTo(20);

		// Act: al round 2 A attacca con 5
		state.opponent().creature().orElseThrow().addModifier(
				StatModifier.attack(ModifierSource.SPELL, Side.OPPONENT, 5, ModifierDuration.whileInPlay()));
		resolver.resolve(state);

		// Assert
		assertThat(hidebehind.defense()).isEqualTo(17);
	}

	@Test
	@DisplayName("La prevenzione non scende sotto 0: un attacco da 1 diventa 0")
	void smallAttack_neverNegative() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, creature("Nurikabe", 4, 20, "NURIKABE"),
				creature("A", 1, 30, null));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(20);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("Nurikabe: prevents 1 damage from the first attack of A"));
	}

	private static CardDefinition creature(String name, int attack, int defense, String code) {
		return TestCards.creature(0, name, TestFactions.WOUND, null, "Uncommon", attack, defense, code);
	}
}
