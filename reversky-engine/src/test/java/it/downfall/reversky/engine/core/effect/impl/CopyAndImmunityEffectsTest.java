package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti {@code P2} che copiano l'effetto avversario o ne sono immuni (ENG-38): Kitsune e Il Custode delle Porte
 * Incrociate. Si provano attraverso il {@link RoundResolver}.
 */
class CopyAndImmunityEffectsTest {

	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0",
			TestFactions.ARCHIVE, "SEALED_FILE_ZERO");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- Kitsune ----

	@Test
	@DisplayName("Kitsune: copia l'effetto dell'avv. (che tiene il suo), +3 DEF e sigilla 3 punti di bonus ATK")
	void kitsune_copiesSealsAndGainsDefense() {
		// Arrange: Wampus Cat 5 con +2 ATK da una Spell
		MatchState state = inCombat(kitsune(4, 20), wampusCat());
		bonus(enemy(state), 2);

		// Act
		resolver.startCombat(state);

		// Assert: Wampus Cat 5 + 2 sigillati − 1 della copia = 4; Kitsune 4 − 1 del Wampus Cat vero = 3
		assertThat(enemy(state).attack()).isEqualTo(4);
		assertThat(self(state).attack()).isEqualTo(3);
		assertThat(self(state).defense()).isEqualTo(23);
		assertThat(self(state).borrowedEffect()).map(CreatureEffect::code).contains(WampusCatEffect.CODE);
		assertThat(enemy(state).isEffectCancelled()).isFalse();
		assertThat(messages(state)).containsSubsequence("Kitsune: copies Wampus Cat's effect, +3 DEF (DEF 23), "
				+ "seals 3 points of Wampus Cat's ATK bonuses (ATK 5)",
				"Kitsune: Wampus Cat −1 ATK while in play (ATK 4)");
	}

	@Test
	@DisplayName("Kitsune: contro una creatura senza effetto non copia e non prende DEF, ma sigilla")
	void kitsune_noEffect_sealOnly() {
		// Arrange
		MatchState state = inCombat(kitsune(4, 20), plain("B", 5, 40));
		bonus(enemy(state), 2);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(self(state).borrowedEffect()).isEmpty();
		assertThat(messages(state)).contains("Kitsune: seals 3 points of B's ATK bonuses (ATK 5)");
	}

	@Test
	@DisplayName("Kitsune contro Kitsune: nessuna copia e nessuna DEF")
	void kitsune_againstKitsune_noCopy() {
		// Arrange
		MatchState state = inCombat(kitsune(4, 20), kitsune(4, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(enemy(state).defense()).isEqualTo(20);
		assertThat(self(state).borrowedEffect()).isEmpty();
		assertThat(enemy(state).borrowedEffect()).isEmpty();
	}

	@Test
	@DisplayName("Kitsune: la copia sparisce a fine scontro, il bonus ATK che ha già dato resta")
	void kitsune_copyEndsWithCombat_gainsStay() {
		// Arrange: Kitsune copia Wretches (+2 ATK alla prima ferita) e la uccide al primo round
		MatchState state = inCombat(kitsune(4, 20),
				TestCards.creature(0, "Wretches", TestFactions.WOUND, null, "Uncommon", 3, 3, WretchesEffect.CODE));
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(self(state).attack()).isEqualTo(6);
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.OPPONENT));

		// Act: Kitsune è ferita di nuovo, senza copia
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(self(state).borrowedEffect()).isEmpty();
		assertThat(self(state).attack()).isEqualTo(6);
		assertThat(enemy(state).defense()).isEqualTo(24);
	}

	@Test
	@DisplayName("Kitsune contro Baku: Baku divora la copia stessa (niente copia, sigillo e DEF) e prende +2 DEF")
	void kitsune_devouredByBaku() {
		// Arrange
		MatchState state = inCombat(kitsune(4, 20),
				TestCards.creature(0, "Baku", TestFactions.DREAM, null, "Mythic", 3, 20, BakuEffect.CODE));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).isEffectCancelled()).isTrue();
		assertThat(self(state).borrowedEffect()).isEmpty();
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(enemy(state).defense()).isEqualTo(22);
		assertThat(messages(state)).noneMatch(message -> message.startsWith("Kitsune:"));
	}

	@Test
	@DisplayName("Kitsune copia anche un effetto annullato: la copia è una nuova istanza")
	void kitsune_copiesCancelledEffect() {
		// Arrange: l'effetto di Wampus Cat è già annullato
		MatchState state = inCombat(kitsune(4, 20), wampusCat());
		enemy(state).cancelEffect(ModifierDuration.whileInPlay());

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(23);
		assertThat(self(state).attack()).isEqualTo(4);
		assertThat(enemy(state).attack()).isEqualTo(4);
	}

	@Test
	@DisplayName("Kitsune non copia e non sigilla Il Custode delle Porte Incrociate")
	void kitsune_againstKeeper_nothing() {
		// Arrange
		MatchState state = inCombat(kitsune(4, 20), keeper(5, 20));
		bonus(enemy(state), 2);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(self(state).borrowedEffect()).isEmpty();
		assertThat(enemy(state).attack()).isEqualTo(7);
	}

	// ---- Il Custode delle Porte Incrociate ----

	@Test
	@DisplayName("Custode: immune ai danni di un effetto avversario; +2 ATK contro una creatura con effetto")
	void keeper_immuneToEffectDamage_bonusAgainstEffect() {
		// Arrange
		MatchState state = inCombat(keeper(5, 20), mothman(5, 20));
		resolver.startCombat(state);
		assertThat(self(state).defense()).isEqualTo(20);

		// Act
		resolver.resolve(state);

		// Assert: 5 + 2 = 7 danni a Mothman, l'attacco di Mothman colpisce
		assertThat(enemy(state).defense()).isEqualTo(13);
		assertThat(self(state).defense()).isEqualTo(15);
		assertThat(messages(state)).contains("The Crossed Doors Keeper: +2 ATK against Mothman");
	}

	@Test
	@DisplayName("Custode: niente +2 ATK contro una creatura senza effetto")
	void keeper_noBonusAgainstPlain() {
		// Arrange
		MatchState state = inCombat(keeper(5, 20), plain("B", 5, 40));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(35);
	}

	@Test
	@DisplayName("Custode: immune al malus ATK di un effetto di entrata avversario")
	void keeper_immuneToMalus() {
		// Arrange
		MatchState state = inCombat(keeper(5, 20), wampusCat());

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).attack()).isEqualTo(5);
		assertThat(messages(state)).noneMatch(message -> message.startsWith("Wampus Cat:"));
	}

	@Test
	@DisplayName("Custode: il Predicatore sotto la Pioggia non lo annulla e non gli toglie ATK")
	void keeper_notCancelledByPreacher() {
		// Arrange
		MatchState state = inCombat(TestCards.creature(0, "The Rain Preacher", TestFactions.INVITATION, null, "Rare",
				3, 20, RainPreacherEffect.CODE), keeper(5, 20));

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: il Custode attacca con 5 + 2
		assertThat(enemy(state).isEffectCancelled()).isFalse();
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(self(state).defense()).isEqualTo(13);
	}

	@Test
	@DisplayName("Custode: Baku non lo divora")
	void keeper_notDevouredByBaku() {
		// Arrange
		MatchState state = inCombat(TestCards.creature(0, "Baku", TestFactions.DREAM, null, "Mythic", 3, 20,
				BakuEffect.CODE), keeper(5, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).isEffectCancelled()).isFalse();
		assertThat(self(state).defense()).isEqualTo(20);
	}

	@Test
	@DisplayName("Custode: il filtro viene prima del Fascicolo Sigillato 0, che non si consuma")
	void keeper_sealedFileNotConsumed() {
		// Arrange
		MatchState state = inCombat(keeper(5, 20), mothman(5, 20));
		state.challenger().placeItem(SEALED_FILE);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(state.challenger().items()).containsExactly(SEALED_FILE);
	}

	@Test
	@DisplayName("Custode: ignora i modificatori in attesa degli effetti avversari, non quelli delle Spell")
	void keeper_ignoresPendingFromEnemyEffects() {
		// Arrange: un −1 DEF in attesa da Cervo Bianco e 2 danni in attesa da una Spell dell'avversario
		MatchState state = inCombat(keeper(5, 20), plain("B", 5, 40));
		state.challenger().addPendingModifier(new PendingModifier("White Stag", ModifierSource.EFFECT,
				Side.OPPONENT, 0, 0, 1));
		state.challenger().addPendingModifier(new PendingModifier("Bite of the Reverse", ModifierSource.SPELL,
				Side.OPPONENT, 0, 0, 2));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(18);
		assertThat(state.challenger().pendingModifiers()).isEmpty();
		assertThat(messages(state)).noneMatch(message -> message.contains("from White Stag"));
		assertThat(messages(state)).anyMatch(message -> message.contains("from Bite of the Reverse"));
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
	}

	private static void bonus(CreatureInPlay creature, int amount) {
		creature.addModifier(StatModifier.attack(ModifierSource.SPELL, creature.side(), amount,
				ModifierDuration.whileInPlay()));
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

	private static CardDefinition kitsune(int attack, int defense) {
		return TestCards.creature(0, "Kitsune", TestFactions.DREAM, null, "Legendary", attack, defense,
				KitsuneEffect.CODE);
	}

	private static CardDefinition keeper(int attack, int defense) {
		return TestCards.creature(0, "The Crossed Doors Keeper", TestFactions.WOUND, null, "Mythic", attack,
				defense, CrossedDoorsKeeperEffect.CODE);
	}

	private static CardDefinition mothman(int attack, int defense) {
		return TestCards.creature(0, "Mothman", TestFactions.INVITATION, null, "Mythic", attack, defense,
				MothmanEffect.CODE);
	}

	private static CardDefinition wampusCat() {
		return TestCards.creature(0, "Wampus Cat", TestFactions.INVITATION, null, "Uncommon", 5, 20,
				WampusCatEffect.CODE);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
