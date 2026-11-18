package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti delle due Mutant (ENG-26, deciso il 18 novembre): The Rootbound Doppelganger e The Stitched Corridor. Si
 * provano attraverso il {@link RoundResolver}.
 */
class MutantEffectsTest {

	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
			"SEALED_FILE_ZERO");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Rootbound: previene 3 dal primo attacco di B, poi B −2 ATK nei due round seguenti (28, 25, 22, 17)")
	void rootbound_shieldThenMalus() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, rootbound(), plain("B", 5, 40));
		CreatureInPlay rootbound = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(28);
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(25);
		assertThat(state.log().events()).extracting(MatchEvent::message).contains(
				"The Rootbound Doppelganger: B −2 ATK for its next two rounds (ATK 3)");
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(22);
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(17);
	}

	@Test
	@DisplayName("Rootbound: contro una nuova avversaria scudo e malus ripartono (C: 26, poi 23)")
	void rootbound_againstEachEnemy() {
		// Arrange: B 5/2 muore al round 1
		MatchState state = TestStates.inCombat(registry, rootbound(), plain("B", 5, 2));
		CreatureInPlay rootbound = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(rootbound.defense()).isEqualTo(28);

		// Act
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 5, 40), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(rootbound.defense()).isEqualTo(26);
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(23);
	}

	@Test
	@DisplayName("Rootbound: il Fascicolo Sigillato 0 annulla il malus, non lo scudo (28, poi 23)")
	void rootbound_sealedFileCancelsMalus() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, rootbound(), plain("B", 5, 40));
		state.opponent().placeItem(SEALED_FILE);
		CreatureInPlay rootbound = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(28);
		resolver.resolve(state);
		assertThat(rootbound.defense()).isEqualTo(23);
		assertThat(state.opponent().items()).isEmpty();
	}

	@Test
	@DisplayName("Stitched Corridor: 2 danni a ogni avversaria a inizio scontro e 3 in meno dal suo primo attacco")
	void stitchedCorridor_eachEnemy() {
		// Arrange: B 5/6 scende a 4 all'entrata e muore al round 2
		MatchState state = TestStates.inCombat(registry,
				TestCards.creature(0, "The Stitched Corridor", TestFactions.WOUND, TestFactions.ARCHIVE, "Mutant",
						2, 30, "STITCHED_CORRIDOR"),
				plain("B", 5, 6));
		CreatureInPlay corridor = state.challenger().creature().orElseThrow();

		// Act + Assert: contro B
		resolver.startCombat(state);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(corridor.defense()).isEqualTo(28);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(corridor.defense()).isEqualTo(23);

		// Act + Assert: contro C
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 5, 10), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(8);
		resolver.resolve(state);
		assertThat(corridor.defense()).isEqualTo(21);
	}

	private static CardDefinition rootbound() {
		return TestCards.creature(0, "The Rootbound Doppelganger", TestFactions.INVITATION, TestFactions.DREAM,
				"Mutant", 2, 30, "ROOTBOUND_DOPPELGANGER");
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
