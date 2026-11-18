package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
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
 * Effetti all'entrata in campo ({@code onEnterPlay}, passo 0) di Theory of the Invitation e Theory of the Archive
 * (ENG-25). Si provano attraverso il {@link RoundResolver}.
 */
class EntryEffectsTest {

	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
			"SEALED_FILE_ZERO");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("SCP-173: all'entrata 2 danni all'avversaria (10 → 8)")
	void scp173_entryDamage() {
		// Arrange
		MatchState state = inCombat(creature("SCP-173", 2, 10, "SCP_173"), plain("B", 3, 10));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemyOfChallenger(state).defense()).isEqualTo(8);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("SCP-173: deals 2 damage to B (DEF 8)"));
	}

	@Test
	@DisplayName("18 novembre: SCP-173 uccide all'entrata il Cervo Bianco prima del suo effetto, che non scatta")
	void entryKill_otherEntryEffectDoesNotRun() {
		// Arrange: Cervo Bianco 3/2, appena entrato
		MatchState state = inCombat(creature("SCP-173", 2, 10, "SCP_173"),
				creature("White Stag", 3, 2, "WHITE_STAG"));

		// Act
		resolver.startCombat(state);

		// Assert: il Cervo muore con DEF 0, SCP-173 resta a 10 e nessun −1 aspetta lo sfidante
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(10);
		assertThat(state.challenger().pendingModifiers()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "WHITE_STAG", "NAME_COLLECTOR" })
	@DisplayName("−1 DEF all'avversaria in campo e −1 DEF alla prossima creatura avversaria che entra")
	void entryAndNext_damagesBoth(String code) {
		// Arrange
		MatchState state = inCombat(creature("Hunter", 9, 30, code), plain("B", 3, 9));

		// Act + Assert: B 9 → 8, poi muore al round 1
		resolver.startCombat(state);
		assertThat(enemyOfChallenger(state).defense()).isEqualTo(8);
		assertThat(state.opponent().pendingModifiers()).containsExactly(
				new PendingModifier("Hunter", ModifierSource.EFFECT, Side.CHALLENGER, 0, 0, 1));
		resolver.resolve(state);

		// Act + Assert: C entra con −1
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 3, 10), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(enemyOfChallenger(state).defense()).isEqualTo(9);
		assertThat(state.opponent().pendingModifiers()).isEmpty();
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0: annulla il −1 di ora, non quello in attesa, che arriva alla creatura dopo")
	void entryAndNext_sealedFileCancelsOnlyTheFirstHit() {
		// Arrange
		MatchState state = inCombat(creature("White Stag", 9, 30, "WHITE_STAG"), plain("B", 3, 9));
		state.opponent().placeItem(SEALED_FILE);

		// Act + Assert
		resolver.startCombat(state);
		assertThat(enemyOfChallenger(state).defense()).isEqualTo(9);
		assertThat(state.opponent().items()).isEmpty();
		assertThat(state.opponent().pendingModifiers()).hasSize(1);
		resolver.resolve(state);
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 3, 10), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(enemyOfChallenger(state).defense()).isEqualTo(9);
	}

	@Test
	@DisplayName("SCP-049: l'avversaria attacca con −2 nel primo round, −1 nel secondo, poi pieno (6 → 4, 5, 6)")
	void scp049_fadingPenalty() {
		// Arrange
		MatchState state = inCombat(creature("SCP-049", 3, 30, "SCP_049"), plain("B", 6, 30));
		CreatureInPlay b = state.opponent().creature().orElseThrow();
		CreatureInPlay scp = state.challenger().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(b.attack()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(scp.defense()).isEqualTo(26);
		assertThat(b.attack()).isEqualTo(5);
		resolver.resolve(state);
		assertThat(scp.defense()).isEqualTo(21);
		assertThat(b.attack()).isEqualTo(6);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "FOOTPRINT_GATHERER", "INDEX_MAW" })
	@DisplayName("+2 DEF permanente all'entrata, compresa nella DEF all'entrata (5 → 7)")
	void entryDefense_permanent(String code) {
		// Arrange
		MatchState state = inCombat(creature("Keeper", 3, 5, code), plain("B", 3, 30));

		// Act
		resolver.startCombat(state);

		// Assert
		CreatureInPlay keeper = state.challenger().creature().orElseThrow();
		assertThat(keeper.defense()).isEqualTo(7);
		assertThat(keeper.initialDefense()).isEqualTo(7);
		assertThat(keeper.modifiers()).isEmpty();
	}

	@Test
	@DisplayName("Wampus Cat: −1 ATK all'avversaria finché resta in campo; una nuova avversaria non lo riceve")
	void wampusCat_entryPenaltyOnly() {
		// Arrange: B 5/4 muore al round 2
		MatchState state = inCombat(creature("Wampus Cat", 3, 30, "WAMPUS_CAT"), plain("B", 5, 4));
		CreatureInPlay b = state.opponent().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(b.attack()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(b.attack()).isEqualTo(4);
		resolver.resolve(state);
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 5, 30), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(state.opponent().creature().orElseThrow().attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("The Nameless Caller: 1 danno per ogni creatura avversaria già morta, massimo 3")
	void namelessCaller_damagePerLostEnemy() {
		// Arrange: due creature avversarie già sconfitte
		MatchState two = inCombat(creature("Caller", 3, 10, "NAMELESS_CALLER"), plain("B", 3, 10));
		two.challenger().recordDefeatedCreature();
		two.challenger().recordDefeatedCreature();
		MatchState five = inCombat(creature("Caller", 3, 10, "NAMELESS_CALLER"), plain("B", 3, 10));
		for (int i = 0; i < 5; i++) {
			five.challenger().recordDefeatedCreature();
		}
		MatchState none = inCombat(creature("Caller", 3, 10, "NAMELESS_CALLER"), plain("B", 3, 10));

		// Act
		resolver.startCombat(two);
		resolver.startCombat(five);
		resolver.startCombat(none);

		// Assert
		assertThat(enemyOfChallenger(two).defense()).isEqualTo(8);
		assertThat(enemyOfChallenger(five).defense()).isEqualTo(7);
		assertThat(enemyOfChallenger(none).defense()).isEqualTo(10);
		assertThat(none.log().events()).extracting(MatchEvent::type).doesNotContain(EventType.EFFECT_TRIGGERED);
	}

	@Test
	@DisplayName("SCP-053: all'entrata avv. −3 DEF e +2 ATK nel primo round (3 → 5, poi 3)")
	void scp053_damageAndEnemyBonus() {
		// Arrange
		MatchState state = inCombat(creature("SCP-053", 3, 30, "SCP_053"), plain("B", 3, 10));
		CreatureInPlay b = state.opponent().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(b.defense()).isEqualTo(7);
		assertThat(b.attack()).isEqualTo(5);
		resolver.resolve(state);
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(25);
		assertThat(b.attack()).isEqualTo(3);
	}

	@Test
	@DisplayName("SCP-2006: prende l'ATK base più alto dell'avversaria come base (2 → 6); se è più basso non cambia")
	void scp2006_copiesHigherBaseAttack() {
		// Arrange
		MatchState higher = inCombat(creature("SCP-2006", 2, 10, "SCP_2006"), plain("B", 6, 10));
		MatchState lower = inCombat(creature("SCP-2006", 2, 10, "SCP_2006"), plain("B", 1, 10));

		// Act
		resolver.startCombat(higher);
		resolver.startCombat(lower);
		resolver.resolve(higher);

		// Assert: è l'ATK base, non un modificatore
		CreatureInPlay copier = higher.challenger().creature().orElseThrow();
		assertThat(copier.baseAttack()).isEqualTo(6);
		assertThat(copier.modifiers()).isEmpty();
		assertThat(lower.challenger().creature().orElseThrow().baseAttack()).isEqualTo(2);
		assertThat(higher.log().events()).anySatisfy(event -> assertThat(event.message())
				.isEqualTo("SCP-2006 attacks with 6 (6 base)"));
	}

	@Test
	@DisplayName("The Drowned Memory: +1 DEF per alleata eliminata (massimo +4), +1 ATK se sono almeno 3")
	void drownedMemory_scalesWithAlliesLost() {
		// Arrange: 3 e 5 creature dello sfidante già eliminate
		MatchState three = inCombat(creature("Memory", 3, 5, "DROWNED_MEMORY"), plain("B", 3, 30));
		MatchState five = inCombat(creature("Memory", 3, 5, "DROWNED_MEMORY"), plain("B", 3, 30));
		for (int i = 0; i < 3; i++) {
			three.opponent().recordDefeatedCreature();
		}
		for (int i = 0; i < 5; i++) {
			five.opponent().recordDefeatedCreature();
		}

		// Act
		resolver.startCombat(three);
		resolver.startCombat(five);

		// Assert
		assertThat(three.challenger().creature().orElseThrow().defense()).isEqualTo(8);
		assertThat(three.challenger().creature().orElseThrow().attack()).isEqualTo(4);
		assertThat(five.challenger().creature().orElseThrow().defense()).isEqualTo(9);
		assertThat(five.challenger().creature().orElseThrow().attack()).isEqualTo(4);
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
	}

	private static CreatureInPlay enemyOfChallenger(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}

	private static CardDefinition creature(String name, int attack, int defense, String code) {
		return TestCards.creature(0, name, TestFactions.ARCHIVE, null, "Uncommon", attack, defense, code);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
