package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test delle Hidden {@code P1} (docs/effetti/catalogo-effetti, "Hidden"), attivate dallo sfidante contro Kappa
 * dell'avversario: un test per ogni sottotipo, {@code SURVIVAL} e {@code DAMAGE}. L'ordine di attivazione è in
 * {@code RoundResolverTest}.
 */
class HiddenEffectsTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final MatchState state = TestStates.inCombat(TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5),
			TestCards.creature("Kappa", TestFactions.DREAM, 6, 7));
	private final CreatureInPlay crawlers = state.challenger().creature().orElseThrow();
	private final CreatureInPlay kappa = state.opponent().creature().orElseThrow();

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "LAST_SANCTUARY, 4, 10, '+5 DEF to Crawlers this combat'",
			"SURVIVAL_INSTINCT, 6, 6, '+2 ATK, +1 DEF to Crawlers this combat'" })
	@DisplayName("SURVIVAL: ATK e DEF in più alla propria creatura, solo nello scontro corrente; la DEF è uno scudo")
	void survival_bonusUntilCombatEnd(String code, int attack, int defense, String effect) {
		// Act
		trigger(code, CardSubtype.SURVIVAL);

		// Assert
		assertThat(crawlers.attack()).isEqualTo(attack);
		assertThat(crawlers.defense()).isEqualTo(defense);
		assertThat(crawlers.modifiers()).allSatisfy(modifier -> {
			assertThat(modifier.source()).isEqualTo(ModifierSource.HIDDEN);
			assertThat(modifier.durationKind()).isEqualTo(ModifierDuration.Kind.UNTIL_COMBAT_END);
		});
		assertThat(state.log().events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(EventType.HIDDEN_TRIGGERED);
			assertThat(event.data()).containsEntry("effect", effect);
		});
		crawlers.endCombat();
		assertThat(crawlers.attack()).isEqualTo(4);
		assertThat(crawlers.defense()).isEqualTo(5);
	}

	@Test
	@DisplayName("Last Sanctuary: lo scudo assorbe per primo, e a fine scontro sparisce solo la parte non usata")
	void lastSanctuary_shieldAbsorbsFirst() {
		// Arrange
		trigger(LastSanctuaryEffect.CODE, CardSubtype.SURVIVAL);

		// Act: 7 danni, 5 li assorbe lo scudo
		crawlers.takeDamage(7);
		crawlers.endCombat();

		// Assert
		assertThat(crawlers.defense()).isEqualTo(3);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "HOSTILE_PRESENCE, 1, 7, '−5 ATK in the first round to Kappa'",
			"ECHO_STRIKE, 4, 5, '−2 ATK in the first round, −2 DEF to Kappa'" })
	@DisplayName("DAMAGE: il malus ATK vale solo il primo round dello scontro, il danno alla DEF è permanente")
	void damage_attackFirstRoundOnly(String code, int attack, int defense, String effect) {
		// Act
		trigger(code, CardSubtype.DAMAGE);

		// Assert
		assertThat(kappa.attack()).isEqualTo(attack);
		assertThat(kappa.defense()).isEqualTo(defense);
		assertThat(kappa.modifiers()).allSatisfy(modifier -> {
			assertThat(modifier.source()).isEqualTo(ModifierSource.HIDDEN);
			assertThat(modifier.side()).isEqualTo(Side.CHALLENGER);
		});
		assertThat(state.log().events()).singleElement()
				.satisfies(event -> assertThat(event.data()).containsEntry("effect", effect));
		kappa.endRound();
		assertThat(kappa.attack()).isEqualTo(6);
		assertThat(kappa.defense()).isEqualTo(defense);
	}

	@Test
	@DisplayName("Breaking Point: avversaria con 7 DEF o meno → DEF 0, muore senza danni al giocatore")
	void breakingPoint_lowDefense_toZero() {
		// Act: Kappa ha DEF 7
		trigger(BreakingPointEffect.CODE, CardSubtype.DAMAGE);

		// Assert
		assertThat(kappa.defense()).isZero();
		assertThat(kappa.isDead()).isTrue();
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.HIDDEN_TRIGGERED);
		assertThat(state.log().events().getFirst().data()).containsEntry("effect", "DEF of Kappa to 0");
	}

	@Test
	@DisplayName("Breaking Point: avversaria con più di 7 DEF, scudi compresi (7 + 2 = 9) → −4 DEF")
	void breakingPoint_highDefense_minusFour() {
		// Arrange
		kappa.addModifier(StatModifier.shield(ModifierSource.SPELL, Side.OPPONENT, 2,
				ModifierDuration.whileInPlay()));

		// Act
		trigger(BreakingPointEffect.CODE, CardSubtype.DAMAGE);

		// Assert: lo scudo assorbe 2 dei 4 danni
		assertThat(kappa.defense()).isEqualTo(5);
		assertThat(state.log().events().getFirst().data()).containsEntry("effect", "−4 DEF to Kappa");
	}

	@Test
	@DisplayName("Il registro del gioco ha le cinque Hidden P1; quelle con condizione (P2) non ci sono ancora")
	void registry_p1HiddenOnly() {
		assertThat(List.of(LastSanctuaryEffect.CODE, SurvivalInstinctEffect.CODE, HostilePresenceEffect.CODE,
				EchoStrikeEffect.CODE, BreakingPointEffect.CODE))
				.allSatisfy(code -> assertThat(registry.exists(code, CardType.HIDDEN)).isTrue());
		assertThat(registry.appliesTo("REMNANTS_OF_REALITY")).isEmpty();
		assertThat(registry.appliesTo("UNSEEN_EXIT")).isEmpty();
		assertThat(registry.appliesTo("STATIC_INTERFERENCE")).isEmpty();
	}

	private void trigger(String code, CardSubtype subtype) {
		CardDefinition hidden = TestCards.hidden(105, "Hidden", subtype, code);
		registry.createHidden(code).trigger(new EffectContext(state, Side.CHALLENGER, hidden));
	}
}
