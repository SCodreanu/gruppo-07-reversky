package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
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
 * Effetti {@code P1} di Theory of the Dream (ENG-26). I gruppi speculari (Noppera-bō, Rokurokubi, The Masked Bloom)
 * sono in {@link WoundEffectsTest}. Si provano attraverso il {@link RoundResolver}.
 */
class DreamEffectsTest {

	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
			"SEALED_FILE_ZERO");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- Uncommon ----

	@Test
	@DisplayName("The Threadbound Doll: alla morte 4 danni all'avversaria (B 10 − 3 = 7, poi 3)")
	void threadboundDoll_deathDamage() {
		// Arrange: la Doll 3/2 muore al round 1
		MatchState state = inCombat(creature("The Threadbound Doll", "Uncommon", 3, 2, "THREADBOUND_DOLL"),
				plain("B", 5, 10));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert: la Doll muore a −3
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(enemy(state).defense()).isEqualTo(3);
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 3);
	}

	@Test
	@DisplayName("The Threadbound Doll: se il danno alla morte uccide, muoiono entrambe e nessuno perde punti vita")
	void threadboundDoll_bothDie() {
		// Arrange: B 5/7 scende a 4, poi a 0
		MatchState state = inCombat(creature("The Threadbound Doll", "Uncommon", 3, 2, "THREADBOUND_DOLL"),
				plain("B", 5, 7));
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
	@DisplayName("L'Ombra del Torii senza Terreno: +2 DEF finché resta in campo (5 → 7)")
	void toriiShadow_noTerrain() {
		// Arrange
		MatchState state = inCombat(creature("Torii Shadow", "Uncommon", 2, 5, "TORII_SHADOW"), plain("B", 3, 10));

		// Act
		resolver.startCombat(state);

		// Assert
		CreatureInPlay torii = state.challenger().creature().orElseThrow();
		assertThat(torii.defense()).isEqualTo(7);
		assertThat(torii.initialDefense()).isEqualTo(7);
	}

	@Test
	@DisplayName("L'Ombra del Torii con il Terreno della sua fazione, anche se l'ha pescato l'avversario: +3 DEF")
	void toriiShadow_ownFactionTerrain() {
		// Arrange: Devouring Abyss (Theory of the Dream) dell'avversario, bonus base +1 DEF (ENG-42)
		MatchState state = inCombat(creature("Torii Shadow", "Uncommon", 2, 5, "TORII_SHADOW"), plain("B", 3, 10));
		TestStates.placeTerrain(state, TestCards.terrain(116, "Devouring Abyss", TestFactions.DREAM, "DEVOURING_ABYSS"),
				Side.OPPONENT);

		// Act
		resolver.startCombat(state);

		// Assert: 5 + 1 del Terreno + 3
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(9);
	}

	@Test
	@DisplayName("L'Ombra del Torii con il Terreno di un'altra fazione: +2 DEF")
	void toriiShadow_otherFactionTerrain() {
		// Arrange: Echo Rift (Theory of the Wound)
		MatchState state = inCombat(creature("Torii Shadow", "Uncommon", 2, 5, "TORII_SHADOW"), plain("B", 3, 10));
		TestStates.placeTerrain(state,
				TestCards.terrain(113, "Echo Rift", TestFactions.WOUND, "ECHO_RIFT"), Side.CHALLENGER);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(7);
	}

	// ---- Rare ----

	@Test
	@DisplayName("Yuki-onna: nel primo round l'attacco avversario è 0 (frozen), poi torna 6 (30, poi 24)")
	void yukiOnna_freezesFirstRound() {
		// Arrange
		MatchState state = inCombat(creature("Yuki-onna", "Rare", 3, 30, "YUKI_ONNA"), plain("B", 6, 30));
		CreatureInPlay yuki = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(yuki.defense()).isEqualTo(30);
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.ATTACK_COMPUTED);
			assertThat(event.message()).isEqualTo("B attacks with 0 (6 base, frozen)");
			assertThat(event.data()).containsEntry("frozen", true).containsEntry("total", 0);
		});
		resolver.resolve(state);
		assertThat(yuki.defense()).isEqualTo(24);
	}

	@Test
	@DisplayName("Yuki-onna: il Fascicolo Sigillato 0 dell'avversario annulla il congelamento (30 → 24)")
	void yukiOnna_sealedFileCancels() {
		// Arrange
		MatchState state = inCombat(creature("Yuki-onna", "Rare", 3, 30, "YUKI_ONNA"), plain("B", 6, 30));
		state.opponent().placeItem(SEALED_FILE);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(24);
		assertThat(state.opponent().items()).isEmpty();
	}

	@Test
	@DisplayName("Jorōgumo: previene 3 danni dai primi due attacchi con danno (D-33): 30, 28, 26, poi 21")
	void jorogumo_firstTwoAttacks() {
		// Arrange: al round 1 B attacca con 0, che non conta
		MatchState state = inCombat(creature("Jorōgumo", "Rare", 1, 30, "JOROGUMO"), plain("B", 5, 40));
		enemy(state).addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, -5,
				ModifierDuration.rounds(1)));
		CreatureInPlay jorogumo = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(jorogumo.defense()).isEqualTo(30);
		resolver.resolve(state);
		assertThat(jorogumo.defense()).isEqualTo(28);
		resolver.resolve(state);
		assertThat(jorogumo.defense()).isEqualTo(26);
		resolver.resolve(state);
		assertThat(jorogumo.defense()).isEqualTo(21);
	}

	@Test
	@DisplayName("Nure-onna: quando B scende a metà della DEF all'entrata (8 ≤ 10) 3 danni, una volta (16, 12, 5, 1)")
	void nureOnna_halfDefenseOncePerEnemy() {
		// Arrange
		MatchState state = inCombat(creature("Nure-onna", "Rare", 4, 30, "NURE_ONNA"), plain("B", 1, 20));
		CreatureInPlay b = enemy(state);
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(12);
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(5);
		assertThat(messages(state)).contains("Nure-onna: deals 3 damage to B (DEF 5)");
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(1);
	}

	@Test
	@DisplayName("Nure-onna (deciso il 18 novembre): una sopravvissuta già a metà o meno è colpita al primo controllo")
	void nureOnna_survivorAlreadyAtHalf() {
		// Arrange: B entrata con 20 è scesa a 8
		MatchState state = inCombat(creature("Nure-onna", "Rare", 4, 30, "NURE_ONNA"), plain("B", 1, 20));
		CreatureInPlay b = enemy(state);
		b.recordInitialDefense();
		b.takeDamage(12);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(b.defense()).isEqualTo(5);
	}

	@Test
	@DisplayName("Nure-onna: annullata dal Fascicolo Sigillato 0, quella avversaria è comunque consumata (8, poi 4)")
	void nureOnna_sealedFileUsesUpThatEnemy() {
		// Arrange
		MatchState state = inCombat(creature("Nure-onna", "Rare", 4, 30, "NURE_ONNA"), plain("B", 1, 20));
		state.opponent().placeItem(SEALED_FILE);
		CreatureInPlay b = enemy(state);
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		resolver.resolve(state);
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(8);
		assertThat(state.opponent().items()).isEmpty();
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(4);
	}

	@Test
	@DisplayName("The Face Collector: ruba 2 ATK nel primo round e 1 nel secondo (5/3, 4/4, poi 3/5)")
	void faceCollector_stealsAttack() {
		// Arrange
		MatchState state = inCombat(creature("The Face Collector", "Rare", 3, 30, "FACE_COLLECTOR"),
				plain("B", 5, 30));
		CreatureInPlay collector = state.challenger().creature().orElseThrow();
		CreatureInPlay b = enemy(state);

		// Act + Assert
		resolver.startCombat(state);
		assertThat(collector.attack()).isEqualTo(5);
		assertThat(b.attack()).isEqualTo(3);
		assertThat(messages(state)).contains(
				"The Face Collector: steals 2 ATK in the first round and 1 in the second from B (ATK 5, B ATK 3)");
		resolver.resolve(state);
		assertThat(collector.attack()).isEqualTo(4);
		assertThat(b.attack()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(collector.attack()).isEqualTo(3);
		assertThat(b.attack()).isEqualTo(5);
		assertThat(b.defense()).isEqualTo(21);
	}

	@Test
	@DisplayName("The Face Collector: con 1 ATK l'avversaria perde solo 1 per round e lui guadagna 1 (4/0, 4/0, 3/1)")
	void faceCollector_stealsOnlyWhatThereIs() {
		// Arrange
		MatchState state = inCombat(creature("The Face Collector", "Rare", 3, 30, "FACE_COLLECTOR"),
				plain("B", 1, 30));
		CreatureInPlay collector = state.challenger().creature().orElseThrow();
		CreatureInPlay b = enemy(state);

		// Act + Assert
		resolver.startCombat(state);
		assertThat(collector.attack()).isEqualTo(4);
		assertThat(b.attack()).isEqualTo(0);
		resolver.resolve(state);
		assertThat(collector.attack()).isEqualTo(4);
		assertThat(b.attack()).isEqualTo(0);
		resolver.resolve(state);
		assertThat(collector.attack()).isEqualTo(3);
		assertThat(b.attack()).isEqualTo(1);
	}

	@Test
	@DisplayName("The Face Collector (18 novembre): annullato dal Fascicolo Sigillato 0 non ruba e non guadagna")
	void faceCollector_sealedFileCancelsAll() {
		// Arrange
		MatchState state = inCombat(creature("The Face Collector", "Rare", 3, 30, "FACE_COLLECTOR"),
				plain("B", 5, 30));
		state.opponent().placeItem(SEALED_FILE);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(3);
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(state.opponent().items()).isEmpty();
	}

	// ---- Mythic e Legendary ----

	@Test
	@DisplayName("The Unseen Matriarch: a metà DEF (4 ≤ 5) B subisce la DEF persa (6) e −2 ATK (ENG-42), una volta")
	void unseenMatriarch_veilBreaks() {
		// Arrange
		MatchState state = inCombat(creature("The Unseen Matriarch", "Mythic", 2, 10, "UNSEEN_MATRIARCH"),
				plain("B", 3, 40));
		CreatureInPlay matriarch = state.challenger().creature().orElseThrow();
		CreatureInPlay b = enemy(state);
		resolver.startCombat(state);

		// Act + Assert: round 1 a 7, round 2 a 4
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(38);
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(30);
		assertThat(b.attack()).isEqualTo(1);
		assertThat(messages(state)).contains("The Unseen Matriarch: deals 6 damage to B (DEF 30), B −2 ATK while in "
				+ "play (ATK 1)");

		// Act + Assert: round 3, nessun secondo colpo; B attacca con 1
		resolver.resolve(state);
		assertThat(matriarch.defense()).isEqualTo(3);
		assertThat(b.defense()).isEqualTo(28);
	}

	@Test
	@DisplayName("The Unseen Matriarch: la DEF persa vale al massimo 8 (20 → 8, persa 12)")
	void unseenMatriarch_damageCappedAtEight() {
		// Arrange
		MatchState state = inCombat(creature("The Unseen Matriarch", "Mythic", 2, 20, "UNSEEN_MATRIARCH"),
				plain("B", 6, 40));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);
		resolver.resolve(state);

		// Assert: 40 − 2 − 2 − 8
		assertThat(enemy(state).defense()).isEqualTo(28);
	}

	@Test
	@DisplayName("The Unseen Matriarch: la DEF persa conta solo le ferite vere, non lo scudo (2, non 8)")
	void unseenMatriarch_shieldIsNotLost() {
		// Arrange: 10 + scudo 6 = 16 all'entrata; il round 1 consuma 4 di scudo, il round 2 2 di scudo e 2 di DEF
		MatchState state = inCombat(creature("The Unseen Matriarch", "Mythic", 2, 10, "UNSEEN_MATRIARCH"),
				plain("B", 4, 40));
		state.challenger().creature().orElseThrow().addModifier(StatModifier.shield(ModifierSource.SPELL,
				Side.CHALLENGER, 6, ModifierDuration.whileInPlay()));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);
		resolver.resolve(state);

		// Assert: DEF 8 ≤ 16 / 2, persa 2; 40 − 2 − 2 − 2
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(8);
		assertThat(enemy(state).defense()).isEqualTo(34);
	}

	@Test
	@DisplayName("The Unseen Matriarch (18 novembre): annullata dal Fascicolo Sigillato 0 si consuma lo stesso")
	void unseenMatriarch_sealedFileUsesItUp() {
		// Arrange
		MatchState state = inCombat(creature("The Unseen Matriarch", "Mythic", 2, 10, "UNSEEN_MATRIARCH"),
				plain("B", 3, 40));
		state.opponent().placeItem(SEALED_FILE);
		CreatureInPlay b = enemy(state);
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(36);
		assertThat(b.attack()).isEqualTo(3);
		assertThat(state.opponent().items()).isEmpty();
		resolver.resolve(state);
		assertThat(b.defense()).isEqualTo(34);
	}

	@Test
	@DisplayName("Shuten-dōji: +6 ATK dopo ogni danno DEF subito, massimo +18 (8, 14, 20, 20; ENG-42, era +4, max +12)")
	void shutenDoji_stacksUpToEighteen() {
		// Arrange
		MatchState state = inCombat(creature("Shuten-dōji", "Legendary", 2, 40, "SHUTEN_DOJI"), plain("B", 3, 60));
		CreatureInPlay shuten = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(shuten.attack()).isEqualTo(8);
		resolver.resolve(state);
		assertThat(shuten.attack()).isEqualTo(14);
		resolver.resolve(state);
		assertThat(shuten.attack()).isEqualTo(20);
		resolver.resolve(state);
		assertThat(shuten.attack()).isEqualTo(20);
		assertThat(enemy(state).defense()).isEqualTo(60 - 2 - 8 - 14 - 20);
	}

	@Test
	@DisplayName("Shuten-dōji: conta anche il danno di un effetto all'entrata, già nel primo round (8)")
	void shutenDoji_effectDamageCounts() {
		// Arrange: The Gnawing Witness le toglie 2 DEF all'entrata
		MatchState state = inCombat(creature("Shuten-dōji", "Legendary", 2, 40, "SHUTEN_DOJI"),
				TestCards.creature(0, "The Gnawing Witness", TestFactions.WOUND, null, "Uncommon", 2, 60,
						"GNAWING_WITNESS"));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(8);
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
	}

	private static CreatureInPlay enemy(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}

	private static List<String> messages(MatchState state) {
		return state.log().events().stream().map(MatchEvent::message).toList();
	}

	private static CardDefinition creature(String name, String rarity, int attack, int defense, String code) {
		return TestCards.creature(0, name, TestFactions.DREAM, null, rarity, attack, defense, code);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.INVITATION, attack, defense);
	}
}
