package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.core.simulation.TerrainService;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti {@code P2} che annullano gli effetti avversari (ENG-38, D-30, M-10): Il Predicatore sotto la Pioggia,
 * SCP-2521 (che ruba l'effetto annullato) e Baku (che divora le attivazioni). Si provano attraverso il
 * {@link RoundResolver}.
 */
class NullifyEffectsTest {

	private static final CardDefinition ECHO_RIFT = TestCards.terrain(113, "Echo Rift", TestFactions.WOUND,
			"ECHO_RIFT");
	private static final CardDefinition WATCHING_RIFT = TestCards.terrain(115, "Watching Rift", TestFactions.ARCHIVE,
			"WATCHING_RIFT");
	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0",
			TestFactions.ARCHIVE, "SEALED_FILE_ZERO");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);
	private final TerrainService terrains = new TerrainService(registry);

	// ---- Il Predicatore sotto la Pioggia ----

	@Test
	@DisplayName("Predicatore: annulla l'entrata dell'avv. anche da avversario (NULLIFY prima dello sfidante)")
	void preacher_cancelsEntryEffect_beforeChallenger() {
		// Arrange: Wampus Cat (−1 ATK all'avv.) è lo sfidante
		MatchState state = inCombat(creature("Wampus Cat", "Uncommon", 5, 20, WampusCatEffect.CODE),
				preacher(3, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(3);
		assertThat(self(state).isEffectCancelled()).isTrue();
		assertThat(messages(state)).contains("The Rain Preacher: cancels Wampus Cat's effect while it stays in play");
		assertThat(messages(state)).noneMatch(message -> message.startsWith("Wampus Cat:"));
	}

	@Test
	@DisplayName("Predicatore: contro una creatura senza effetto, −2 ATK finché resta in campo")
	void preacher_noEffect_attackPenalty() {
		// Arrange
		MatchState state = inCombat(preacher(3, 20), plain("B", 5, 40));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(3);
		assertThat(enemy(state).isEffectCancelled()).isFalse();
		assertThat(messages(state)).contains("The Rain Preacher: B −2 ATK while in play (ATK 3)");
	}

	@Test
	@DisplayName("Predicatore contro The Hollow Wyrm: +3 ATK sospeso, +3 DEF resta, i nuovi Terreni restano")
	void preacher_cancelledHollowWyrm() {
		// Arrange: il Wyrm inghiotte Echo Rift (7/8) e uccide B
		MatchState state = inCombat(hollowWyrm(), plain("B", 1, 1));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);
		resolver.startCombat(state);
		resolver.resolve(state);
		state.opponent().placeCreature(TestStates.withEffect(registry, preacher(3, 20), Side.OPPONENT));

		// Act
		resolver.startCombat(state);
		terrains.place(state, state.opponent(), WATCHING_RIFT);

		// Assert: ATK 4 (il bonus è sospeso), DEF 8 − 1 = 7, Watching Rift in gioco
		assertThat(self(state).attack()).isEqualTo(4);
		assertThat(self(state).defense()).isEqualTo(7);
		assertThat(state.terrain()).map(placed -> placed.card().name()).contains("Watching Rift");
	}

	@Test
	@DisplayName("Predicatore: l'annullamento resta all'avversaria per tutta la permanenza, anche dopo")
	void preacher_cancelOutlivesPreacher() {
		// Arrange: il Predicatore 1/1 muore al primo round contro Smiler
		MatchState state = inCombat(preacher(1, 1), creature("Smiler", "Rare", 3, 40, SmilerEffect.CODE));
		resolver.startCombat(state);
		resolver.resolve(state);
		state.challenger().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.CHALLENGER));

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: Smiler non cresce e non toglie ATK a C
		assertThat(enemy(state).attack()).isEqualTo(3);
		assertThat(self(state).attack()).isEqualTo(1);
		assertThat(self(state).defense()).isEqualTo(27);
		assertThat(messages(state)).noneMatch(message -> message.startsWith("Smiler:"));
	}

	@Test
	@DisplayName("Predicatore: il Fascicolo Sigillato 0 dell'avversario annulla l'annullamento e si consuma")
	void preacher_sealedFileStopsTheCancel() {
		// Arrange
		MatchState state = inCombat(preacher(3, 20), creature("Wampus Cat", "Uncommon", 5, 20, WampusCatEffect.CODE));
		state.opponent().placeItem(SEALED_FILE);

		// Act
		resolver.startCombat(state);

		// Assert: Wampus Cat tiene il suo effetto e toglie 1 ATK al Predicatore
		assertThat(state.opponent().items()).isEmpty();
		assertThat(enemy(state).isEffectCancelled()).isFalse();
		assertThat(self(state).attack()).isEqualTo(2);
	}

	// ---- SCP-2521 ----

	@Test
	@DisplayName("SCP-2521: annulla l'effetto di una Rare o inferiore e lo usa come proprio, appena entrato")
	void scp2521_stealsRareOrLower() {
		// Arrange
		MatchState state = inCombat(scp2521(4, 20), creature("Wampus Cat", "Uncommon", 5, 20, WampusCatEffect.CODE));

		// Act
		resolver.startCombat(state);

		// Assert: il −1 ATK rubato colpisce Wampus Cat, il suo non colpisce SCP-2521
		assertThat(enemy(state).isEffectCancelled()).isTrue();
		assertThat(enemy(state).attack()).isEqualTo(4);
		assertThat(self(state).attack()).isEqualTo(4);
		assertThat(self(state).borrowedEffect()).map(CreatureEffect::code).contains(WampusCatEffect.CODE);
		assertThat(messages(state)).containsSubsequence(
				"SCP-2521: cancels Wampus Cat's effect while it stays in play and steals it",
				"SCP-2521: Wampus Cat −1 ATK while in play (ATK 4)");
	}

	@Test
	@DisplayName("SCP-2521: contro una Mythic annulla soltanto")
	void scp2521_mythic_onlyCancels() {
		// Arrange
		MatchState state = inCombat(scp2521(4, 20), creature("Mothman", "Mythic", 5, 20, MothmanEffect.CODE));

		// Act
		resolver.startCombat(state);

		// Assert: niente 3 danni da Mothman, niente furto
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(self(state).borrowedEffect()).isEmpty();
		assertThat(messages(state)).contains("SCP-2521: cancels Mothman's effect while it stays in play");
	}

	@Test
	@DisplayName("SCP-2521: l'effetto rubato resta anche dopo la morte della vittima e scatta contro la prossima")
	void scp2521_stolenEffectOutlivesVictim() {
		// Arrange: un Mothman Uncommon di prova muore per il suo stesso effetto, rubato
		MatchState state = inCombat(scp2521(4, 20), creature("Moth", "Uncommon", 1, 3, MothmanEffect.CODE));
		resolver.startCombat(state);
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.OPPONENT));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(27);
		assertThat(messages(state)).containsSubsequence("SCP-2521: deals 3 damage to Moth (DEF 0)",
				"SCP-2521: deals 3 damage to C (DEF 27)");
	}

	@Test
	@DisplayName("SCP-2521 contro SCP-2521: lo sfidante annulla e ruba; l'effetto rubato non ruba di nuovo")
	void scp2521_againstScp2521_noLoop() {
		// Arrange
		MatchState state = inCombat(scp2521(4, 20), scp2521(4, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).isEffectCancelled()).isFalse();
		assertThat(enemy(state).isEffectCancelled()).isTrue();
		assertThat(self(state).effects()).hasSize(2);
		assertThat(messages(state)).filteredOn(message -> message.endsWith("steals it")).hasSize(1);
	}

	// ---- Baku ----

	@Test
	@DisplayName("Baku: divora l'effetto di entrata dell'avv. e prende +2 DEF")
	void baku_devoursEntryEffect() {
		// Arrange
		MatchState state = inCombat(baku(3, 20), creature("Wampus Cat", "Uncommon", 5, 20, WampusCatEffect.CODE));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).attack()).isEqualTo(3);
		assertThat(self(state).defense()).isEqualTo(22);
		assertThat(enemy(state).isEffectCancelled()).isTrue();
		assertThat(messages(state)).contains("Baku: devours Wampus Cat's effect, +2 DEF (DEF 22)");
	}

	@Test
	@DisplayName("Baku: divora anche l'effetto di inizio scontro di un'avversaria già in campo")
	void baku_devoursCombatStartOfEnemyInPlay() {
		// Arrange: Mothman è già entrato in uno scontro prima
		MatchState state = inCombat(baku(3, 20), creature("Mothman", "Mythic", 5, 20, MothmanEffect.CODE));
		enemy(state).recordEntered();

		// Act
		resolver.startCombat(state);

		// Assert: niente 3 danni
		assertThat(self(state).defense()).isEqualTo(22);
	}

	@Test
	@DisplayName("Baku: +2 DEF una volta per avversaria; quando Baku esce l'effetto divorato riparte")
	void baku_oncePerEnemy_untilBakuLeaves() {
		// Arrange: Baku 1/1 (+2 = 3) muore al primo round contro Smiler 3/40
		MatchState state = inCombat(baku(1, 1), creature("Smiler", "Rare", 3, 40, SmilerEffect.CODE));
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(enemy(state).attack()).isEqualTo(3);
		state.challenger().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.CHALLENGER));

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: Smiler cresce di nuovo (3 + 2) e toglie 2 ATK a C
		assertThat(enemy(state).isEffectCancelled()).isFalse();
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(messages(state)).filteredOn(message -> message.startsWith("Baku: devours")).hasSize(1);
	}

	@Test
	@DisplayName("Baku: contro una creatura senza effetto non fa niente")
	void baku_noEffect_nothing() {
		// Arrange
		MatchState state = inCombat(baku(3, 20), plain("B", 5, 40));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(messages(state)).noneMatch(message -> message.startsWith("Baku:"));
	}

	@Test
	@DisplayName("Due Baku: divora quello che risolve per primo (lo sfidante)")
	void baku_againstBaku_challengerWins() {
		// Arrange
		MatchState state = inCombat(baku(3, 20), baku(3, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(22);
		assertThat(self(state).isEffectCancelled()).isFalse();
		assertThat(enemy(state).defense()).isEqualTo(20);
		assertThat(enemy(state).isEffectCancelled()).isTrue();
	}

	@Test
	@DisplayName("Baku non divora un effetto collegato da una Spell: non è l'effetto della creatura")
	void baku_ignoresAttachedEffect() {
		// Arrange: B senza effetto, con un effetto collegato che le dà +1 DEF a inizio scontro
		MatchState state = inCombat(baku(3, 20), plain("B", 5, 40));
		enemy(state).attachEffect(new AttachedDefense());

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(41);
		assertThat(self(state).defense()).isEqualTo(20);
	}

	@Test
	@DisplayName("Baku: il Fascicolo dell'avversario salva la prima chiamata, anche senza effetto, poi Baku divora")
	void baku_sealedFileSpentOnFirstCall() {
		// Arrange: Mothman è già entrato. La sua prima chiamata è onDefenseCheck, nel controllo delle morti dopo
		// l'entrata di Baku: il Fascicolo la salva e si consuma; Baku divora il suo onCombatStart
		MatchState state = inCombat(baku(3, 20), creature("Mothman", "Mythic", 5, 20, MothmanEffect.CODE));
		enemy(state).recordEntered();
		state.opponent().placeItem(SEALED_FILE);

		// Act
		resolver.startCombat(state);

		// Assert: niente 3 danni, +2 DEF
		assertThat(state.opponent().items()).isEmpty();
		assertThat(enemy(state).isEffectCancelled()).isTrue();
		assertThat(self(state).defense()).isEqualTo(22);
		assertThat(messages(state)).noneMatch(message -> message.startsWith("Mothman:"));
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
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

	private static CardDefinition preacher(int attack, int defense) {
		return TestCards.creature(0, "The Rain Preacher", TestFactions.INVITATION, null, "Rare", attack, defense,
				RainPreacherEffect.CODE);
	}

	private static CardDefinition scp2521(int attack, int defense) {
		return TestCards.creature(0, "SCP-2521", TestFactions.ARCHIVE, null, "Rare", attack, defense,
				Scp2521Effect.CODE);
	}

	private static CardDefinition baku(int attack, int defense) {
		return TestCards.creature(0, "Baku", TestFactions.DREAM, null, "Mythic", attack, defense, BakuEffect.CODE);
	}

	private static CardDefinition hollowWyrm() {
		return TestCards.creature(0, "The Hollow Wyrm", TestFactions.WOUND, null, "Legendary", 4, 5,
				HollowWyrmEffect.CODE);
	}

	private static CardDefinition creature(String name, String rarity, int attack, int defense, String code) {
		return TestCards.creature(0, name, TestFactions.WOUND, null, rarity, attack, defense, code);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}

	/** Effetto collegato di prova (come quello di SCP-109): +1 DEF a ogni inizio di scontro. */
	private static final class AttachedDefense implements CreatureEffect {

		@Override
		public String code() {
			return "TEST_ATTACHED";
		}

		@Override
		public void onCombatStart(CreatureContext ctx) {
			ctx.self().addDefense(1);
		}
	}
}
