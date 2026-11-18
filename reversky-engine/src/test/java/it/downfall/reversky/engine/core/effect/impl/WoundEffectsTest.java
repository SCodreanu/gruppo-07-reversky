package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
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
 * Effetti {@code P1} di Theory of the Wound (ENG-26), più i gruppi speculari che la fazione condivide con Theory of
 * the Dream (classi base, D-37). Si provano attraverso il {@link RoundResolver}.
 */
class WoundEffectsTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- gruppi speculari ----

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "HOWLERS", "NOPPERA_BO", "MASKED_BLOOM" })
	@DisplayName("Gruppo di Howlers: avv. −2 ATK nel primo round e −1 nel secondo (B 5 → 3, 4, poi 5)")
	void fadingAttackPenalty(String code) {
		// Arrange
		MatchState state = inCombat(creature("Howler", "Uncommon", 1, 30, code), plain("B", 5, 30));
		CreatureInPlay b = enemy(state);

		// Act + Assert
		resolver.startCombat(state);
		assertThat(b.attack()).isEqualTo(3);
		resolver.resolve(state);
		assertThat(b.attack()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(b.attack()).isEqualTo(5);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "FADED_TAG", "ROKUROKUBI" })
	@DisplayName("Gruppo di Cervo Bianco: avv. −1 DEF (10 → 9) e −1 DEF in attesa per la prossima avversaria")
	void entryAndNextDamage(String code) {
		// Arrange
		MatchState state = inCombat(creature("Tagger", "Uncommon", 1, 30, code), plain("B", 3, 10));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(9);
		assertThat(state.opponent().pendingModifiers()).containsExactly(
				new PendingModifier("Tagger", ModifierSource.EFFECT, Side.CHALLENGER, 0, 0, 1));
	}

	@Test
	@DisplayName("The Gnawing Witness: all'entrata 2 danni all'avversaria (10 → 8)")
	void gnawingWitness_entryDamage() {
		// Arrange
		MatchState state = inCombat(creature("The Gnawing Witness", "Uncommon", 2, 10, "GNAWING_WITNESS"),
				plain("B", 3, 10));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(8);
		assertThat(messages(state)).contains("The Gnawing Witness: deals 2 damage to B (DEF 8)");
	}

	// ---- Wretches ----

	@Test
	@DisplayName("Wretches: la prima ferita dà +2 ATK finché resta in campo (3 → 5), la seconda niente")
	void wretches_firstWoundOnly() {
		// Arrange
		MatchState state = inCombat(creature("Wretches", "Uncommon", 3, 30, "WRETCHES"), plain("B", 4, 40));
		CreatureInPlay wretches = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert: il round 1 attacca con 3, poi è ferita
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(37);
		assertThat(wretches.attack()).isEqualTo(5);
		assertThat(messages(state)).contains("Wretches: +2 ATK while in play (ATK 5)");

		// Act + Assert: il round 2 attacca con 5 e la seconda ferita non dà altro
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(32);
		assertThat(wretches.attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("Wretches: ferita all'entrata da un effetto avversario, il bonus vale già nel primo round")
	void wretches_woundedByEntryEffect() {
		// Arrange: The Gnawing Witness le toglie 2 DEF all'entrata
		MatchState state = inCombat(creature("Wretches", "Uncommon", 3, 30, "WRETCHES"),
				creature("The Gnawing Witness", "Uncommon", 2, 40, "GNAWING_WITNESS"));
		CreatureInPlay wretches = state.challenger().creature().orElseThrow();

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: 40 − 5 = 35
		assertThat(enemy(state).defense()).isEqualTo(35);
		assertThat(wretches.attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("Wretches: un colpo tutto assorbito da uno scudo non è una ferita")
	void wretches_shieldAbsorbs_noBonus() {
		// Arrange: scudo di 10
		MatchState state = inCombat(creature("Wretches", "Uncommon", 3, 30, "WRETCHES"), plain("B", 4, 40));
		CreatureInPlay wretches = state.challenger().creature().orElseThrow();
		wretches.addModifier(StatModifier.shield(ModifierSource.SPELL, Side.CHALLENGER, 10,
				ModifierDuration.whileInPlay()));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(wretches.attack()).isEqualTo(3);
	}

	// ---- Rare ----

	@ParameterizedTest(name = "DEF base avversaria {0}: Skin-Stealers 10 → {1}")
	@CsvSource({ "7, 13", "9, 14", "12, 14" })
	@DisplayName("Skin-Stealers: metà della DEF base avversaria per difetto, massimo 4, compresa nella DEF all'entrata")
	void skinStealers_halfEnemyBaseDefense(int enemyDefense, int expected) {
		// Arrange
		MatchState state = inCombat(creature("Skin-Stealers", "Rare", 3, 10, "SKIN_STEALERS"),
				plain("B", 3, enemyDefense));

		// Act
		resolver.startCombat(state);

		// Assert
		CreatureInPlay skin = state.challenger().creature().orElseThrow();
		assertThat(skin.defense()).isEqualTo(expected);
		assertThat(skin.initialDefense()).isEqualTo(expected);
	}

	@Test
	@DisplayName("Skin-Stealers: conta la DEF base della carta, non quella attuale di una sopravvissuta ferita")
	void skinStealers_usesBaseDefense() {
		// Arrange: B 3/8 è già scesa a 2
		MatchState state = inCombat(creature("Skin-Stealers", "Rare", 3, 10, "SKIN_STEALERS"), plain("B", 3, 8));
		enemy(state).takeDamage(6);

		// Act
		resolver.startCombat(state);

		// Assert: 8 / 2 = 4
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(14);
	}

	@Test
	@DisplayName("Il Doppio di Servizio: +3 ATK nei primi due round dopo l'entrata (7, 7, poi 4)")
	void serviceDouble_firstTwoRounds() {
		// Arrange
		MatchState state = inCombat(creature("The Service Double", "Rare", 4, 30, "SERVICE_DOUBLE"),
				plain("B", 1, 40));
		CreatureInPlay serviceDouble = state.challenger().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(serviceDouble.attack()).isEqualTo(7);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(33);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(26);
		assertThat(serviceDouble.attack()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(22);
	}

	@ParameterizedTest(name = "DEF base {0}: 40% = {1} danni")
	@CsvSource({ "7, 2", "10, 4", "13, 4" })
	@DisplayName("Il Direttore dell'Edificio: danni pari al 40% della DEF base avversaria, per difetto, massimo 4")
	void buildingDirector_fortyPercentOfBaseDefense(int enemyDefense, int damage) {
		// Arrange
		MatchState state = inCombat(creature("The Building Director", "Rare", 2, 30, "BUILDING_DIRECTOR"),
				plain("B", 3, enemyDefense));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(enemyDefense - damage);
	}

	@Test
	@DisplayName("M-06 (deciso il 18 novembre): il danno del Direttore non cambia la DEF all'entrata di B (10)")
	void buildingDirector_entryDefenseUnchanged() {
		// Arrange
		MatchState state = inCombat(creature("The Building Director", "Rare", 2, 30, "BUILDING_DIRECTOR"),
				plain("B", 3, 10));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(6);
		assertThat(enemy(state).initialDefense()).isEqualTo(10);
	}

	// ---- Hounds ----

	@Test
	@DisplayName("Hounds: +4 ATK contro un'avversaria già ferita (round 1 con 5: 30 → 25; round 2 con 9: → 16)")
	void hounds_bonusAgainstWoundedEnemy() {
		// Arrange
		MatchState state = inCombat(creature("Hounds", "Mythic", 5, 30, "HOUNDS"), plain("B", 2, 30));
		resolver.startCombat(state);

		// Act + Assert
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(25);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(16);
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.ATTACK_COMPUTED);
			assertThat(event.data()).containsEntry("attacker", "Hounds").containsEntry("effectBonus", 4)
					.containsEntry("total", 9);
		});
	}

	@Test
	@DisplayName("Hounds (18 novembre): conta la DEF sotto quella all'entrata, anche se ha assorbito uno scudo")
	void hounds_shieldAbsorbedCountsAsWounded() {
		// Arrange: B ha uno scudo di 5, DEF all'entrata 35
		MatchState state = inCombat(creature("Hounds", "Mythic", 5, 30, "HOUNDS"), plain("B", 2, 30));
		enemy(state).addModifier(StatModifier.shield(ModifierSource.SPELL, Side.OPPONENT, 5,
				ModifierDuration.whileInPlay()));
		resolver.startCombat(state);

		// Act + Assert: il round 1 consuma lo scudo (35 → 30), il round 2 attacca con 9
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(30);
		resolver.resolve(state);
		assertThat(enemy(state).defense()).isEqualTo(21);
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
		return TestCards.creature(0, name, TestFactions.WOUND, null, rarity, attack, defense, code);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.INVITATION, attack, defense);
	}
}
