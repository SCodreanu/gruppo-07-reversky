package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Test di {@link CreatureInPlay} e {@link StatModifier}: danni permanenti (regolamento base, punto 5), modificatori
 * con origine, lato e durata, scudi di DEF temporanea e sospensioni (D-35).
 */
class CreatureInPlayTest {

	private static final CardDefinition CRAWLERS = TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5);
	private static final CardDefinition BRUTE = TestCards.creature("Brute", TestFactions.WOUND, 3, 10);

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
	@DisplayName("M-06 (deciso il 18 novembre): la DEF all'entrata conta i bonus, non i danni presi prima di fissarla")
	void initialDefense_ignoresDamageBeforeRecording() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		crawlers.addDefense(3);
		crawlers.takeDamage(2);

		// Act + Assert: 5 + 3 = 8 anche con DEF 6, prima e dopo averla fissata
		assertThat(crawlers.initialDefense()).isEqualTo(8);
		crawlers.recordInitialDefense();
		crawlers.takeDamage(1);
		assertThat(crawlers.defense()).isEqualTo(5);
		assertThat(crawlers.initialDefense()).isEqualTo(8);
	}

	@Test
	@DisplayName("Ogni ferita aspetta di essere riferita, con la causa; lo scudo non ferisce; il totale le somma")
	void takeDamage_queuesWoundsWithCause() {
		// Arrange: scudo di 3
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		CreatureInPlay enemy = new CreatureInPlay(CRAWLERS, Side.OPPONENT);
		crawlers.addModifier(StatModifier.shield(ModifierSource.SPELL, Side.CHALLENGER, 3,
				ModifierDuration.whileInPlay()));

		// Act: 2 tutti allo scudo; 3 da enemy (1 allo scudo, 2 di ferita); 1 da una carta che non è una creatura
		crawlers.takeDamage(2, enemy);
		crawlers.takeDamage(3, enemy);
		crawlers.takeDamage(1);

		// Assert
		assertThat(crawlers.hasUnreportedWounds()).isTrue();
		List<Wound> wounds = crawlers.takeUnreportedWounds();
		assertThat(wounds).extracting(Wound::amount).containsExactly(2, 1);
		assertThat(wounds.get(0).cause()).contains(enemy);
		assertThat(wounds.get(1).cause()).isEmpty();
		assertThat(crawlers.hasUnreportedWounds()).isFalse();
		assertThat(crawlers.woundsTaken()).isEqualTo(3);
	}

	@Test
	@DisplayName("L'attacco congelato vale finché il modificatore è attivo: sospeso non conta, poi scade a fine round")
	void freezeAttack_activeSuspendedExpired() {
		// Arrange
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);
		StatModifier freeze = StatModifier.freezeAttack(ModifierSource.EFFECT, Side.OPPONENT,
				ModifierDuration.rounds(1));

		// Act + Assert
		crawlers.addModifier(freeze);
		assertThat(crawlers.isAttackFrozen()).isTrue();
		assertThat(crawlers.attack()).isEqualTo(4);
		freeze.suspend(ModifierDuration.rounds(1));
		assertThat(crawlers.isAttackFrozen()).isFalse();
		freeze.resume();
		crawlers.endRound();
		assertThat(crawlers.isAttackFrozen()).isFalse();
		assertThat(crawlers.modifiers()).isEmpty();
	}

	@Test
	@DisplayName("Uno scudo di 0 DEF e una durata di 0 round non si possono creare")
	void invalidModifiers_throw() {
		assertThatThrownBy(() -> StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 0,
				ModifierDuration.untilCombatEnd())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ModifierDuration.rounds(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("ENG-34: heal rende DEF fino alla DEF all'entrata (10 − 6 + 4 = 8, poi solo +2), le ferite restano")
	void heal_upToInitialDefense() {
		// Arrange
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.recordInitialDefense();
		brute.takeDamage(6);

		// Act + Assert
		assertThat(brute.heal(4)).isEqualTo(4);
		assertThat(brute.defense()).isEqualTo(8);
		assertThat(brute.heal(5)).isEqualTo(2);
		assertThat(brute.defense()).isEqualTo(10);
		assertThat(brute.heal(3)).isZero();
		assertThat(brute.woundsTaken()).isEqualTo(6);
	}

	@Test
	@DisplayName("ENG-34: lo scudo dell'entrata scaduto si recupera (10 + 3 = 13 all'entrata, scudo finito, +3)")
	void heal_expiredEntryShield_restored() {
		// Arrange
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));
		brute.recordInitialDefense();
		brute.endCombat();

		// Act
		int healed = brute.heal(5);

		// Assert
		assertThat(healed).isEqualTo(3);
		assertThat(brute.defense()).isEqualTo(13);
	}

	@Test
	@DisplayName("ENG-34: con DEF 0 o meno è morta e non recupera niente (D-16); un recupero negativo non esiste")
	void heal_deadOrNegative() {
		// Arrange
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.recordInitialDefense();
		brute.takeDamage(12);

		// Act + Assert
		assertThat(brute.heal(5)).isZero();
		assertThat(brute.defense()).isEqualTo(-2);
		assertThatThrownBy(() -> brute.heal(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("ENG-34: gli effetti collegati vengono dopo il proprio, nell'ordine in cui arrivano")
	void attachEffect_afterOwnEffect() {
		// Arrange
		CreatureEffect own = () -> "OWN";
		CreatureEffect first = () -> "FIRST";
		CreatureEffect second = () -> "SECOND";
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER, own);
		CreatureInPlay plain = new CreatureInPlay(BRUTE, Side.OPPONENT);

		// Act
		brute.attachEffect(first);
		brute.attachEffect(second);
		plain.attachEffect(first);

		// Assert
		assertThat(brute.effects()).containsExactly(own, first, second);
		assertThat(brute.effect()).contains(own);
		assertThat(plain.effects()).containsExactly(first);
		assertThat(plain.effect()).isEmpty();
	}

	@Test
	@DisplayName("ENG-35: il sigillo toglie punti solo ai bonus ATK, anche a quelli dopo; i malus contano sempre")
	void sealAttack_onlyPositiveBonuses() {
		// Arrange: Brute 3 con +1 e +2 ATK, sigillo di 2
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.addModifier(StatModifier.attack(ModifierSource.TERRAIN, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay()));
		brute.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 2,
				ModifierDuration.whileInPlay()));

		// Act + Assert: 3 + (3 − 2) = 4
		brute.addModifier(StatModifier.sealAttack(ModifierSource.EFFECT, Side.OPPONENT, 2,
				ModifierDuration.whileInPlay()));
		assertThat(brute.attack()).isEqualTo(4);
		assertThat(brute.effectiveAttackBonus()).isEqualTo(1);

		// Act + Assert: un malus di 2 conta per intero, 3 + 1 − 2 = 2
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -2,
				ModifierDuration.whileInPlay()));
		assertThat(brute.attack()).isEqualTo(2);

		// Act + Assert: un sigillo più grande dei bonus non tocca il malus, 3 + 0 − 2 = 1
		brute.addModifier(StatModifier.sealAttack(ModifierSource.SPELL, Side.OPPONENT, 5,
				ModifierDuration.untilCombatEnd()));
		assertThat(brute.attack()).isEqualTo(1);
		assertThat(brute.effectiveAttackBonus()).isZero();

		// Act + Assert: a fine scontro scade solo il secondo sigillo
		brute.endCombat();
		assertThat(brute.attack()).isEqualTo(2);
	}

	@Test
	@DisplayName("ENG-35: un sigillo di 0 punti non si può creare")
	void sealAttack_zero_throws() {
		assertThatThrownBy(() -> StatModifier.sealAttack(ModifierSource.EFFECT, Side.OPPONENT, 0,
				ModifierDuration.whileInPlay())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("ENG-35: removeBonuses toglie bonus ATK e scudi; tiene malus, sigilli, congelamento, DEF permanente")
	void removeBonuses_keepsPenaltiesAndPermanentDefense() {
		// Arrange: Brute 3/10 con +2 DEF permanente, +2 ATK, scudo di 3, −1 ATK, sigillo e congelamento
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.addDefense(2);
		brute.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 2,
				ModifierDuration.whileInPlay()));
		brute.addModifier(StatModifier.shield(ModifierSource.TERRAIN, Side.CHALLENGER, 3,
				ModifierDuration.whileInPlay()));
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -1,
				ModifierDuration.whileInPlay()));
		brute.addModifier(StatModifier.sealAttack(ModifierSource.EFFECT, Side.OPPONENT, 1,
				ModifierDuration.whileInPlay()));
		brute.addModifier(StatModifier.freezeAttack(ModifierSource.EFFECT, Side.OPPONENT,
				ModifierDuration.rounds(1)));

		// Act
		boolean removed = brute.removeBonuses();

		// Assert
		assertThat(removed).isTrue();
		assertThat(brute.defense()).isEqualTo(12);
		assertThat(brute.attack()).isEqualTo(2);
		assertThat(brute.modifiers()).extracting(StatModifier::stat)
				.containsExactly(Stat.ATTACK, Stat.ATTACK_SEAL, Stat.ATTACK_FREEZE);
		assertThat(brute.removeBonuses()).isFalse();
	}

	@Test
	@DisplayName("ENG-36: removeTemporaryModifiers toglie i modificatori a durata di ogni segno, non quelli in campo")
	void removeTemporaryModifiers_onlyWithDuration() {
		// Arrange: +2 ATK per un round, −1 ATK e scudo di 3 fino a fine scontro, +1 ATK finché resta in campo
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, 2, ModifierDuration.rounds(1)));
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -1,
				ModifierDuration.untilCombatEnd()));
		brute.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));
		brute.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay()));

		// Act + Assert
		assertThat(brute.removeTemporaryModifiers()).isTrue();
		assertThat(brute.attack()).isEqualTo(4);
		assertThat(brute.defense()).isEqualTo(10);
		assertThat(brute.removeTemporaryModifiers()).isFalse();
	}

	@Test
	@DisplayName("ENG-37: un malus ATK dell'avversario è una penalità in coda, uno del proprio lato no")
	void addModifier_enemyAttackMalus_recordedAsPenalty() {
		// Arrange
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		StatModifier enemyMalus = StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -2,
				ModifierDuration.whileInPlay());

		// Act
		brute.addModifier(enemyMalus);
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, -1,
				ModifierDuration.whileInPlay()));
		brute.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.OPPONENT, 2, ModifierDuration.whileInPlay()));
		brute.recordDefensePenalty(3);
		brute.recordDefensePenalty(0);

		// Assert
		assertThat(brute.takeUnreportedPenalties()).containsExactly(new Penalty(0, enemyMalus), new Penalty(3, null));
		assertThat(brute.hasUnreportedPenalties()).isFalse();
		assertThat(brute.enemyAttackPenalties()).containsExactly(enemyMalus);
	}

	@Test
	@DisplayName("ENG-37: cancelAttackPenalty toglie punti a un malus e tiene la durata; restoreDefense vale da morta")
	void cancelAttackPenaltyAndRestoreDefense() {
		// Arrange: −3 ATK per due round
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		StatModifier malus = StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -3, ModifierDuration.rounds(2));
		brute.addModifier(malus);

		// Act + Assert: 2 punti, resta −1 per due round
		assertThat(brute.cancelAttackPenalty(malus, 2)).isEqualTo(2);
		assertThat(brute.attack()).isEqualTo(2);
		assertThat(brute.modifiers()).singleElement()
				.satisfies(left -> assertThat(left.remainingRounds()).isEqualTo(2));
		assertThat(brute.cancelAttackPenalty(malus, 2)).isZero();

		// Act + Assert: da DEF −2 torna a 3
		brute.takeDamage(12);
		brute.restoreDefense(5);
		assertThat(brute.defense()).isEqualTo(3);
	}

	@Test
	@DisplayName("ENG-37: removeTemporaryBonuses toglie solo bonus e scudi a durata")
	void removeTemporaryBonuses_onlyBonusesWithDuration() {
		// Arrange
		CreatureInPlay brute = new CreatureInPlay(BRUTE, Side.CHALLENGER);
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, 2, ModifierDuration.rounds(1)));
		brute.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));
		brute.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -1, ModifierDuration.rounds(1)));
		brute.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay()));

		// Act + Assert: 3 − 1 + 1 = 3
		assertThat(brute.removeTemporaryBonuses()).isTrue();
		assertThat(brute.attack()).isEqualTo(3);
		assertThat(brute.defense()).isEqualTo(10);
	}
}
