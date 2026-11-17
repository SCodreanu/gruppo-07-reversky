package it.downfall.reversky.engine.core.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * Test di {@link DeathResolver}: le regole del passo 7 valgono dopo ogni fase, anche fuori da un round (D-08). Qui la
 * DEF scende con un danno da effetto ({@code takeDamage} fuori dal passo 4), come farà una Spell o una Hidden.
 */
class DeathResolverTest {

	private final DeathResolver deaths = new DeathResolver(new ItemService(EffectRegistry.withAllEffects()));

	@Test
	@DisplayName("Nessuna creatura morta: niente cambia e lo scontro continua")
	void resolve_nobodyDead_false() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));

		// Act + Assert
		assertThat(deaths.resolve(state)).isFalse();
		assertThat(state.challenger().hasCreature()).isTrue();
		assertThat(state.opponent().hasCreature()).isTrue();
		assertThat(state.log().events()).isEmpty();
	}

	@Test
	@DisplayName("Danno da effetto letale (6 − 9 = −3): lascia il campo, il proprietario perde 3 PV, conta per l'altro")
	void resolve_oneDead_ownerDamagedAndCounted() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		creature(state, Side.CHALLENGER).takeDamage(9);

		// Act
		boolean combatEnded = deaths.resolve(state);

		// Assert
		assertThat(combatEnded).isTrue();
		assertThat(state.challenger().hasCreature()).isFalse();
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 3);
		assertThat(state.opponent().creaturesDefeated()).isEqualTo(1);
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.containsExactly(EventType.CREATURE_DIED, EventType.PLAYER_DAMAGED);
	}

	@Test
	@DisplayName("La sopravvissuta perde i modificatori fino a fine scontro e tiene quelli finché resta in campo")
	void resolve_oneDead_survivorEndsCombat() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		CreatureInPlay survivor = creature(state, Side.OPPONENT);
		survivor.addModifier(StatModifier.attack(ModifierSource.HIDDEN, Side.OPPONENT, 2,
				ModifierDuration.untilCombatEnd()));
		survivor.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.OPPONENT, 1,
				ModifierDuration.whileInPlay()));
		creature(state, Side.CHALLENGER).takeDamage(6);

		// Act
		deaths.resolve(state);

		// Assert
		assertThat(survivor.attackModifiers()).isEqualTo(1);
	}

	@Test
	@DisplayName("DEF esattamente 0: muore ma il proprietario non perde punti vita e non c'è PLAYER_DAMAGED")
	void resolve_defenseExactlyZero_noPlayerDamage() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		creature(state, Side.OPPONENT).takeDamage(9);

		// Act
		deaths.resolve(state);

		// Assert
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CREATURE_DIED);
	}

	@Test
	@DisplayName("Muoiono entrambe nello stesso controllo: nessun giocatore perde punti vita, contano per tutti e due")
	void resolve_bothDead_noPlayerDamage() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		creature(state, Side.CHALLENGER).takeDamage(10);
		creature(state, Side.OPPONENT).takeDamage(12);

		// Act
		deaths.resolve(state);

		// Assert: prima lo sfidante, poi l'avversario
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
		assertThat(state.opponent().creaturesDefeated()).isEqualTo(1);
		assertThat(state.log().events()).extracting(event -> event.data().get("card"))
				.containsExactly("A", "B");
	}

	@Test
	@DisplayName("Fuori da un round con un solo campo occupato (es. Spell alla pescata): la creatura muore e basta")
	void resolve_onlyOneCreatureInPlay_dies() {
		// Arrange: lo sfidante ha il campo vuoto, come quando pesca dopo la morte della sua creatura
		MatchState state = TestStates.withDecks(List.of(), List.of());
		CreatureInPlay lonely = new CreatureInPlay(creature("B", 4, 5), Side.OPPONENT);
		state.opponent().placeCreature(lonely);
		lonely.takeDamage(7);

		// Act
		boolean combatEnded = deaths.resolve(state);

		// Assert
		assertThat(combatEnded).isTrue();
		assertThat(state.opponent().hasCreature()).isFalse();
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 2);
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
	}

	@Test
	@DisplayName("Campi vuoti: niente da controllare")
	void resolve_emptyFields_false() {
		assertThat(deaths.resolve(TestStates.withDecks(List.of(), List.of()))).isFalse();
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.ARCHIVE, attack, defense);
	}

	private static CreatureInPlay creature(MatchState state, Side side) {
		return state.player(side).creature().orElseThrow();
	}
}
