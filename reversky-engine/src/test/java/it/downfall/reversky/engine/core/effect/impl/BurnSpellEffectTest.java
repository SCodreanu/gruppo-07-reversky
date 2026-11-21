package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test delle Spell {@code BURN} (D-13, <i>Nessuna carta persa</i>): Call of the Rift, Reverse Fusion, Echo of the
 * Forgotten. Lo sfidante pesca la Burn; la prossima carta del suo mazzo è una creatura, una Spell, o niente.
 */
class BurnSpellEffectTest {

	private static final CardDefinition KAPPA = TestCards.creature("Kappa", TestFactions.DREAM, 6, 7);
	private static final CardDefinition VEIL = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "CRACK_CALL, 2, 2", "REVERSE_FUSION, 2, 1", "FORGOTTEN_ECHO, 2, 1" })
	@DisplayName("La carta rivelata è una creatura: entrerà con il bonus fisso della Burn, e resta in cima al mazzo")
	void resolve_creatureRevealed_fixedBonus(String code, int attack, int shield) {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(KAPPA, VEIL), List.of());

		// Act
		resolve(state, code);

		// Assert
		assertThat(state.challenger().pendingModifiers()).extracting(PendingModifier::attack, PendingModifier::shield)
				.containsExactly(tuple(attack, shield));
		assertThat(state.challenger().deck()).containsExactly(KAPPA, VEIL);
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.containsExactly(EventType.BURN_REVEALED, EventType.SPELL_RESOLVED);
		assertThat(state.log().events().getFirst().data())
				.containsEntry("revealed", "Kappa").containsEntry("bonusApplied", true);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "CRACK_CALL, 0, 0, no effect", "REVERSE_FUSION, 1, 0, +1 ATK", "FORGOTTEN_ECHO, 0, 2, +2 DEF" })
	@DisplayName("La carta rivelata non è una creatura: non si scarta nulla; Fusion ed Echo danno il bonus minore")
	void resolve_nonCreatureRevealed_nothingDiscarded(String code, int attack, int shield, String effect) {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(VEIL, KAPPA), List.of());

		// Act
		resolve(state, code);

		// Assert
		assertThat(state.challenger().deck()).containsExactly(VEIL, KAPPA);
		if (attack == 0 && shield == 0) {
			assertThat(state.challenger().pendingModifiers()).isEmpty();
		} else {
			assertThat(state.challenger().pendingModifiers())
					.extracting(PendingModifier::attack, PendingModifier::shield)
					.containsExactly(tuple(attack, shield));
		}
		assertThat(state.log().events().getFirst().data())
				.containsEntry("revealed", "Veil of the Anchored").containsEntry("bonusApplied", false);
		assertThat(state.log().events().getLast().data()).containsEntry("effect", effect);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "CRACK_CALL", "REVERSE_FUSION", "FORGOTTEN_ECHO" })
	@DisplayName("Mazzo vuoto: non succede nulla, il log dice che non c'era niente da rivelare")
	void resolve_emptyDeck_nothing(String code) {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());

		// Act
		resolve(state, code);

		// Assert
		assertThat(state.challenger().pendingModifiers()).isEmpty();
		assertThat(state.log().events().getFirst().message()).endsWith("reveals nothing");
		assertThat(state.log().events().getFirst().data()).containsEntry("revealed", null);
	}

	private void resolve(MatchState state, String code) {
		CardDefinition burn = TestCards.spell(102, "Burn", CardSubtype.BURN, code);
		registry.createSpell(code).resolve(new EffectContext(state, Side.CHALLENGER, burn));
	}
}
