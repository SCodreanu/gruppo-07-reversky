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
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
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

	private final List<String> calls = new ArrayList<>();
	private final EffectRegistry registry = new EffectRegistry()
			.registerCreature(RECORDER, () -> new Recorder(RECORDER, EffectPriority.NORMAL, calls))
			.registerCreature(NULLIFIER, () -> new Recorder(NULLIFIER, EffectPriority.NULLIFY, calls))
			.registerCreature(ROUND_START_STRIKE, RoundStartStrike::new);
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
				"A afterDamage 2", "A roundEnd");
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
		public void onRoundEnd(CreatureContext ctx) {
			calls.add(ctx.self().name() + " roundEnd");
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
