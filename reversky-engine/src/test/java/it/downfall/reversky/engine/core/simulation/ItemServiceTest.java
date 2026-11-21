package it.downfall.reversky.engine.core.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test degli Oggetti (D-47, D-48): ognuno scatta una volta nel suo momento, poi si consuma. Si provano attraverso
 * {@link DeathResolver} e {@link RoundResolver}, che chiamano {@link ItemService} nei passi giusti.
 */
class ItemServiceTest {

	private static final CardDefinition BADGE = TestCards.item(117, "Badge di Uscita Negata", TestFactions.WOUND,
			"DENIED_EXIT_BADGE");
	private static final CardDefinition COMPASS = TestCards.item(118, "Bussola dei Sentieri Falsi",
			TestFactions.INVITATION, "FALSE_PATHS_COMPASS");
	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
			"SEALED_FILE_ZERO");
	private static final CardDefinition TORII_BELL = TestCards.item(120, "Campanello del Torii", TestFactions.DREAM,
			"TORII_BELL");
	private static final CardDefinition ECHO_STRIKE = TestCards.hidden(108, "Echo Strike", CardSubtype.DAMAGE,
			"ECHO_STRIKE");
	private static final CardDefinition SANCTUARY = TestCards.hidden(105, "Last Sanctuary", CardSubtype.SURVIVAL,
			"LAST_SANCTUARY");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final DeathResolver deaths = new DeathResolver(new ItemService(registry));
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("I quattro Oggetti sono nel registro del gioco")
	void registry_fourItems() {
		assertThat(List.of("DENIED_EXIT_BADGE", "FALSE_PATHS_COMPASS", "SEALED_FILE_ZERO", "TORII_BELL"))
				.allSatisfy(code -> assertThat(registry.exists(code, CardType.ITEM)).isTrue());
	}

	@Test
	@DisplayName("Badge: la prima volta che la creatura morirebbe resta con 1 DEF; la volta dopo muore")
	void badge_savesOnce() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.challenger().placeItem(BADGE);
		CreatureInPlay a = challenger(state);
		a.takeDamage(10);

		// Act + Assert: salvata a 1 DEF, il Badge si consuma
		assertThat(deaths.resolve(state)).isFalse();
		assertThat(a.defense()).isEqualTo(1);
		assertThat(state.challenger().items()).isEmpty();
		assertThat(state.log().events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(EventType.ITEM_CONSUMED);
			assertThat(event.message()).isEqualTo("Badge di Uscita Negata is used on A");
			assertThat(event.data()).containsEntry("effect", "stays at 1 DEF");
		});
		a.takeDamage(3);
		assertThat(deaths.resolve(state)).isTrue();
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 2);
	}

	@Test
	@DisplayName("Badge con due morti nello stesso controllo: la sua creatura resta, l'altra muore da sola, con il danno")
	void badge_simultaneousDeath_becomesSingleDeath() {
		// Arrange: A a −1, B a −2; il Badge è dello sfidante
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.challenger().placeItem(BADGE);
		challenger(state).takeDamage(7);
		opponent(state).takeDamage(11);

		// Act
		deaths.resolve(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(1);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 2);
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
	}

	@Test
	@DisplayName("Bussola: scudo di 2 a inizio dei primi tre round, si rinnova e non si somma; al quarto niente")
	void compass_shieldFirstThreeRounds() {
		// Arrange: B attacca con 3; senza scudo A perderebbe 3 DEF a round
		MatchState state = TestStates.inCombat(creature("A", 0, 20), creature("B", 3, 30));
		state.challenger().placeItem(COMPASS);
		resolver.startCombat(state);

		// Act + Assert: 20 → 19 → 18 → 17 (lo scudo assorbe 2 dei 3), poi 14
		for (int expected : new int[] { 19, 18, 17, 14 }) {
			resolver.resolve(state);
			assertThat(challenger(state).defense()).isEqualTo(expected);
		}
		assertThat(state.challenger().items()).isEmpty();
		assertThat(state.log().events()).filteredOn(event -> event.type() == EventType.EFFECT_TRIGGERED)
				.extracting(MatchEvent::message)
				.containsExactly("A: +2 DEF shield this round (DEF 22)", "A: +2 DEF shield this round (DEF 21)",
						"A: +2 DEF shield this round (DEF 20)");
	}

	@Test
	@DisplayName("Bussola: se lo scontro finisce prima, i round restanti si perdono")
	void compass_combatEnds_restLost() {
		// Arrange: A uccide B al primo round
		MatchState state = TestStates.inCombat(creature("A", 9, 20), creature("B", 3, 5));
		state.challenger().placeItem(COMPASS);
		resolver.startCombat(state);
		resolver.resolve(state);

		// Act: entra una nuova avversaria, nuovo scontro
		state.opponent().placeCreature(new CreatureInPlay(creature("C", 3, 30), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: 20 − 1 (round 1, scudo) − 3 (round 2, niente scudo)
		assertThat(challenger(state).defense()).isEqualTo(16);
	}

	@Test
	@DisplayName("D-48: all'inizio dello scontro prima il Terreno, poi l'Oggetto")
	void startCombat_terrainThenItem() {
		// Arrange
		MatchState state = TestStates.inCombat(TestCards.creature("Squonk", TestFactions.INVITATION, 3, 9),
				creature("B", 4, 9));
		TestStates.placeTerrain(state,
				TestCards.terrain(114, "Hollow Clearing", TestFactions.INVITATION, "HOLLOW_CLEARING"),
				Side.CHALLENGER);
		state.challenger().placeItem(COMPASS);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.containsExactly(EventType.EFFECT_TRIGGERED, EventType.ITEM_CONSUMED);
	}

	@Test
	@DisplayName("Campanello del Torii: la prima Hidden avversaria che starebbe per attivarsi si scarta senza effetto")
	void toriiBell_cancelsEnemyHidden() {
		// Arrange: Last Sanctuary dell'avversario, il Campanello è dello sfidante
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.challenger().placeItem(TORII_BELL);
		state.opponent().setHidden(new SetHidden(SANCTUARY, 1));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(opponent(state).defense()).isEqualTo(9);
		assertThat(state.opponent().hiddenZone()).isEmpty();
		assertThat(state.challenger().items()).isEmpty();
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.containsExactly(EventType.ITEM_CONSUMED, EventType.HIDDEN_TRIGGERED);
		assertThat(state.log().events().getLast().data()).containsEntry("effect", "cancelled");
	}

	@Test
	@DisplayName("Campanello del Torii: una Hidden del proprietario non si annulla")
	void toriiBell_ownHidden_notCancelled() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.challenger().placeItem(TORII_BELL);
		state.challenger().setHidden(new SetHidden(SANCTUARY, 1));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(11);
		assertThat(state.challenger().items()).containsExactly(TORII_BELL);
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0 annulla una Hidden DAMAGE avversaria, non una SURVIVAL che non la tocca")
	void sealedFile_cancelsDamageHiddenOnly() {
		// Arrange: l'avversario ha Echo Strike (DAMAGE) attiva e Last Sanctuary (SURVIVAL) in fila; il Fascicolo è
		// dello sfidante
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.challenger().placeItem(SEALED_FILE);
		state.opponent().setHidden(new SetHidden(ECHO_STRIKE, 1));
		state.opponent().setHidden(new SetHidden(SANCTUARY, 1));

		// Act + Assert: scontro 1, Echo Strike annullata; A non perde niente e il Fascicolo si consuma
		resolver.startCombat(state);
		assertThat(challenger(state).attack()).isEqualTo(8);
		assertThat(challenger(state).defense()).isEqualTo(6);
		assertThat(state.challenger().items()).isEmpty();

		// ENG-41: la Hidden annullata libera il posto; Last Sanctuary si attiva nello scontro dopo, B riceve +5 DEF
		assertThat(state.opponent().hiddenZone()).containsExactly(new SetHidden(SANCTUARY, 2));
		assertThat(opponent(state).defense()).isEqualTo(9);
		resolver.startCombat(state);
		assertThat(opponent(state).defense()).isEqualTo(14);
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0 annulla anche una Spell DAMAGE in attesa quando arriva, non un bonus")
	void sealedFile_cancelsPendingEnemyDamage() {
		// Arrange: in attesa per A un −5 DEF dell'avversario e il suo +5 DEF
		MatchState state = TestStates.inCombat(creature("A", 8, 6), creature("B", 4, 9));
		state.challenger().placeItem(SEALED_FILE);
		state.challenger().addPendingModifier(
				new PendingModifier("Skin Fracture", ModifierSource.SPELL, Side.OPPONENT, 0, 0, 5));
		state.challenger().addPendingModifier(
				new PendingModifier("Veil of the Anchored", ModifierSource.SPELL, Side.CHALLENGER, 0, 5, 0));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(11);
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.containsExactly(EventType.ITEM_CONSUMED, EventType.EFFECT_TRIGGERED);
	}

	private static CardDefinition creature(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.ARCHIVE, attack, defense);
	}

	private static CreatureInPlay challenger(MatchState state) {
		return state.challenger().creature().orElseThrow();
	}

	private static CreatureInPlay opponent(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}
}
