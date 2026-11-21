package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
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
 * Effetti {@code P2} sul mazzo e sulle Hidden dell'avversario (ENG-39, M-11, D-15): La Regina dei Sentieri Perduti e
 * L'Operatore della Stanza 0. Si provano attraverso il {@link RoundResolver}.
 */
class OpponentDeckAndHiddenEffectsTest {

	private static final String QUEEN = "La Regina dei Sentieri Perduti";
	private static final String OPERATOR = "L'Operatore della Stanza 0";
	private static final CardDefinition HOSTILE_PRESENCE = TestCards.hidden(105, "Hostile Presence",
			CardSubtype.DAMAGE, "HOSTILE_PRESENCE");
	private static final CardDefinition LAST_SANCTUARY = TestCards.hidden(101, "Last Sanctuary", CardSubtype.SURVIVAL,
			"LAST_SANCTUARY");
	private static final CardDefinition UNSEEN_EXIT = TestCards.hidden(107, "Unseen Exit", CardSubtype.SURVIVAL,
			"UNSEEN_EXIT");
	private static final CardDefinition TORII_BELL = TestCards.item(120, "Campanello del Torii", TestFactions.DREAM,
			"TORII_BELL");
	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0",
			TestFactions.ARCHIVE, "SEALED_FILE_ZERO");
	private static final CardDefinition SPELL = TestCards.spell(97, "Bite of the Reverse", CardSubtype.DAMAGE,
			"BITE_OF_REVERSE");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- La Regina dei Sentieri Perduti ----

	@Test
	@DisplayName("Regina: all'entrata manda in fondo la più rara delle prossime tre creature, poi avv. −2 DEF")
	void queen_entry_movesRarestOfNextThree() {
		// Arrange: le prossime tre creature sono A (Common), B (Rare), C (Legendary); la Spell resta al suo posto
		MatchState state = withOpponentDeck(queen(), plain("E", 5, 40), List.of(creature("A", "Common"), SPELL,
				creature("B", "Rare"), creature("C", "Legendary"), creature("D", "Mythic")));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(deckNames(state)).containsExactly("A", "Bite of the Reverse", "B", "D", "C");
		assertThat(enemy(state).defense()).isEqualTo(38);
		assertThat(messages(state)).containsSubsequence(QUEEN + " moves C to position 5",
				QUEEN + ": deals 2 damage to E (DEF 38)");
		assertThat(state.log().events()).filteredOn(event -> event.type() == EventType.DECK_REORDERED)
				.singleElement().satisfies(event -> assertThat(event.side()).isEqualTo(Side.OPPONENT));
	}

	@Test
	@DisplayName("Regina: a parità di rarità va in fondo quella che verrebbe pescata prima")
	void queen_tie_movesFirstDrawn() {
		// Arrange
		MatchState state = withOpponentDeck(queen(), plain("E", 5, 40), List.of(creature("A", "Rare"),
				creature("B", "Rare"), creature("C", "Common")));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(deckNames(state)).containsExactly("B", "C", "A");
	}

	@Test
	@DisplayName("Regina: con meno di due creature nel mazzo avversario non sposta niente, il −2 DEF arriva lo stesso")
	void queen_fewerThanTwoCreatures_noMove() {
		// Arrange
		MatchState state = withOpponentDeck(queen(), plain("E", 5, 40), List.of(SPELL, creature("C", "Legendary")));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(deckNames(state)).containsExactly("Bite of the Reverse", "C");
		assertThat(enemy(state).defense()).isEqualTo(38);
		assertThat(state.log().events()).noneMatch(event -> event.type() == EventType.DECK_REORDERED);
	}

	@Test
	@DisplayName("Regina: se la carta scelta è già l'ultima del mazzo non c'è DECK_REORDERED")
	void queen_alreadyLast_noEvent() {
		// Arrange
		MatchState state = withOpponentDeck(queen(), plain("E", 5, 40), List.of(creature("A", "Common"),
				creature("B", "Legendary")));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(deckNames(state)).containsExactly("A", "B");
		assertThat(state.log().events()).noneMatch(event -> event.type() == EventType.DECK_REORDERED);
	}

	@Test
	@DisplayName("Regina: sposta di nuovo a ogni eliminazione")
	void queen_onKill_movesAgain() {
		// Arrange: E 1/3 resta a 1 dopo il −2 e muore al primo round
		MatchState state = withOpponentDeck(queen(), plain("E", 1, 3), List.of(creature("M", "Mythic"),
				creature("C1", "Common"), creature("L", "Legendary"), creature("R", "Rare")));
		resolver.startCombat(state);
		assertThat(deckNames(state)).containsExactly("M", "C1", "R", "L");

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(deckNames(state)).containsExactly("C1", "R", "L", "M");
		assertThat(messages(state)).contains(QUEEN + " moves M to position 4");
	}

	@Test
	@DisplayName("Regina contro Il Custode delle Porte Incrociate: il Custode non subisce il −2 DEF, il mazzo sì")
	void queen_againstKeeper_deckStillMoves() {
		// Arrange
		MatchState state = withOpponentDeck(queen(), TestCards.creature(0, "The Crossed Doors Keeper",
				TestFactions.WOUND, null, "Mythic", 5, 20, CrossedDoorsKeeperEffect.CODE), List.of(
						creature("A", "Common"), creature("B", "Legendary"), creature("C", "Common")));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(20);
		assertThat(deckNames(state)).containsExactly("A", "C", "B");
	}

	// ---- L'Operatore della Stanza 0 ----

	@Test
	@DisplayName("Operatore: all'entrata la prossima Hidden avversaria lavorerà per il suo proprietario")
	void operator_entry_crossesNextEnemyHidden() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, operator(), plain("B", 5, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.opponent().pendingCrossings()).containsExactly(OPERATOR);
		assertThat(messages(state)).contains(
				OPERATOR + ": crosses the lines: the next Opponent Hidden works for the Challenger");
	}

	@Test
	@DisplayName("Operatore: una Hidden DAMAGE incrociata colpisce la creatura del suo proprietario")
	void operator_crossedDamageHidden_hitsItsOwner() {
		// Arrange: la Hidden dell'avversario è incrociata
		MatchState state = TestStates.inCombat(plain("A", 6, 20), plain("B", 5, 20));
		state.opponent().setHidden(new SetHidden(HOSTILE_PRESENCE, 1));
		state.opponent().crossNextHidden(OPERATOR);

		// Act
		resolver.startCombat(state);

		// Assert: B −5 ATK, A intatta
		assertThat(enemy(state).attack()).isZero();
		assertThat(self(state).attack()).isEqualTo(6);
		assertThat(state.opponent().hiddenZone()).isEmpty();
		assertThat(state.opponent().pendingCrossings()).isEmpty();
		assertThat(messages(state)).containsSubsequence(
				OPERATOR + ": crosses the lines: Hostile Presence works for the Challenger",
				"Hostile Presence triggers");
		assertThat(state.log().events()).filteredOn(event -> event.type() == EventType.HIDDEN_TRIGGERED)
				.singleElement().satisfies(event -> assertThat(event.side()).isEqualTo(Side.CHALLENGER));
	}

	@Test
	@DisplayName("Operatore: una Hidden SURVIVAL incrociata aiuta la creatura dell'Operatore")
	void operator_crossedSurvivalHidden_helpsOperatorSide() {
		// Arrange
		MatchState state = TestStates.inCombat(plain("A", 6, 20), plain("B", 5, 20));
		state.opponent().setHidden(new SetHidden(LAST_SANCTUARY, 1));
		state.opponent().crossNextHidden(OPERATOR);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(25);
		assertThat(enemy(state).defense()).isEqualTo(20);
	}

	@Test
	@DisplayName("Operatore: una Hidden con condizione incrociata si controlla sulla creatura dell'Operatore")
	void operator_crossedConditionalHidden_checksOperatorSide() {
		// Arrange: Unseen Exit dell'avversario, incrociata; l'attacco di B (6) ucciderebbe A (DEF 3)
		MatchState state = TestStates.inCombat(plain("A", 1, 3), plain("B", 6, 40));
		state.opponent().setHidden(new SetHidden(UNSEEN_EXIT, 1));
		state.opponent().crossNextHidden(OPERATOR);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert: 6 − 5 = 1 danno
		assertThat(self(state).defense()).isEqualTo(2);
		assertThat(state.opponent().hiddenZone()).isEmpty();
		assertThat(state.opponent().pendingCrossings()).isEmpty();
	}

	@Test
	@DisplayName("Operatore: finché l'incrocio è in attesa, la Hidden con condizione non salva il suo proprietario")
	void operator_pendingCrossing_ownerNotSaved() {
		// Arrange: l'attacco di A (8) ucciderebbe B (DEF 5)
		MatchState state = TestStates.inCombat(plain("A", 8, 40), plain("B", 1, 5));
		state.opponent().setHidden(new SetHidden(UNSEEN_EXIT, 1));
		state.opponent().crossNextHidden(OPERATOR);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().hiddenZone()).hasSize(1);
		assertThat(state.opponent().pendingCrossings()).containsExactly(OPERATOR);
	}

	@Test
	@DisplayName("Operatore: il Campanello del Torii del proprietario annulla la Hidden incrociata; incrocio usato")
	void operator_ownersToriiBell_cancelsCrossedHidden() {
		// Arrange
		MatchState state = TestStates.inCombat(plain("A", 6, 20), plain("B", 5, 20));
		state.opponent().setHidden(new SetHidden(HOSTILE_PRESENCE, 1));
		state.opponent().crossNextHidden(OPERATOR);
		state.opponent().placeItem(TORII_BELL);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(state.opponent().items()).isEmpty();
		assertThat(state.opponent().hiddenZone()).isEmpty();
		assertThat(state.opponent().pendingCrossings()).isEmpty();
	}

	@Test
	@DisplayName("Operatore: niente incrocio contro Il Custode delle Porte Incrociate")
	void operator_againstKeeper_noCrossing() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, operator(), TestCards.creature(0,
				"The Crossed Doors Keeper", TestFactions.WOUND, null, "Mythic", 5, 20, CrossedDoorsKeeperEffect.CODE));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.opponent().pendingCrossings()).isEmpty();
	}

	@Test
	@DisplayName("Operatore: il Fascicolo Sigillato 0 dell'avversario ferma l'incrocio e si consuma")
	void operator_sealedFile_stopsCrossing() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, operator(), plain("B", 5, 20));
		state.opponent().placeItem(SEALED_FILE);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.opponent().pendingCrossings()).isEmpty();
		assertThat(state.opponent().items()).isEmpty();
	}

	private MatchState withOpponentDeck(CardDefinition challenger, CardDefinition opponent,
			List<CardDefinition> opponentDeck) {
		MatchState state = TestStates.withDecks(List.of(), opponentDeck);
		state.challenger().placeCreature(TestStates.withEffect(registry, challenger, Side.CHALLENGER));
		state.opponent().placeCreature(TestStates.withEffect(registry, opponent, Side.OPPONENT));
		return state;
	}

	private static List<String> deckNames(MatchState state) {
		return state.opponent().deck().stream().map(CardDefinition::name).toList();
	}

	private static CreatureInPlay self(MatchState state) {
		return state.challenger().creature().orElseThrow();
	}

	private static CreatureInPlay enemy(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}

	private static List<String> messages(MatchState state) {
		return state.log().events().stream().map(MatchEvent::message).toList();
	}

	private static CardDefinition queen() {
		return TestCards.creature(0, QUEEN, TestFactions.INVITATION, null, "Legendary", 5, 10,
				QueenOfLostPathsEffect.CODE);
	}

	private static CardDefinition operator() {
		return TestCards.creature(0, OPERATOR, TestFactions.WOUND, null, "Rare", 4, 8,
				RoomZeroOperatorEffect.CODE);
	}

	private static CardDefinition creature(String name, String rarity) {
		return TestCards.creature(0, name, TestFactions.WOUND, null, rarity, 1, 10, null);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
