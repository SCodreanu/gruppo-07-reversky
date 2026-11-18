package it.downfall.reversky.engine.core.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectPriority;
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
import it.downfall.reversky.engine.core.model.Wound;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * I momenti degli effetti delle creature nei passi del round (docs/effetti, "Ordine di risoluzione di un round") e
 * l'ordine D-30, con effetti di prova che registrano le chiamate. Si provano attraverso il {@link RoundResolver}.
 */
class CreatureEffectServiceTest {

	private static final String RECORDER = "TEST_RECORDER";
	private static final String NULLIFIER = "TEST_NULLIFIER";
	private static final String ROUND_START_STRIKE = "TEST_ROUND_START_STRIKE";
	private static final String WOUND_RECORDER = "TEST_WOUND_RECORDER";

	private final List<String> calls = new ArrayList<>();
	private final EffectRegistry registry = new EffectRegistry()
			.registerCreature(RECORDER, () -> new Recorder(RECORDER, EffectPriority.NORMAL, calls))
			.registerCreature(NULLIFIER, () -> new Recorder(NULLIFIER, EffectPriority.NULLIFY, calls))
			.registerCreature(ROUND_START_STRIKE, RoundStartStrike::new)
			.registerCreature(WOUND_RECORDER, () -> new WoundRecorder(WOUND_RECORDER, calls));
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("Un round chiama i momenti nell'ordine dei passi: entrata, inizio, attacco, danno, dopo, fine")
	void round_callsMomentsInStepOrder() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 3, 20), creature("B", null, 2, 20));

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(calls).containsExactly("A enter", "A roundStart", "A outgoing 3", "A incoming 2",
				"A afterDamage 2", "A afterAttack 3", "A roundEnd");
	}

	@Test
	@DisplayName("D-30: prima la priorità NULLIFY, poi lo sfidante")
	void sameMoment_nullifyFirstThenChallenger() {
		// Arrange: l'avversario ha l'effetto NULLIFY
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 3, 20),
				creature("B", NULLIFIER, 2, 20));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(calls).containsExactly("B enter", "A enter");
	}

	@Test
	@DisplayName("D-31: onEnterPlay una volta per entrata, non a ogni nuovo scontro")
	void enterPlay_oncePerEntry() {
		// Arrange: B muore al round 1, entra C
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 9, 20), creature("B", null, 1, 5));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Act
		state.opponent().placeCreature(new CreatureInPlay(creature("C", null, 1, 30), Side.OPPONENT));
		resolver.startCombat(state);

		// Assert
		assertThat(calls).filteredOn(call -> call.endsWith("enter")).containsExactly("A enter");
	}

	@Test
	@DisplayName("Passo 5 riceve la ferita dopo gli scudi (5 − 2 = 3); passo 6 solo per le creature vive")
	void afterDamageGetsWound_roundEndOnlyAlive() {
		// Arrange: A ha uno scudo di 2 e muore lo stesso
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 1, 3), creature("B", null, 5, 20));
		state.challenger().creature().orElseThrow().addModifier(
				StatModifier.shield(ModifierSource.SPELL, Side.CHALLENGER, 2, ModifierDuration.whileInPlay()));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert: la ferita conta solo la DEF sopra 0 (3), e A morta non ha onRoundEnd
		assertThat(calls).contains("A afterDamage 3").doesNotContain("A roundEnd");
		assertThat(state.challenger().creature()).isEmpty();
	}

	@Test
	@DisplayName("D-08: una morte al passo 1 interrompe il round: conta, nessuno attacca, i modificatori scadono")
	void deathAtRoundStart_interruptsRound() {
		// Arrange: A a inizio round fa 10 danni a B; B ha un malus di un round
		MatchState state = TestStates.inCombat(registry, creature("A", ROUND_START_STRIKE, 3, 20),
				creature("B", null, 2, 10));
		CreatureInPlay a = state.challenger().creature().orElseThrow();
		a.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.OPPONENT, -1, ModifierDuration.rounds(1)));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.round()).isEqualTo(1);
		assertThat(a.attack()).isEqualTo(3);
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.doesNotContain(EventType.ATTACK_COMPUTED, EventType.DAMAGE_DEALT);
	}

	@Test
	@DisplayName("Una creatura senza effetto gioca come prima: nessun evento di effetto")
	void noEffect_playsPlain() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, creature("A", null, 3, 20), creature("B", null, 2, 20));

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().effect()).isEmpty();
		assertThat(state.log().events()).extracting(MatchEvent::type).doesNotContain(EventType.EFFECT_TRIGGERED);
	}

	@Test
	@DisplayName("18 novembre: onWounded una volta per ferita, con la causa, dopo la fase; poi onDefenseCheck")
	void wounds_reportedOncePerWoundWithCause() {
		// Arrange: A riceve due danni in attesa (1 e 2) da una Spell, poi B la colpisce con 2
		MatchState state = TestStates.inCombat(registry, creature("A", WOUND_RECORDER, 3, 20),
				creature("B", null, 2, 20));
		state.challenger().addPendingModifier(new PendingModifier("X", ModifierSource.SPELL, Side.OPPONENT, 0, 0, 1));
		state.challenger().addPendingModifier(new PendingModifier("Y", ModifierSource.SPELL, Side.OPPONENT, 0, 0, 2));

		// Act
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(calls).containsSubsequence("A wounded 1 by none", "A wounded 2 by none", "A check 17",
				"A wounded 2 by B", "A check 15");
		assertThat(calls).filteredOn(call -> call.contains("wounded")).hasSize(3);
	}

	@Test
	@DisplayName("Una creatura morta non riceve onWounded né onDefenseCheck")
	void deadCreature_notReported() {
		// Arrange: A 3/2 muore al round 1
		MatchState state = TestStates.inCombat(registry, creature("A", WOUND_RECORDER, 3, 2),
				creature("B", null, 5, 20));
		resolver.startCombat(state);
		calls.clear();

		// Act
		resolver.resolve(state);

		// Assert: il controllo del passo 1 la vede ancora viva; la ferita letale non arriva e dopo non c'è altro
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(calls).containsOnly("A check 2");
	}

	@Test
	@DisplayName("ENG-34: afterAttack solo se l'attacco ha fatto danni sopra 0")
	void afterAttack_onlyWithDamage() {
		// Arrange: A ha ATK 0
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 0, 20), creature("B", null, 2, 20));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(calls).contains("A afterDamage 2").noneMatch(call -> call.contains("afterAttack"));
	}

	@Test
	@DisplayName("ENG-34: onKill della sopravvissuta a scontro finito: l'avversaria è uscita, lo scudo è scaduto")
	void onKill_survivorAfterCombatEnd() {
		// Arrange: A 9/20 con uno scudo fino a fine scontro di 4 uccide B 1/5
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 9, 20), creature("B", null, 1, 5));
		state.challenger().creature().orElseThrow().addModifier(StatModifier.shield(ModifierSource.HIDDEN,
				Side.CHALLENGER, 4, ModifierDuration.untilCombatEnd()));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert: il colpo di B lo prende lo scudo, poi lo scudo scade
		assertThat(calls).containsSubsequence("A roundEnd", "A kill B, enemy out, DEF 20");
	}

	@Test
	@DisplayName("ENG-34: una morte fuori dal round (danno da Spell) conta come eliminazione (D-42)")
	void onKill_deathOutsideRound() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 3, 20), creature("B", null, 1, 5));
		state.opponent().creature().orElseThrow().takeDamage(5);

		// Act
		new DeathResolver(new ItemService(registry)).resolve(state);

		// Assert
		assertThat(calls).containsExactly("A kill B, enemy out, DEF 20");
	}

	@Test
	@DisplayName("ENG-34: in una morte doppia nessuno riceve onKill")
	void onKill_doubleDeath_none() {
		// Arrange: A 9/1 e B 5/5 muoiono insieme
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 9, 1), creature("B", null, 5, 5));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(calls).noneMatch(call -> call.contains("kill"));
	}

	@Test
	@DisplayName("ENG-34: un effetto collegato riceve i momenti subito dopo quello della creatura")
	void attachedEffect_calledAfterOwn() {
		// Arrange
		MatchState state = TestStates.inCombat(registry, creature("A", RECORDER, 3, 20), creature("B", null, 2, 20));
		state.challenger().creature().orElseThrow().attachEffect(new AttachedProbe(calls));
		resolver.startCombat(state);
		calls.clear();

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(calls).containsSubsequence("A roundStart", "attached roundStart", "A roundEnd",
				"attached roundEnd");
	}

	private static CardDefinition creature(String name, String code, int attack, int defense) {
		return TestCards.creature(0, name, TestFactions.WOUND, null, "Uncommon", attack, defense, code);
	}

	/** Effetto di prova: registra ogni chiamata con il nome della creatura. */
	private record Recorder(String code, EffectPriority priority, List<String> calls) implements CreatureEffect {

		@Override
		public void onEnterPlay(CreatureContext ctx) {
			calls.add(ctx.self().name() + " enter");
		}

		@Override
		public void onRoundStart(CreatureContext ctx) {
			calls.add(ctx.self().name() + " roundStart");
		}

		@Override
		public int modifyOutgoingAttack(CreatureContext ctx, int attack) {
			calls.add(ctx.self().name() + " outgoing " + attack);
			return attack;
		}

		@Override
		public int modifyIncomingDamage(CreatureContext ctx, int damage) {
			calls.add(ctx.self().name() + " incoming " + damage);
			return damage;
		}

		@Override
		public void afterDamage(CreatureContext ctx, int damage, int wound) {
			calls.add(ctx.self().name() + " afterDamage " + wound);
		}

		@Override
		public void afterAttack(CreatureContext ctx, int damage) {
			calls.add(ctx.self().name() + " afterAttack " + damage);
		}

		@Override
		public void onRoundEnd(CreatureContext ctx) {
			calls.add(ctx.self().name() + " roundEnd");
		}

		@Override
		public void onKill(CreatureContext ctx, CreatureInPlay defeated) {
			calls.add(ctx.self().name() + " kill " + defeated.name() + ", enemy "
					+ (ctx.enemy().isPresent() ? "in" : "out") + ", DEF " + ctx.self().defense());
		}
	}

	/** Effetto collegato di prova (ENG-34): registra inizio e fine del round. */
	private record AttachedProbe(List<String> calls) implements CreatureEffect {

		@Override
		public String code() {
			return "TEST_ATTACHED";
		}

		@Override
		public void onRoundStart(CreatureContext ctx) {
			calls.add("attached roundStart");
		}

		@Override
		public void onRoundEnd(CreatureContext ctx) {
			calls.add("attached roundEnd");
		}
	}

	/** Effetto di prova: registra le ferite riferite e i controlli della DEF. */
	private record WoundRecorder(String code, List<String> calls) implements CreatureEffect {

		@Override
		public void onWounded(CreatureContext ctx, Wound wound) {
			calls.add(ctx.self().name() + " wounded " + wound.amount() + " by "
					+ wound.cause().map(CreatureInPlay::name).orElse("none"));
		}

		@Override
		public void onDefenseCheck(CreatureContext ctx) {
			calls.add(ctx.self().name() + " check " + ctx.self().defense());
		}
	}

	/** Effetto di prova: a inizio round 10 danni da effetto alla creatura avversaria. */
	private static final class RoundStartStrike implements CreatureEffect {

		@Override
		public String code() {
			return ROUND_START_STRIKE;
		}

		@Override
		public void onRoundStart(CreatureContext ctx) {
			ctx.targetEnemy().ifPresent(enemy -> enemy.takeDamage(10));
		}
	}
}
