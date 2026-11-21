package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

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
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test delle Spell {@code DAMAGE} (Bite of the Reverse, Skin Fracture, Dissolution): subito sulla creatura avversaria
 * in campo (D-10), in attesa della prossima se non c'è (D-11). Lo sfidante pesca la Spell; l'avversaria è Kappa 6/7.
 */
class DamageSpellEffectTest {

	private static final CardDefinition KAPPA = TestCards.creature("Kappa", TestFactions.DREAM, 6, 7);

	private final EffectRegistry registry = EffectRegistry.withAllEffects();

	@ParameterizedTest(name = "{1}: Kappa {3}/{4}")
	@CsvSource({
			"BITE_OF_REVERSE, Bite of the Reverse, −3 ATK, 3, 7",
			"SKIN_FRACTURE, Skin Fracture, −5 DEF, 6, 2",
			"DISSOLUTION, Dissolution, '−2 ATK, −2 DEF', 4, 5" })
	@DisplayName("Colpisce subito la creatura avversaria in campo: malus ATK finché resta in campo, danno alla DEF")
	void resolve_opponentInPlay_hitsNow(String code, String name, String effect, int attack, int defense) {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		CreatureInPlay kappa = new CreatureInPlay(KAPPA, Side.OPPONENT);
		state.opponent().placeCreature(kappa);
		CardDefinition spell = TestCards.spell(93, name, CardSubtype.DAMAGE, code);

		// Act
		registry.createSpell(code).resolve(new EffectContext(state, Side.CHALLENGER, spell));

		// Assert
		assertThat(kappa.attack()).isEqualTo(attack);
		assertThat(kappa.defense()).isEqualTo(defense);
		assertThat(kappa.modifiers()).allSatisfy(modifier -> {
			assertThat(modifier.source()).isEqualTo(ModifierSource.SPELL);
			assertThat(modifier.side()).isEqualTo(Side.CHALLENGER);
		});
		kappa.endCombat();
		assertThat(kappa.attack()).isEqualTo(attack);
		assertThat(state.opponent().pendingModifiers()).isEmpty();
		assertThat(state.log().events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(EventType.SPELL_RESOLVED);
			assertThat(event.data()).containsEntry("effect", effect).containsEntry("target", "Kappa");
			assertThat(event.message()).isEqualTo(name + " resolves on Kappa");
		});
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "BITE_OF_REVERSE, -3, 0", "SKIN_FRACTURE, 0, 5", "DISSOLUTION, -2, 2" })
	@DisplayName("Senza creatura avversaria in campo il colpo resta in attesa della sua prossima creatura (D-11)")
	void resolve_noOpponentCreature_pendingOnOpponent(String code, int attack, int damage) {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		CardDefinition spell = TestCards.spell(93, "Spell", CardSubtype.DAMAGE, code);

		// Act
		registry.createSpell(code).resolve(new EffectContext(state, Side.CHALLENGER, spell));

		// Assert
		assertThat(state.opponent().pendingModifiers()).containsExactly(
				new PendingModifier("Spell", ModifierSource.SPELL, Side.CHALLENGER, attack, 0, damage));
		assertThat(state.challenger().pendingModifiers()).isEmpty();
		assertThat(state.log().events()).extracting(MatchEvent::message).containsExactly("Spell resolves");
	}
}
