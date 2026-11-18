package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.impl.EchoRiftEffect;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link MatchState}: giocatori per lato, round e un solo Terreno in gioco (D-19).
 */
class MatchStateTest {

	private static final CardDefinition ECHO_RIFT = TestCards.terrain(113, "Echo Rift", TestFactions.WOUND,
			"ECHO_RIFT");
	private static final CardDefinition WATCHING_RIFT = TestCards.terrain(115, "Watching Rift",
			TestFactions.ARCHIVE, "WATCHING_RIFT");

	private final PlayerState challenger = new PlayerState(Side.CHALLENGER, 7, 30, List.of());
	private final PlayerState opponent = new PlayerState(Side.OPPONENT, 12, 30, List.of());
	private final MatchState state = new MatchState(challenger, opponent, new Random(42));

	@Test
	@DisplayName("player e opponentOf restituiscono il giocatore giusto per ogni lato")
	void player_andOpponentOf_bySide() {
		assertThat(state.player(Side.CHALLENGER)).isSameAs(challenger);
		assertThat(state.player(Side.OPPONENT)).isSameAs(opponent);
		assertThat(state.opponentOf(Side.CHALLENGER)).isSameAs(opponent);
		assertThat(state.opponentOf(Side.OPPONENT)).isSameAs(challenger);
	}

	@Test
	@DisplayName("Lati invertiti nel costruttore sono un bug")
	void constructor_invertedSides_throws() {
		assertThatThrownBy(() -> new MatchState(opponent, challenger, new Random(42)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Ogni stato ha il suo log, che parte vuoto")
	void log_newStateHasEmptyLog() {
		assertThat(state.log().events()).isEmpty();
		assertThat(new MatchState(challenger, opponent, new Random(42)).log()).isNotSameAs(state.log());
	}

	@Test
	@DisplayName("La partita parte dal round 0 (preparazione) e i round contano da 1")
	void startNextRound_countsFromOne() {
		assertThat(state.round()).isZero();
		assertThat(state.startNextRound()).isEqualTo(1);
		assertThat(state.startNextRound()).isEqualTo(2);
		assertThat(state.round()).isEqualTo(2);
	}

	@Test
	@DisplayName("Un nuovo Terreno, di chiunque sia, sostituisce quello in gioco e lo restituisce (D-19)")
	void placeTerrain_replacesPreviousOfAnyOwner() {
		// Act
		assertThat(TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER)).isEmpty();
		var replaced = TestStates.placeTerrain(state, WATCHING_RIFT, Side.OPPONENT);

		// Assert
		assertThat(replaced).hasValueSatisfying(old -> assertTerrain(old, ECHO_RIFT, Side.CHALLENGER));
		assertThat(state.terrain()).hasValueSatisfying(now -> assertTerrain(now, WATCHING_RIFT, Side.OPPONENT));
	}

	@Test
	@DisplayName("removeTerrain toglie il Terreno in gioco; solo un TERRAIN si può mettere in gioco")
	void removeTerrain_andInvalidCard() {
		// Arrange
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);

		// Act + Assert
		assertThat(state.removeTerrain()).hasValueSatisfying(old -> assertTerrain(old, ECHO_RIFT, Side.CHALLENGER));
		assertThat(state.terrain()).isEmpty();
		assertThatThrownBy(() -> state.placeTerrain(TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5),
				Side.CHALLENGER, new EchoRiftEffect())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("ENG-27: la finestra di scontro è quello in corso, o il prossimo se nessuno scontro è in corso")
	void combatWindow_currentOrNext() {
		// Act + Assert: prima del primo scontro conta il primo
		assertThat(state.combatWindow()).isEqualTo(1);
		state.startNextCombat();
		assertThat(state.combatWindow()).isEqualTo(1);
		state.endCombat();
		assertThat(state.combatWindow()).isEqualTo(2);
		state.startNextCombat();
		assertThat(state.combatWindow()).isEqualTo(2);
	}

	@Test
	@DisplayName("Gli scontri si contano da 1: 0 durante la pescata iniziale")
	void startNextCombat_countsFromOne() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());

		// Act + Assert
		assertThat(state.combatsStarted()).isZero();
		assertThat(state.startNextCombat()).isEqualTo(1);
		assertThat(state.startNextCombat()).isEqualTo(2);
		assertThat(state.combatsStarted()).isEqualTo(2);
	}

	private static void assertTerrain(PlacedTerrain terrain, CardDefinition card, Side owner) {
		assertThat(terrain.card()).isEqualTo(card);
		assertThat(terrain.owner()).isEqualTo(owner);
		assertThat(terrain.effect().code()).isEqualTo(card.effectCode());
	}
}
