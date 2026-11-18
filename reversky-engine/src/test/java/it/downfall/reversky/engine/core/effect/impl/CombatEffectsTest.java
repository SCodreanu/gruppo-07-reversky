package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
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
 * Effetti di Theory of the Invitation e Theory of the Archive che agiscono durante lo scontro (ENG-25): a inizio
 * scontro ({@code onCombatStart}), a inizio round, sui danni ricevuti e alla morte. Si provano attraverso il
 * {@link RoundResolver}.
 */
class CombatEffectsTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- inizio scontro ----

	@Test
	@DisplayName("Mothman: 3 danni a ogni avversaria che affronta, all'entrata e quando ne entra una nuova")
	void mothman_damagesEachEnemy() {
		// Arrange: B 3/8 muore al round 1
		MatchState state = inCombat(creature("Mothman", "Mythic", 5, 20, "MOTHMAN"), plain("B", 3, 8));

		// Act + Assert
		resolver.startCombat(state);
		assertThat(enemy(state).defense()).isEqualTo(5);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 3, 10), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(enemy(state).defense()).isEqualTo(7);
	}

	@Test
	@DisplayName("Spearfinger: +2 ATK nel primo round contro ogni nuova avversaria (6, poi 4; di nuovo 6 contro C)")
	void spearfinger_firstRoundAgainstEachEnemy() {
		// Arrange: B 1/10 muore al round 2
		MatchState state = inCombat(creature("Spearfinger", "Rare", 4, 30, "SPEARFINGER"), plain("B", 1, 10));
		CreatureInPlay spearfinger = state.challenger().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(spearfinger.attack()).isEqualTo(6);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(4);
		assertThat(spearfinger.attack()).isEqualTo(4);
		resolver.resolve(state);
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(spearfinger.attack()).isEqualTo(6);
	}

	@Test
	@DisplayName("Bigfoot contro una Rare: +4 ATK e 2 danni in meno da ogni suo attacco; contro una Common +2 ATK")
	void bigfoot_dependsOnEnemyRarity() {
		// Arrange: B Rare 6/9 muore al round 1
		MatchState state = inCombat(creature("Bigfoot", "Mythic", 5, 30, "BIGFOOT"),
				creature("B", "Rare", 6, 9, null));
		CreatureInPlay bigfoot = state.challenger().creature().orElseThrow();

		// Act + Assert: contro la Rare
		resolver.startCombat(state);
		assertThat(bigfoot.attack()).isEqualTo(9);
		resolver.resolve(state);
		assertThat(bigfoot.defense()).isEqualTo(26);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(bigfoot.attack()).isEqualTo(5);

		// Act + Assert: contro la Common nessuna prevenzione
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 6, 30), Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(bigfoot.attack()).isEqualTo(7);
		resolver.resolve(state);
		assertThat(bigfoot.defense()).isEqualTo(20);
	}

	@Test
	@DisplayName("D-20: per Bigfoot anche una Mutant conta come Rare o superiore")
	void bigfoot_mutantCountsAsRareOrHigher() {
		// Arrange
		MatchState state = inCombat(creature("Bigfoot", "Mythic", 5, 30, "BIGFOOT"),
				creature("B", "Mutant", 6, 30, null));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(9);
	}

	// ---- inizio round ----

	@Test
	@DisplayName("Bell Witch: 2 danni a inizio del suo secondo round, una volta (B 27, 22, 19)")
	void bellWitch_secondRoundStrike() {
		// Arrange
		MatchState state = inCombat(creature("Bell Witch", "Rare", 3, 30, "BELL_WITCH"), plain("B", 2, 30));
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(27);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(22);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(19);
	}

	@Test
	@DisplayName("Bell Witch alla morte: la prossima creatura avversaria entra con −2 DEF")
	void bellWitch_deathPreparesNextEnemy() {
		// Arrange: Bell Witch 3/2 muore al round 1
		MatchState state = inCombat(creature("Bell Witch", "Rare", 3, 2, "BELL_WITCH"), plain("B", 5, 30));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().pendingModifiers()).containsExactly(
				new PendingModifier("Bell Witch", ModifierSource.EFFECT, Side.CHALLENGER, 0, 0, 2));
	}

	@Test
	@DisplayName("The Thorned Matriarch: con DEF più alta sacrifica 2 e fa 4 danni; una morte a inizio round lo ferma")
	void thornedMatriarch_sacrificesWhileHigher() {
		// Arrange
		MatchState state = inCombat(creature("Matriarch", "Mythic", 3, 20, "THORNED_MATRIARCH"), plain("B", 2, 10));
		CreatureInPlay matriarch = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert: round 1, 20 > 10: Matriarch 18, B 6, poi gli attacchi: B 3, Matriarch 16
		resolver.resolve(state);
		assertThat(matriarch.defense()).isEqualTo(16);
		assertThat(enemy(state).defense()).isEqualTo(3);

		// Act + Assert: round 2, 16 > 3: B −1 muore a inizio round, nessuno attacca
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(matriarch.defense()).isEqualTo(14);
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 1);
	}

	@Test
	@DisplayName("The Thorned Matriarch: con DEF uguale non fa niente; con DEF 2 contro 1 il sacrificio la uccide")
	void thornedMatriarch_equalDoesNothing_sacrificeCanKill() {
		// Arrange
		MatchState equal = inCombat(creature("Matriarch", "Mythic", 3, 10, "THORNED_MATRIARCH"), plain("B", 0, 10));
		MatchState lethal = inCombat(creature("Matriarch", "Mythic", 3, 2, "THORNED_MATRIARCH"), plain("B", 2, 1));
		resolver.startCombat(equal);
		resolver.startCombat(lethal);

		// Act
		resolver.resolve(equal);
		resolver.resolve(lethal);

		// Assert: muoiono entrambe a inizio round, nessuno perde punti vita
		assertThat(equal.challenger().creature().orElseThrow().defense()).isEqualTo(10);
		assertThat(lethal.challenger().creature()).isEmpty();
		assertThat(lethal.opponent().creature()).isEmpty();
		assertThat(lethal.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(lethal.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
	}

	@Test
	@DisplayName("SCP-001: 4 danni all'entrata e a inizio dei suoi primi 3 round (B 36, 31, 26, 21, poi 20)")
	void scp001_entryAndThreeRounds() {
		// Arrange
		MatchState state = inCombat(creature("SCP-001", "Legendary", 1, 40, "SCP_001"), plain("B", 1, 40));

		// Act + Assert
		resolver.startCombat(state);
		assertThat(enemy(state).defense()).isEqualTo(36);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(31);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(26);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(21);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(20);
	}

	@Test
	@DisplayName("SCP-001: i colpi restanti vanno alla nuova avversaria; i round si contano dall'entrata")
	void scp001_remainingStrikesHitNewEnemy() {
		// Arrange: B 1/5 scende a 1 all'entrata e muore a inizio del round 1
		MatchState state = inCombat(creature("SCP-001", "Legendary", 1, 40, "SCP_001"), plain("B", 1, 5));
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();

		// Act: C entra; restano i colpi dei round 2 e 3
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 1, 40), Side.OPPONENT));
		resolver.startCombat(state);
		CreatureInPlay c = enemy(state);

		// Assert
		assertThat(c.defense()).isEqualTo(40);
		resolver.resolve(state);
		assertThat(c.defense()).isEqualTo(35);
		resolver.resolve(state);
		assertThat(c.defense()).isEqualTo(30);
		resolver.resolve(state);
		assertThat(c.defense()).isEqualTo(29);
	}

	// ---- danni ricevuti ----

	@Test
	@DisplayName("SCP-106: riflette 3 danni per ciascuno dei primi due attacchi (B 25, 20, poi 18)")
	void scp106_reflectsFirstTwoAttacks() {
		// Arrange
		MatchState state = inCombat(creature("SCP-106", "Rare", 2, 30, "SCP_106"), plain("B", 4, 30));
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(25);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(20);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(18);
	}

	@Test
	@DisplayName("SCP-106 riflette anche se l'attacco lo uccide: muoiono entrambe, nessuno perde punti vita")
	void scp106_reflectsWhenDying() {
		// Arrange: SCP-106 3 − 4 = −1; B 3 − 2 − 3 = −2
		MatchState state = inCombat(creature("SCP-106", "Rare", 2, 3, "SCP_106"), plain("B", 4, 3));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
	}

	@Test
	@DisplayName("SCP-682: il primo attacco arriva intero, ogni attacco dopo fa 2 danni in meno (30 → 25, 22, 19)")
	void scp682_adapts() {
		// Arrange
		MatchState state = inCombat(creature("SCP-682", "Rare", 1, 30, "SCP_682"), plain("B", 5, 30));
		CreatureInPlay scp = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(scp.defense()).isEqualTo(25);
		resolver.resolve(state);
		assertThat(scp.defense()).isEqualTo(22);
		resolver.resolve(state);
		assertThat(scp.defense()).isEqualTo(19);
	}

	@Test
	@DisplayName("SCP-3000: +2 ATK; dimezza per difetto i danni di una Rare (7 → 3), non quelli di una Common")
	void scp3000_halvesRareDamage() {
		// Arrange
		MatchState rare = inCombat(creature("SCP-3000", "Mythic", 1, 30, "SCP_3000"),
				creature("B", "Rare", 7, 30, null));
		MatchState common = inCombat(creature("SCP-3000", "Mythic", 1, 30, "SCP_3000"), plain("C", 7, 30));
		resolver.startCombat(rare);
		resolver.startCombat(common);

		// Act
		resolver.resolve(rare);
		resolver.resolve(common);

		// Assert
		assertThat(rare.challenger().creature().orElseThrow().attack()).isEqualTo(3);
		assertThat(rare.challenger().creature().orElseThrow().defense()).isEqualTo(27);
		assertThat(common.challenger().creature().orElseThrow().defense()).isEqualTo(23);
	}

	// ---- morte ----

	@Test
	@DisplayName("Il Pastore Muto: alla morte la prossima creatura del proprietario entra con uno scudo di 2 DEF")
	void silentShepherd_shieldsNextCreature() {
		// Arrange: il Pastore 3/2 muore al round 1
		MatchState state = inCombat(creature("Shepherd", "Uncommon", 3, 2, "SILENT_SHEPHERD"), plain("B", 5, 30));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Act
		state.challenger().placeCreature(new CreatureInPlay(plain("D", 3, 5), Side.CHALLENGER));
		resolver.startCombat(state);

		// Assert
		CreatureInPlay d = state.challenger().creature().orElseThrow();
		assertThat(d.defense()).isEqualTo(7);
		assertThat(d.modifiers()).singleElement()
				.satisfies(modifier -> assertThat(modifier.shieldValue()).isEqualTo(2));
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
	}

	private static CreatureInPlay enemy(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}

	private static CardDefinition creature(String name, String rarity, int attack, int defense, String code) {
		return TestCards.creature(0, name, TestFactions.INVITATION, null, rarity, attack, defense, code);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
