package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * SCP-2317: "quando muore, 4 danni all'avv. e +1 ATK alla prossima propria creatura" (docs/effetti, "Effetto alla
 * morte").
 */
class Scp2317EffectTest {

	private static final PendingModifier NEXT_ATTACK = new PendingModifier("SCP-2317", ModifierSource.EFFECT,
			Side.CHALLENGER, 1, 0, 0);

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Muore a −3: 4 danni ad A (10 − 3 − 4 = 3), +1 ATK in attesa, lo sfidante perde 3 PV")
	void dies_damagesEnemyAndPreparesNextCreature() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, scp2317(), creature("A", 5, 10));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(3);
		assertThat(state.challenger().pendingModifiers()).containsExactly(NEXT_ATTACK);
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 3);
		assertThat(state.log().events()).extracting(MatchEvent::type).endsWith(EventType.CREATURE_DIED,
				EventType.EFFECT_TRIGGERED, EventType.PLAYER_DAMAGED);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("SCP-2317: deals 4 damage to A (DEF 3), +1 ATK to the next creature"));
	}

	@Test
	@DisplayName("Il danno alla morte uccide anche A (6 − 3 − 4 = −1): muoiono entrambe, nessuno perde PV")
	void deathDamageKills_doubleDeath() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, scp2317(), creature("A", 5, 6));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
		assertThat(state.opponent().creaturesDefeated()).isEqualTo(1);
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0 dell'avversario: annulla il danno e si consuma; il +1 ATK resta")
	void sealedFile_cancelsDamageNotBonus() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, scp2317(), creature("A", 5, 10));
		state.opponent().placeItem(TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
				"SEALED_FILE_ZERO"));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(7);
		assertThat(state.opponent().items()).isEmpty();
		assertThat(state.challenger().pendingModifiers()).containsExactly(NEXT_ATTACK);
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.ITEM_CONSUMED);
			assertThat(event.data()).containsEntry("effect", "cancels SCP-2317");
		});
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("SCP-2317: +1 ATK to the next creature"));
	}

	@Test
	@DisplayName("Il +1 ATK arriva alla prossima creatura dello sfidante, a inizio del suo primo scontro (D-34)")
	void nextCreature_getsAttack() {
		// Arrange: SCP-2317 muore, entra B
		MatchState state = TestStates.inCombat(registry, scp2317(), creature("A", 5, 10));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Act
		state.challenger().placeCreature(TestStates.withEffect(registry, creature("B", 4, 9), Side.CHALLENGER));
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(5);
		assertThat(state.challenger().pendingModifiers()).isEmpty();
	}

	private static CardDefinition scp2317() {
		return TestCards.creature(0, "SCP-2317", TestFactions.ARCHIVE, null, "Mythic", 3, 2, "SCP_2317");
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
