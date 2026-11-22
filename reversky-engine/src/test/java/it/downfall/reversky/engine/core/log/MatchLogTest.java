package it.downfall.reversky.engine.core.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Test di {@link MatchLog}: un test per ogni {@link EventType}, con le chiavi di {@code data} e il messaggio in
 * inglese, più numerazione e round degli eventi (docs/log-partita).
 */
class MatchLogTest {

	private static final CardDefinition CRAWLERS = TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5);
	private static final CardDefinition KAPPA = TestCards.creature("Kappa", TestFactions.DREAM, 6, 7);
	private static final CardDefinition VEIL = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");
	private static final CardDefinition CALL_OF_THE_RIFT = TestCards.spell(102, "Call of the Rift",
			CardSubtype.BURN, "CRACK_CALL");
	private static final CardDefinition LAST_SANCTUARY = TestCards.hidden(105, "Last Sanctuary",
			CardSubtype.SURVIVAL, "LAST_SANCTUARY");
	private static final CardDefinition ECHO_RIFT = TestCards.terrain(113, "Echo Rift", TestFactions.WOUND,
			"ECHO_RIFT");
	private static final CardDefinition WATCHING_RIFT = TestCards.terrain(115, "Watching Rift",
			TestFactions.ARCHIVE, "WATCHING_RIFT");
	private static final CardDefinition TORII_BELL = TestCards.item(120, "Campanello del Torii", TestFactions.DREAM,
			"TORII_BELL");

	private final MatchLog log = new MatchLog();

	@Test
	@DisplayName("Ogni EventType ha il suo metodo: chiamandoli tutti escono i 19 tipi del contratto")
	void everyEventType_hasAMethod() {
		// Act
		logOneOfEach();

		// Assert
		assertThat(log.events()).extracting(MatchEvent::type).containsExactlyInAnyOrder(EventType.values());
	}

	@Test
	@DisplayName("Gli eventi sono numerati da 1 in ordine; quelli prima del primo round sono al round 0")
	void events_numberedFromOneWithCurrentRound() {
		// Act
		log.matchStarted(30);
		log.creatureEntered(TestCards.inPlay(CRAWLERS, Side.CHALLENGER));
		log.roundStarted(1);
		log.creatureDied(TestCards.inPlay(CRAWLERS, Side.CHALLENGER));

		// Assert
		assertThat(log.events()).extracting(MatchEvent::sequence).containsExactly(1, 2, 3, 4);
		assertThat(log.events()).extracting(MatchEvent::round).containsExactly(0, 0, 1, 1);
		assertThat(log.round()).isEqualTo(1);
	}

	@Test
	@DisplayName("La lista restituita è una copia non modificabile")
	void events_returnedListIsUnmodifiableCopy() {
		// Arrange
		log.matchStarted(30);

		// Act + Assert
		assertThatThrownBy(() -> log.events().clear()).isInstanceOf(UnsupportedOperationException.class);
		assertThat(log.events()).hasSize(1);
	}

	@Test
	@DisplayName("MATCH_STARTED: startingLifePoints 30, side nullo")
	void matchStarted() {
		log.matchStarted(30);

		assertEvent(EventType.MATCH_STARTED, null, "Match started — 30 life points each");
		assertThat(last().data()).containsExactly(entry("startingLifePoints", 30));
	}

	@Test
	@DisplayName("CARD_DRAWN: card e cardType")
	void cardDrawn() {
		log.cardDrawn(Side.CHALLENGER, VEIL);

		assertEvent(EventType.CARD_DRAWN, Side.CHALLENGER, "Challenger draws Veil of the Anchored (SPELL)");
		assertThat(last().data()).containsExactly(entry("card", "Veil of the Anchored"), entry("cardType", "SPELL"));
	}

	@Test
	@DisplayName("CREATURE_ENTERED: card, attack e defense attuali")
	void creatureEntered() {
		log.creatureEntered(TestCards.inPlay(CRAWLERS, Side.CHALLENGER));

		assertEvent(EventType.CREATURE_ENTERED, Side.CHALLENGER, "Challenger plays Crawlers (ATK 4, DEF 5)");
		assertThat(last().data()).containsExactly(entry("card", "Crawlers"), entry("attack", 4),
				entry("defense", 5));
	}

	@Test
	@DisplayName("SPELL_RESOLVED: con bersaglio e senza (si applica alla prossima creatura)")
	void spellResolved() {
		log.spellResolved(Side.OPPONENT, VEIL, "VEIL_OF_ANCHORED", "Kappa");
		assertEvent(EventType.SPELL_RESOLVED, Side.OPPONENT, "Veil of the Anchored resolves on Kappa");
		assertThat(last().data()).containsExactly(entry("card", "Veil of the Anchored"),
				entry("effect", "VEIL_OF_ANCHORED"), entry("target", "Kappa"));

		log.spellResolved(Side.OPPONENT, VEIL, "VEIL_OF_ANCHORED", null);
		assertThat(last().message()).isEqualTo("Veil of the Anchored resolves");
		assertThat(last().data()).containsEntry("target", null);
	}

	@Test
	@DisplayName("BURN_REVEALED: carta rivelata, oppure niente a mazzo vuoto")
	void burnRevealed() {
		log.burnRevealed(Side.CHALLENGER, CALL_OF_THE_RIFT, KAPPA, true);
		assertEvent(EventType.BURN_REVEALED, Side.CHALLENGER, "Call of the Rift reveals Kappa");
		assertThat(last().data()).containsExactly(entry("card", "Call of the Rift"), entry("revealed", "Kappa"),
				entry("bonusApplied", true));

		log.burnRevealed(Side.CHALLENGER, CALL_OF_THE_RIFT, null, false);
		assertThat(last().message()).isEqualTo("Call of the Rift reveals nothing");
	}

	@Test
	@DisplayName("HIDDEN_SET: la carta non si nomina")
	void hiddenSet() {
		log.hiddenSet(Side.OPPONENT);

		assertEvent(EventType.HIDDEN_SET, Side.OPPONENT, "Opponent sets a Hidden card");
		assertThat(last().data()).isEmpty();
	}

	@Test
	@DisplayName("ENG-41: HIDDEN_SET di una Hidden in fila, stesso evento senza dati, messaggio diverso")
	void hiddenSetBehind() {
		log.hiddenSetBehind(Side.CHALLENGER);

		assertEvent(EventType.HIDDEN_SET, Side.CHALLENGER, "Challenger sets a Hidden card behind the other one");
		assertThat(last().data()).isEmpty();
	}

	@Test
	@DisplayName("HIDDEN_TRIGGERED: card ed effect")
	void hiddenTriggered() {
		log.hiddenTriggered(Side.CHALLENGER, LAST_SANCTUARY, "LAST_SANCTUARY");

		assertEvent(EventType.HIDDEN_TRIGGERED, Side.CHALLENGER, "Last Sanctuary triggers");
		assertThat(last().data()).containsExactly(entry("card", "Last Sanctuary"), entry("effect", "LAST_SANCTUARY"));
	}

	@Test
	@DisplayName("TERRAIN_PLACED: senza e con un Terreno sostituito (D-19)")
	void terrainPlaced() {
		log.terrainPlaced(Side.CHALLENGER, ECHO_RIFT, null);
		assertEvent(EventType.TERRAIN_PLACED, Side.CHALLENGER, "Echo Rift enters play");
		assertThat(last().data()).containsExactly(entry("card", "Echo Rift"), entry("replaced", null));

		log.terrainPlaced(Side.OPPONENT, WATCHING_RIFT, ECHO_RIFT);
		assertThat(last().message()).isEqualTo("Watching Rift enters play, replacing Echo Rift");
	}

	@Test
	@DisplayName("TERRAIN_REMOVED: card e cause")
	void terrainRemoved() {
		log.terrainRemoved(Side.OPPONENT, ECHO_RIFT, "The Null Witness");

		assertEvent(EventType.TERRAIN_REMOVED, Side.OPPONENT, "Echo Rift is removed by The Null Witness");
		assertThat(last().data()).containsExactly(entry("card", "Echo Rift"), entry("cause", "The Null Witness"));
	}

	@Test
	@DisplayName("DECK_REORDERED: source, card e position")
	void deckReordered() {
		log.deckReordered(Side.OPPONENT, "La Regina dei Sentieri Perduti", KAPPA, 3);

		assertEvent(EventType.DECK_REORDERED, Side.OPPONENT, "La Regina dei Sentieri Perduti moves Kappa to position 3");
		assertThat(last().data()).containsExactly(entry("source", "La Regina dei Sentieri Perduti"),
				entry("card", "Kappa"), entry("position", 3));
	}

	@Test
	@DisplayName("ITEM_PLACED: card")
	void itemPlaced() {
		log.itemPlaced(Side.OPPONENT, TORII_BELL);

		assertEvent(EventType.ITEM_PLACED, Side.OPPONENT, "Opponent places Campanello del Torii");
		assertThat(last().data()).containsExactly(entry("card", "Campanello del Torii"));
	}

	@Test
	@DisplayName("ITEM_CONSUMED: card, effect e creature")
	void itemConsumed() {
		log.itemConsumed(Side.OPPONENT, TORII_BELL, "TORII_BELL", "Kappa");

		assertEvent(EventType.ITEM_CONSUMED, Side.OPPONENT, "Campanello del Torii is used on Kappa");
		assertThat(last().data()).containsExactly(entry("card", "Campanello del Torii"), entry("effect", "TORII_BELL"),
				entry("creature", "Kappa"));
	}

	@Test
	@DisplayName("ROUND_STARTED: round, side nullo")
	void roundStarted() {
		log.roundStarted(7);

		assertEvent(EventType.ROUND_STARTED, null, "Round 7");
		assertThat(last().data()).containsExactly(entry("round", 7));
		assertThat(last().round()).isEqualTo(7);
	}

	@Test
	@DisplayName("ATTACK_COMPUTED: 4 base +2 fazione = 6; le parti a 0 non compaiono nel messaggio")
	void attackComputed() {
		log.attackComputed(TestCards.inPlay(CRAWLERS, Side.CHALLENGER), 2, 0, false, 6);
		assertEvent(EventType.ATTACK_COMPUTED, Side.CHALLENGER, "Crawlers attacks with 6 (4 base, +2 faction)");
		assertThat(last().data()).containsExactly(entry("attacker", "Crawlers"), entry("baseAttack", 4),
				entry("factionBonus", 2), entry("modifiers", 0), entry("effectBonus", 0), entry("frozen", false),
				entry("total", 6));

		log.attackComputed(TestCards.inPlay(KAPPA, Side.OPPONENT), 0, 0, false, 6);
		assertThat(last().message()).isEqualTo("Kappa attacks with 6 (6 base)");
	}

	@Test
	@DisplayName("ATTACK_COMPUTED: la modifica dell'effetto dell'attaccante compare come parte \"effect\"")
	void attackComputed_withEffectBonus() {
		log.attackComputed(TestCards.inPlay(KAPPA, Side.OPPONENT), 0, 2, false, 8);
		assertThat(last().message()).isEqualTo("Kappa attacks with 8 (6 base, +2 effect)");
		assertThat(last().data()).containsEntry("effectBonus", 2);
	}

	@Test
	@DisplayName("ATTACK_COMPUTED: il malus usa il segno meno: 6 base −1 fazione +3 modificatori")
	void attackComputed_withMalusAndModifiers() {
		// Arrange
		CreatureInPlay kappa = TestCards.inPlay(KAPPA, Side.OPPONENT);
		kappa.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, 3, ModifierDuration.rounds(1)));

		// Act
		log.attackComputed(kappa, -1, 0, false, 8);

		// Assert
		assertThat(last().message()).isEqualTo("Kappa attacks with 8 (6 base, −1 faction, +3 modifiers)");
		assertThat(last().data()).containsEntry("modifiers", 3);
	}

	@Test
	@DisplayName("ATTACK_COMPUTED (deciso il 18 novembre): un attacco congelato vale 0 e il messaggio dice frozen")
	void attackComputed_frozen() {
		log.attackComputed(TestCards.inPlay(KAPPA, Side.OPPONENT), 2, 0, true, 0);
		assertThat(last().message()).isEqualTo("Kappa attacks with 0 (6 base, +2 faction, frozen)");
		assertThat(last().data()).containsEntry("frozen", true).containsEntry("total", 0);
	}

	@Test
	@DisplayName("DAMAGE_DEALT: source, target, amount e targetDefense dopo il danno (anche negativa)")
	void damageDealt() {
		// Arrange
		CreatureInPlay crawlers = TestCards.inPlay(CRAWLERS, Side.CHALLENGER);
		CreatureInPlay kappa = TestCards.inPlay(KAPPA, Side.OPPONENT);
		kappa.takeDamage(10);

		// Act
		log.damageDealt(crawlers, kappa, 10);

		// Assert
		assertEvent(EventType.DAMAGE_DEALT, Side.CHALLENGER, "Crawlers deals 10 damage to Kappa (DEF −3)");
		assertThat(last().data()).containsExactly(entry("source", "Crawlers"), entry("target", "Kappa"),
				entry("amount", 10), entry("targetDefense", -3));
	}

	@Test
	@DisplayName("EFFECT_TRIGGERED: card e description")
	void effectTriggered() {
		log.effectTriggered(Side.OPPONENT, "Kappa", "+3 ATK in the first round");

		assertEvent(EventType.EFFECT_TRIGGERED, Side.OPPONENT, "Kappa: +3 ATK in the first round");
		assertThat(last().data()).containsExactly(entry("card", "Kappa"),
				entry("description", "+3 ATK in the first round"));
	}

	@Test
	@DisplayName("CREATURE_DIED: card")
	void creatureDied() {
		log.creatureDied(TestCards.inPlay(CRAWLERS, Side.CHALLENGER));

		assertEvent(EventType.CREATURE_DIED, Side.CHALLENGER, "Crawlers is destroyed");
		assertThat(last().data()).containsExactly(entry("card", "Crawlers"));
	}

	@Test
	@DisplayName("PLAYER_DAMAGED: amount e lifePoints rimasti, anche negativi (D-43)")
	void playerDamaged() {
		log.playerDamaged(Side.OPPONENT, 3, 27);
		assertEvent(EventType.PLAYER_DAMAGED, Side.OPPONENT, "Opponent loses 3 life points (27 left)");
		assertThat(last().data()).containsExactly(entry("amount", 3), entry("lifePoints", 27));

		log.playerDamaged(Side.OPPONENT, 35, -5);
		assertThat(last().message()).isEqualTo("Opponent loses 35 life points (−5 left)");
	}

	@Test
	@DisplayName("MATCH_ENDED: result ed endReason, side nullo")
	void matchEnded() {
		log.matchEnded(BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT);
		assertEvent(EventType.MATCH_ENDED, null, "Challenger wins (no creatures left)");
		assertThat(last().data()).containsExactly(entry("result", "CHALLENGER_WIN"),
				entry("endReason", "NO_CREATURES_LEFT"));

		log.matchEnded(BattleResult.DRAW, EndReason.ROUND_LIMIT);
		assertThat(last().message()).isEqualTo("Draw (round limit)");
	}

	private void logOneOfEach() {
		CreatureInPlay crawlers = TestCards.inPlay(CRAWLERS, Side.CHALLENGER);
		CreatureInPlay kappa = TestCards.inPlay(KAPPA, Side.OPPONENT);
		log.matchStarted(30);
		log.cardDrawn(Side.CHALLENGER, VEIL);
		log.creatureEntered(crawlers);
		log.spellResolved(Side.CHALLENGER, VEIL, "VEIL_OF_ANCHORED", null);
		log.burnRevealed(Side.CHALLENGER, CALL_OF_THE_RIFT, KAPPA, true);
		log.hiddenSet(Side.OPPONENT);
		log.hiddenTriggered(Side.OPPONENT, LAST_SANCTUARY, "LAST_SANCTUARY");
		log.terrainPlaced(Side.CHALLENGER, ECHO_RIFT, null);
		log.terrainRemoved(Side.OPPONENT, ECHO_RIFT, "The Null Witness");
		log.deckReordered(Side.OPPONENT, "La Regina dei Sentieri Perduti", KAPPA, 3);
		log.itemPlaced(Side.OPPONENT, TORII_BELL);
		log.itemConsumed(Side.OPPONENT, TORII_BELL, "TORII_BELL", "Kappa");
		log.roundStarted(1);
		log.attackComputed(crawlers, 2, 0, false, 6);
		log.damageDealt(crawlers, kappa, 6);
		log.effectTriggered(Side.OPPONENT, "Kappa", "+3 ATK in the first round");
		log.creatureDied(kappa);
		log.playerDamaged(Side.OPPONENT, 3, 27);
		log.matchEnded(BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT);
		assertThat(Arrays.asList(EventType.values())).hasSize(19);
	}

	private void assertEvent(EventType type, Side side, String message) {
		MatchEvent event = last();
		assertThat(event.type()).isEqualTo(type);
		assertThat(event.side()).isEqualTo(side);
		assertThat(event.message()).isEqualTo(message);
	}

	private MatchEvent last() {
		return log.events().getLast();
	}
}
