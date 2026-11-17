package it.downfall.reversky.engine.core.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.FactionMatrix;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link RoundResolver}: l'ordine dei passi di un round e il regolamento base (docs/regole-base, "Un round",
 * "Esito del round", "Danno al giocatore"), con i bonus di fazione (docs/fazioni).
 */
class RoundResolverTest {

	private static final int ARCHIVE = TestFactions.ARCHIVE;

	private final RoundResolver noBonuses = new RoundResolver(TestFactions.noBonuses());
	private final RoundResolver standard = new RoundResolver(TestFactions.standardMatrix());

	@Test
	@DisplayName("Attacco simultaneo: 8/6 contro 4/9, le due DEF scendono nello stesso round (9 − 8 = 1, 6 − 4 = 2)")
	void resolve_bothSurvive_bothDefensesDecrease() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(2);
		assertThat(opponent(state).defense()).isEqualTo(1);
		assertThat(state.round()).isEqualTo(1);
	}

	@Test
	@DisplayName("Nessuna muore: nuovo round con le stesse creature e la DEF ridotta (esempio 02: 8/4 → 6/1 → 4/−2)")
	void resolve_threeRounds_defenseIsPermanent() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("Archive Puppet", 3, 10), creature("SCP-999", 2, 7));

		// Act + Assert
		noBonuses.resolve(state);
		assertThat(challenger(state).defense()).isEqualTo(8);
		assertThat(opponent(state).defense()).isEqualTo(4);
		noBonuses.resolve(state);
		assertThat(challenger(state).defense()).isEqualTo(6);
		assertThat(opponent(state).defense()).isEqualTo(1);
		noBonuses.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(30 - 2);
		assertThat(state.round()).isEqualTo(3);
	}

	@Test
	@DisplayName("Muore solo B (esempio del regolamento 8/6 contro 4/5): l'avversario perde 3 PV, A resta con DEF 2")
	void resolve_onlyOneDies_ownerLosesNegativeDefense() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 5));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(2);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(27);
		assertThat(state.challenger().lifePoints()).isEqualTo(30);
	}

	@Test
	@DisplayName("DEF finale esattamente 0: la creatura muore ma il giocatore non perde PV e non c'è PLAYER_DAMAGED")
	void resolve_defenseExactlyZero_diesWithoutPlayerDamage() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 5, 6), creature("B", 1, 5));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(30);
		assertThat(types(state)).doesNotContain(EventType.PLAYER_DAMAGED);
	}

	@Test
	@DisplayName("DEF 1 dopo il colpo: la creatura vive")
	void resolve_defenseOne_survives() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 4, 6), creature("B", 1, 5));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(opponent(state).defense()).isEqualTo(1);
	}

	@Test
	@DisplayName("Muoiono entrambe (5/5 contro 5/5): nessun danno ai giocatori, una sconfitta a testa (D-42)")
	void resolve_bothDie_noPlayerDamage() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("Dullers", 5, 5), creature("Faceling", 9, 3));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.challenger().lifePoints()).isEqualTo(30);
		assertThat(state.opponent().lifePoints()).isEqualTo(30);
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
		assertThat(state.opponent().creaturesDefeated()).isEqualTo(1);
	}

	@Test
	@DisplayName("La creatura sconfitta conta per chi la stava affrontando: solo l'avversario muore → +1 allo sfidante")
	void resolve_onlyOneDies_countsForTheOtherPlayer() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 5));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
		assertThat(state.opponent().creaturesDefeated()).isZero();
	}

	@Test
	@DisplayName("Bonus di fazione (esempio 04): Wound contro Dream attacca con 4 + 2 = 6, Dream con 4 − 1 = 3")
	void resolve_factionBonusAndMalus_appliedToAttack() {
		// Arrange
		MatchState state = TestStates.inCombat(TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5),
				TestCards.creature("Kodama", TestFactions.DREAM, 4, 6));

		// Act
		standard.resolve(state);

		// Assert: Kodama 6 − 6 = 0, muore senza danno; Crawlers 5 − 3 = 2
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(30);
		assertThat(challenger(state).defense()).isEqualTo(2);
	}

	@Test
	@DisplayName("Il malus non porta l'attacco sotto 0: ATK 0 con −1 attacca con 0, non cura (spostato da ENG-04)")
	void resolve_malusOnZeroAttack_attackStaysZero() {
		// Arrange: Dream ha −1 contro Wound, Wound ha +2 contro Dream
		MatchState state = TestStates.inCombat(TestCards.creature("Kasa-obake", TestFactions.DREAM, 0, 5),
				TestCards.creature("Crawlers", TestFactions.WOUND, 0, 5));

		// Act
		standard.resolve(state);

		// Assert: Kasa-obake attacca con 0, non con −1, quindi Crawlers resta a 5
		assertThat(opponent(state).defense()).isEqualTo(5);
		assertThat(challenger(state).defense()).isEqualTo(3);
		assertThat(attackTotals(state)).containsExactly(0, 2);
	}

	@Test
	@DisplayName("Due fazioni (esempio 11): Rootbound 6 + 0 + 2 = 8 contro Archive Puppet 6 + 0 − 1 = 5")
	void resolve_twoFactionCard_sumsAllPairs() {
		// Arrange
		MatchState state = TestStates.inCombat(
				TestCards.creature("The Rootbound Doppelganger", TestFactions.DREAM, TestFactions.INVITATION, 6, 10),
				TestCards.creature("Archive Puppet", ARCHIVE, 6, 6));

		// Act
		standard.resolve(state);

		// Assert
		assertThat(attackTotals(state)).containsExactly(8, 5);
		assertThat(state.opponent().lifePoints()).isEqualTo(28);
		assertThat(challenger(state).defense()).isEqualTo(5);
	}

	@Test
	@DisplayName("Bonus DEF di fazione (esempio 05): +3 una sola volta a inizio scontro, non a ogni round")
	void startCombat_defenseBonus_appliedOnce() {
		// Arrange: Archive → Invitation defBonus 3, nessun bonus ATK
		RoundResolver resolver = new RoundResolver(new FactionMatrix(List.of(
				new FactionBonus(ARCHIVE, TestFactions.INVITATION, 0, 3))));
		MatchState state = TestStates.inCombat(TestCards.creature("SCP-1762", ARCHIVE, 3, 4),
				TestCards.creature("Squonk", TestFactions.INVITATION, 3, 9));

		// Act + Assert: DEF 4/6 → 1/3 → −2/0, muoiono entrambe al round 3
		resolver.startCombat(state);
		assertThat(challenger(state).defense()).isEqualTo(7);
		resolver.resolve(state);
		resolver.resolve(state);
		assertThat(challenger(state).defense()).isEqualTo(1);
		resolver.resolve(state);
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().creature()).isEmpty();
	}

	@Test
	@DisplayName("Il bonus DEF di fazione si registra con EFFECT_TRIGGERED solo se è maggiore di 0")
	void startCombat_defenseBonus_loggedOnlyWhenPositive() {
		// Arrange
		RoundResolver resolver = new RoundResolver(new FactionMatrix(List.of(
				new FactionBonus(ARCHIVE, TestFactions.INVITATION, 0, 3))));
		MatchState state = TestStates.inCombat(TestCards.creature("SCP-1762", ARCHIVE, 3, 4),
				TestCards.creature("Squonk", TestFactions.INVITATION, 3, 9));

		// Act
		resolver.startCombat(state);

		// Assert: solo lo sfidante ha un bonus
		List<MatchEvent> events = state.log().events();
		assertThat(events).hasSize(1);
		assertThat(events.getFirst().type()).isEqualTo(EventType.EFFECT_TRIGGERED);
		assertThat(events.getFirst().side()).isEqualTo(Side.CHALLENGER);
		assertThat(events.getFirst().message()).isEqualTo("SCP-1762: +3 DEF faction bonus (DEF 7)");
	}

	@Test
	@DisplayName("La DEF all'entrata si fissa a inizio del primo scontro, bonus compresi (M-06)")
	void startCombat_recordsInitialDefenseWithBonus() {
		// Arrange
		RoundResolver resolver = new RoundResolver(new FactionMatrix(List.of(
				new FactionBonus(ARCHIVE, TestFactions.INVITATION, 0, 3))));
		MatchState state = TestStates.inCombat(TestCards.creature("SCP-1762", ARCHIVE, 3, 4),
				TestCards.creature("Squonk", TestFactions.INVITATION, 3, 9));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(challenger(state).initialDefense()).isEqualTo(7);
		assertThat(opponent(state).initialDefense()).isEqualTo(9);
	}

	@Test
	@DisplayName("D-34: i modificatori in attesa arrivano dopo il bonus DEF di fazione (4 + 3 − 5 = 2, non morta)")
	void startCombat_pendingAfterFactionBonus() {
		// Arrange: Skin Fracture pescata dall'avversario quando lo sfidante non aveva creature in campo
		RoundResolver resolver = new RoundResolver(new FactionMatrix(List.of(
				new FactionBonus(ARCHIVE, TestFactions.INVITATION, 0, 3))));
		MatchState state = TestStates.inCombat(TestCards.creature("SCP-1762", ARCHIVE, 3, 4),
				TestCards.creature("Squonk", TestFactions.INVITATION, 3, 9));
		state.challenger().addPendingModifier(
				new PendingModifier("Skin Fracture", ModifierSource.SPELL, Side.OPPONENT, 0, 0, 5));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(2);
		assertThat(challenger(state).initialDefense()).isEqualTo(2);
		assertThat(state.challenger().pendingModifiers()).isEmpty();
		assertThat(state.log().events()).extracting(MatchEvent::message).containsExactly(
				"SCP-1762: +3 DEF faction bonus (DEF 7)",
				"SCP-1762: −5 DEF from Skin Fracture (ATK 3, DEF 2)");
	}

	@Test
	@DisplayName("I modificatori in attesa vanno prima allo sfidante, poi all'avversario, e si sommano tutti")
	void startCombat_pendingBothSides_challengerFirst() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.opponent().addPendingModifier(
				new PendingModifier("Veil of the Anchored", ModifierSource.SPELL, Side.OPPONENT, 0, 5, 0));
		state.opponent().addPendingModifier(
				new PendingModifier("Rain Armor", ModifierSource.SPELL, Side.OPPONENT, 2, 0, 0));
		state.challenger().addPendingModifier(
				new PendingModifier("Bite of the Reverse", ModifierSource.SPELL, Side.OPPONENT, -3, 0, 0));

		// Act
		noBonuses.startCombat(state);

		// Assert
		assertThat(challenger(state).attack()).isEqualTo(5);
		assertThat(opponent(state).attack()).isEqualTo(6);
		assertThat(opponent(state).defense()).isEqualTo(14);
		assertThat(state.log().events()).extracting(MatchEvent::side)
				.containsExactly(Side.CHALLENGER, Side.OPPONENT, Side.OPPONENT);
	}

	@Test
	@DisplayName("Una creatura sopravvissuta non riceve i modificatori in attesa: aspettano la prossima creatura")
	void startCombat_survivor_pendingWaits() {
		// Arrange: A ha già avuto il suo primo scontro
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		noBonuses.startCombat(state);
		PendingModifier slice = new PendingModifier("SCP-458", ModifierSource.SPELL, Side.CHALLENGER, 0, 2, 0);
		state.challenger().addPendingModifier(slice);

		// Act
		noBonuses.startCombat(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(6);
		assertThat(state.challenger().pendingModifiers()).containsExactly(slice);
	}

	@Test
	@DisplayName("D-08: un danno in attesa letale uccide a inizio scontro (4 − 7 = −3): il proprietario perde 3 PV")
	void startCombat_lethalPending_deathBeforeTheRound() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 4), creature("B", 4, 9));
		state.challenger().addPendingModifier(
				new PendingModifier("Skin Fracture", ModifierSource.SPELL, Side.OPPONENT, 0, 0, 7));

		// Act
		noBonuses.startCombat(state);

		// Assert: nessun round giocato, B resta in campo con la DEF all'entrata fissata
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 3);
		assertThat(state.opponent().creaturesDefeated()).isEqualTo(1);
		assertThat(opponent(state).hasStartedCombat()).isTrue();
		assertThat(state.round()).isZero();
	}

	@Test
	@DisplayName("Ordine degli eventi (log minimo dell'esempio 01): round, due attacchi, due danni, morte, danno al giocatore")
	void resolve_eventsInRuleOrder() {
		// Arrange
		MatchState state = TestStates.inCombat(TestCards.creature("SCP-131", ARCHIVE, 8, 6),
				TestCards.creature("Eternal Cup", ARCHIVE, 4, 5));

		// Act
		standard.resolve(state);

		// Assert
		assertThat(state.log().events()).extracting(MatchEvent::type, MatchEvent::side).containsExactly(
				tuple(EventType.ROUND_STARTED, null),
				tuple(EventType.ATTACK_COMPUTED, Side.CHALLENGER),
				tuple(EventType.ATTACK_COMPUTED, Side.OPPONENT),
				tuple(EventType.DAMAGE_DEALT, Side.CHALLENGER),
				tuple(EventType.DAMAGE_DEALT, Side.OPPONENT),
				tuple(EventType.CREATURE_DIED, Side.OPPONENT),
				tuple(EventType.PLAYER_DAMAGED, Side.OPPONENT));
		assertThat(state.log().events().getLast().data()).containsEntry("amount", 3).containsEntry("lifePoints", 27);
	}

	@Test
	@DisplayName("Un modificatore per 1 round vale solo nel prossimo round: +3 ATK al round 1, poi niente")
	void resolve_roundModifier_appliesOnlyForItsRound() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 2, 20), creature("B", 0, 20));
		challenger(state).addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, 3,
				ModifierDuration.rounds(1)));

		// Act + Assert
		noBonuses.resolve(state);
		assertThat(opponent(state).defense()).isEqualTo(15);
		noBonuses.resolve(state);
		assertThat(opponent(state).defense()).isEqualTo(13);
	}

	@Test
	@DisplayName("A fine scontro la sopravvissuta perde i modificatori fino a fine scontro e tiene gli altri")
	void resolve_combatEnds_survivorLosesCombatModifiers() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 5));
		CreatureInPlay survivor = challenger(state);
		survivor.addModifier(StatModifier.attack(ModifierSource.HIDDEN, Side.CHALLENGER, 2,
				ModifierDuration.untilCombatEnd()));
		survivor.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay()));

		// Act
		noBonuses.resolve(state);

		// Assert
		assertThat(survivor.attack()).isEqualTo(9);
	}

	@Test
	@DisplayName("Risolvere un round o iniziare uno scontro senza due creature in campo è un bug")
	void withoutTwoCreatures_throws() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());

		// Act + Assert
		assertThatThrownBy(() -> noBonuses.resolve(state)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> noBonuses.startCombat(state)).isInstanceOf(IllegalStateException.class);
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, ARCHIVE, attack, defense);
	}

	private static CreatureInPlay challenger(MatchState state) {
		return state.challenger().creature().orElseThrow();
	}

	private static CreatureInPlay opponent(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}

	private static List<EventType> types(MatchState state) {
		return state.log().events().stream().map(MatchEvent::type).toList();
	}

	private static List<Object> attackTotals(MatchState state) {
		return state.log().events().stream()
				.filter(event -> event.type() == EventType.ATTACK_COMPUTED)
				.map(event -> event.data().get("total"))
				.toList();
	}
}
