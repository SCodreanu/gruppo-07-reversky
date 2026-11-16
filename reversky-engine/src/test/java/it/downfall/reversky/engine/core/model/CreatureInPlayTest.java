package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Test di {@link CreatureInPlay} e {@link StatModifier}: danni permanenti (regolamento base, punto 5), modificatori
 * con origine, lato e durata, scudi di DEF temporanea e sospensioni (D-35).
 */
class CreatureInPlayTest {

	private static final CardDefinition CRAWLERS = TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5);

	@Test
	@DisplayName("Appena entrata ha ATK e DEF del catalogo (4/5) ed è viva")
	void constructor_newCreature_hasCatalogStats() {
		// Act
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);

		// Assert
		assertThat(crawlers.attack()).isEqualTo(4);
		assertThat(crawlers.defense()).isEqualTo(5);
		assertThat(crawlers.initialDefense()).isEqualTo(5);
		assertThat(crawlers.isDead()).isFalse();
	}

	@Test
	@DisplayName("Una carta che non è una creatura non entra in campo")
	void constructor_nonCreatureCard_throws() {
		assertThatThrownBy(() -> new CreatureInPlay(TestCards.terrain(90, "Echo Rift", TestFactions.WOUND,
				"ECHO_RIFT"), Side.CHALLENGER)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Il danno riduce la DEF in modo permanente: 5 − 4 = 1 vive, poi − 1 = 0 muore, l'ATK non cambia")
	void takeDamage_reducesDefensePermanently() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);

		// Act + Assert
		crawlers.takeDamage(4);
		assertThat(crawlers.defense()).isEqualTo(1);
		assertThat(crawlers.isDead()).isFalse();
		crawlers.takeDamage(1);
		assertThat(crawlers.defense()).isZero();
		assertThat(crawlers.isDead()).isTrue();
		assertThat(crawlers.attack()).isEqualTo(4);
	}

	@Test
	@DisplayName("La DEF può scendere sotto 0: 5 − 8 = −3, serve al danno al giocatore (D-03)")
	void takeDamage_moreThanDefense_goesNegative() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);

		// Act
		crawlers.takeDamage(8);

		// Assert
		assertThat(crawlers.defense()).isEqualTo(-3);
	}

	@Test
	@DisplayName("Danno negativo o DEF aggiunta negativa sono errori di programmazione")
	void negativeAmounts_throw() {
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);

		assertThatThrownBy(() -> crawlers.takeDamage(-1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> crawlers.addDefense(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("La DEF aggiunta è permanente: 5 + 3 = 8, e i danni la consumano come la DEF del catalogo")
	void addDefense_isPermanent() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);

		// Act
		crawlers.addDefense(3);
		crawlers.endCombat();
		crawlers.takeDamage(7);

		// Assert
		assertThat(crawlers.defense()).isEqualTo(1);
	}

	@Test
	@DisplayName("I modificatori di ATK si sommano, anche negativi: 4 + 2 − 3 = 3; riportano origine e lato")
	void attackModifiers_sumWithSourceAndSide() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		StatModifier terrain = StatModifier.attack(ModifierSource.TERRAIN, Side.OPPONENT, 2,
				ModifierDuration.whileInPlay());

		// Act
		crawlers.addModifier(terrain);
		crawlers.addModifier(StatModifier.attack(ModifierSource.HIDDEN, Side.OPPONENT, -3,
				ModifierDuration.rounds(1)));

		// Assert
		assertThat(crawlers.attack()).isEqualTo(3);
		assertThat(crawlers.attackModifiers()).isEqualTo(-1);
		assertThat(terrain.source()).isEqualTo(ModifierSource.TERRAIN);
		assertThat(terrain.side()).isEqualTo(Side.OPPONENT);
	}

	@Test
	@DisplayName("Un modificatore per 2 round vale in 2 round e scade alla fine del secondo")
	void endRound_roundsModifier_expiresAfterItsRounds() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -2,
				ModifierDuration.rounds(2)));

		// Act + Assert
		crawlers.endRound();
		assertThat(crawlers.attack()).isEqualTo(2);
		crawlers.endRound();
		assertThat(crawlers.attack()).isEqualTo(4);
		assertThat(crawlers.modifiers()).isEmpty();
	}

	@Test
	@DisplayName("A fine scontro scade solo il modificatore fino a fine scontro: resta quello finché in campo")
	void endCombat_removesOnlyCombatModifiers() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.attack(ModifierSource.HIDDEN, Side.CHALLENGER, 2,
				ModifierDuration.untilCombatEnd()));
		crawlers.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay()));

		// Act
		crawlers.endRound();
		crawlers.endCombat();

		// Assert
		assertThat(crawlers.attack()).isEqualTo(5);
		assertThat(crawlers.modifiers()).extracting(StatModifier::source).containsExactly(ModifierSource.SPELL);
	}

	@Test
	@DisplayName("Lo scudo assorbe per primo: DEF 5 + scudo 3, danno 4 → scudo finito, DEF permanente 4")
	void takeDamage_withShield_shieldAbsorbsFirst() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));

		// Act
		crawlers.takeDamage(4);

		// Assert
		assertThat(crawlers.defense()).isEqualTo(4);
		assertThat(crawlers.modifiers()).isEmpty();
	}

	@Test
	@DisplayName("La scadenza dello scudo non uccide: DEF 5 + scudo 3, danno 7 → DEF 1; a fine scontro resta 1")
	void endCombat_usedShield_neverKills() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));

		// Act
		crawlers.takeDamage(7);
		crawlers.endCombat();

		// Assert
		assertThat(crawlers.defense()).isEqualTo(1);
		assertThat(crawlers.isDead()).isFalse();
	}

	@Test
	@DisplayName("Alla scadenza sparisce solo la parte non usata: scudo 3, danno 2 → DEF 6, a fine scontro 5")
	void endCombat_partlyUsedShield_removesOnlyUnusedPart() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));

		// Act + Assert
		crawlers.takeDamage(2);
		assertThat(crawlers.defense()).isEqualTo(6);
		crawlers.endCombat();
		assertThat(crawlers.defense()).isEqualTo(5);
	}

	@Test
	@DisplayName("Assorbe prima lo scudo che scade prima: a round prima di finché in campo, anche se aggiunto dopo")
	void takeDamage_twoShields_soonestToExpireAbsorbsFirst() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.shield(ModifierSource.TERRAIN, Side.OPPONENT, 2,
				ModifierDuration.whileInPlay()));
		crawlers.addModifier(StatModifier.shield(ModifierSource.ITEM, Side.CHALLENGER, 2,
				ModifierDuration.rounds(1)));

		// Act
		crawlers.takeDamage(2);
		crawlers.endRound();

		// Assert: lo scudo dell'Oggetto si è consumato, quello del Terreno è intatto
		assertThat(crawlers.defense()).isEqualTo(5 + 2);
		assertThat(crawlers.modifiers()).extracting(StatModifier::source).containsExactly(ModifierSource.TERRAIN);
	}

	@Test
	@DisplayName("A parità di scadenza assorbe prima lo scudo aggiunto prima")
	void takeDamage_sameExpiry_olderShieldAbsorbsFirst() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		StatModifier first = StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd());
		StatModifier second = StatModifier.shield(ModifierSource.ITEM, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd());
		crawlers.addModifier(first);
		crawlers.addModifier(second);

		// Act
		crawlers.takeDamage(1);

		// Assert
		assertThat(first.shieldValue()).isEqualTo(2);
		assertThat(second.shieldValue()).isEqualTo(3);
	}

	@Test
	@DisplayName("Quando esce il Terreno si tolgono solo i suoi modificatori, ATK e scudo (D-19)")
	void removeModifiers_terrainLeaves_removesOnlyTerrainModifiers() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addModifier(StatModifier.attack(ModifierSource.TERRAIN, Side.OPPONENT, 1,
				ModifierDuration.whileInPlay()));
		crawlers.addModifier(StatModifier.shield(ModifierSource.TERRAIN, Side.OPPONENT, 2,
				ModifierDuration.whileInPlay()));
		crawlers.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 2,
				ModifierDuration.whileInPlay()));

		// Act
		crawlers.removeModifiers(ModifierSource.TERRAIN);

		// Assert
		assertThat(crawlers.attack()).isEqualTo(6);
		assertThat(crawlers.defense()).isEqualTo(5);
	}

	@Test
	@DisplayName("Un modificatore sospeso non conta, né l'ATK né lo scudo; riprende con resume")
	void suspend_modifierDoesNotCountUntilResumed() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		StatModifier bonus = StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 3,
				ModifierDuration.whileInPlay());
		StatModifier shield = StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 2,
				ModifierDuration.whileInPlay());
		crawlers.addModifier(bonus);
		crawlers.addModifier(shield);

		// Act
		bonus.suspend(ModifierDuration.whileInPlay());
		shield.suspend(ModifierDuration.whileInPlay());
		crawlers.takeDamage(1);

		// Assert: lo scudo sospeso non assorbe
		assertThat(crawlers.attack()).isEqualTo(4);
		assertThat(crawlers.defense()).isEqualTo(4);
		bonus.resume();
		shield.resume();
		assertThat(crawlers.attack()).isEqualTo(7);
		assertThat(crawlers.defense()).isEqualTo(6);
	}

	@Test
	@DisplayName("Una sospensione di 1 round finisce a fine round; una fino a fine scontro finisce a fine scontro")
	void suspend_endsWithItsDuration() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		StatModifier oneRound = StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay());
		StatModifier combat = StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, 2,
				ModifierDuration.whileInPlay());
		crawlers.addModifier(oneRound);
		crawlers.addModifier(combat);
		oneRound.suspend(ModifierDuration.rounds(1));
		combat.suspend(ModifierDuration.untilCombatEnd());

		// Act + Assert
		crawlers.endRound();
		assertThat(oneRound.isSuspended()).isFalse();
		assertThat(combat.isSuspended()).isTrue();
		crawlers.endCombat();
		assertThat(combat.isSuspended()).isFalse();
		assertThat(crawlers.attack()).isEqualTo(7);
	}

	@Test
	@DisplayName("La DEF all'entrata comprende i bonus d'entrata e si fissa una volta sola (M-06)")
	void recordInitialDefense_fixesDefenseWithBonusesOnce() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addDefense(3);

		// Act
		crawlers.recordInitialDefense();
		crawlers.takeDamage(4);
		crawlers.addDefense(2);
		crawlers.recordInitialDefense();

		// Assert
		assertThat(crawlers.initialDefense()).isEqualTo(8);
	}

	@Test
	@DisplayName("Uno scudo di 0 DEF e una durata di 0 round non si possono creare")
	void invalidModifiers_throw() {
		assertThatThrownBy(() -> StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 0,
				ModifierDuration.untilCombatEnd())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ModifierDuration.rounds(0)).isInstanceOf(IllegalArgumentException.class);
	}
}
