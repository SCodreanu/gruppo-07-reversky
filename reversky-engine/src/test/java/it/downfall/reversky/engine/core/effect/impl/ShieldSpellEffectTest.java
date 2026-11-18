package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test delle Spell {@code SHIELD} (Veil of the Anchored, Registry Pact, Rain Armor, SCP-458): il bonus resta in attesa
 * della prossima creatura del proprietario e si somma agli altri (D-10, D-34). L'avversario pesca la Spell.
 */
class ShieldSpellEffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final MatchState state = TestStates.withDecks(List.of(), List.of());

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "VEIL_OF_ANCHORED, 0, 5, +5 DEF", "REGISTRY_PACT, 1, 2, '+1 ATK, +2 DEF'",
			"RAIN_ARMOR, 2, 0, +2 ATK" })
	@DisplayName("Il bonus va in attesa della prossima creatura del proprietario, la Spell si risolve senza bersaglio")
	void resolve_bonusPendingOnOwner(String code, int attack, int shield, String effect) {
		// Act
		resolve(code, Side.OPPONENT);

		// Assert
		assertThat(state.opponent().pendingModifiers()).containsExactly(
				new PendingModifier("Spell", ModifierSource.SPELL, Side.OPPONENT, attack, shield, 0));
		assertThat(state.challenger().pendingModifiers()).isEmpty();
		assertThat(state.log().events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(EventType.SPELL_RESOLVED);
			assertThat(event.data()).containsEntry("effect", effect).containsEntry("target", null);
		});
	}

	@Test
	@DisplayName("D-34: due Spell SHIELD prima della creatura restano in attesa tutte e due, nell'ordine")
	void resolve_twoSpells_bothPending() {
		// Act
		resolve(VeilOfAnchoredEffect.CODE, Side.OPPONENT);
		resolve(RainArmorEffect.CODE, Side.OPPONENT);

		// Assert
		assertThat(state.opponent().pendingModifiers()).extracting(PendingModifier::attack, PendingModifier::shield)
				.containsExactly(tuple(0, 5), tuple(2, 0));
	}

	@Test
	@DisplayName("SCP-458: +2 DEF alla prossima creatura e +2 DEF a quella dopo")
	void scp458_twoCreaturesInARow() {
		// Act
		resolve(Scp458Effect.CODE, Side.CHALLENGER);

		// Assert
		PendingModifier slice = new PendingModifier("Spell", ModifierSource.SPELL, Side.CHALLENGER, 0, 2, 0);
		assertThat(state.challenger().takePendingModifiers()).containsExactly(slice);
		assertThat(state.challenger().takePendingModifiers()).containsExactly(slice);
		assertThat(state.challenger().takePendingModifiers()).isEmpty();
		assertThat(state.log().events()).singleElement().satisfies(
				event -> assertThat(event.data()).containsEntry("effect", "+2 DEF to the next 2 creatures"));
	}

	private void resolve(String code, Side owner) {
		CardDefinition spell = TestCards.spell(96, "Spell", CardSubtype.SHIELD, code);
		registry.createSpell(code).resolve(new EffectContext(state, owner, spell));
	}
}
