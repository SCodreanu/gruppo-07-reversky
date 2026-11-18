package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.Penalties;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.core.simulation.DeathResolver;
import it.downfall.reversky.engine.core.simulation.ItemService;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti {@code P2} di sopravvivenza (ENG-37, D-15, D-36): le Hidden con condizione Remnants of Reality e Unseen Exit
 * e The Undying Blossom. Si provano attraverso il {@link RoundResolver} e il {@link DeathResolver}.
 */
class SurvivalEffectsTest {

	private static final CardDefinition REMNANTS = TestCards.hidden(106, "Remnants of Reality", CardSubtype.SURVIVAL,
			"REMNANTS_OF_REALITY");
	private static final CardDefinition UNSEEN_EXIT = TestCards.hidden(107, "Unseen Exit", CardSubtype.SURVIVAL,
			"UNSEEN_EXIT");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);
	private final DeathResolver deaths = new DeathResolver(new ItemService(registry));

	// ---- Remnants of Reality ----

	@Test
	@DisplayName("Remnants of Reality: una penalità DEF torna indietro al controllo dopo la fase (20 − 4 → 20)")
	void remnants_restoresDefensePenalty() {
		// Arrange
		MatchState state = startedCombat(plain("A", 3, 20), plain("B", 5, 40));
		state.challenger().setHidden(new SetHidden(REMNANTS, 1));

		// Act
		Penalties.defense(state, self(state), 4, Side.OPPONENT, null);
		deaths.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(state.challenger().hiddenZone()).isEmpty();
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.HIDDEN_TRIGGERED);
			assertThat(event.data()).containsEntry("effect", "cancels 4 penalty points on A (ATK 3, DEF 20)");
		});
	}

	@Test
	@DisplayName("Remnants of Reality: annulla anche una penalità letale (3 − 5 = −2 → 3), la creatura non muore")
	void remnants_preventsLethalPenalty() {
		// Arrange
		MatchState state = startedCombat(plain("A", 3, 3), plain("B", 5, 40));
		state.challenger().setHidden(new SetHidden(REMNANTS, 1));

		// Act
		Penalties.defense(state, self(state), 5, Side.OPPONENT, null);
		boolean ended = deaths.resolve(state);

		// Assert
		assertThat(ended).isFalse();
		assertThat(self(state).defense()).isEqualTo(3);
	}

	@Test
	@DisplayName("Remnants of Reality: 5 punti, prima la penalità che l'ha attivata, poi i malus già presenti")
	void remnants_triggerFirstThenExistingMaluses() {
		// Arrange: A 3 con −2 e −3 ATK già presenti (riferiti in fasi precedenti)
		MatchState state = startedCombat(plain("A", 3, 20), plain("B", 5, 40));
		CreatureInPlay a = self(state);
		a.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -2, ModifierDuration.whileInPlay()));
		a.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.OPPONENT, -3, ModifierDuration.whileInPlay()));
		a.takeUnreportedPenalties();
		state.challenger().setHidden(new SetHidden(REMNANTS, 1));

		// Act: 4 di penalità DEF, poi resta 1 punto per il −2
		Penalties.defense(state, a, 4, Side.OPPONENT, null);
		deaths.resolve(state);

		// Assert: DEF 20, ATK 3 − 1 − 3 = −1
		assertThat(a.defense()).isEqualTo(20);
		assertThat(a.attack()).isEqualTo(-1);
	}

	@Test
	@DisplayName("Remnants of Reality: un malus ATK avversario si toglie (5 − 3 → 5)")
	void remnants_cancelsAttackPenalty() {
		// Arrange
		MatchState state = startedCombat(plain("A", 5, 20), plain("B", 5, 40));
		state.challenger().setHidden(new SetHidden(REMNANTS, 1));

		// Act
		self(state).addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, -3,
				ModifierDuration.rounds(2)));
		deaths.resolve(state);

		// Assert
		assertThat(self(state).attack()).isEqualTo(5);
		assertThat(self(state).modifiers()).isEmpty();
	}

	@Test
	@DisplayName("Remnants of Reality: non scatta per un attacco, per un malus del proprio lato né a inizio scontro")
	void remnants_notTriggered() {
		// Arrange: coperta prima dell'inizio dello scontro, pronta dal primo
		MatchState state = TestStates.inCombat(registry, plain("A", 3, 20), plain("B", 5, 40));
		state.challenger().setHidden(new SetHidden(REMNANTS, 1));

		// Act: inizio dello scontro, un round (attacco di B), un malus del proprietario
		resolver.startCombat(state);
		resolver.resolve(state);
		self(state).addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, -1,
				ModifierDuration.whileInPlay()));
		deaths.resolve(state);

		// Assert
		assertThat(state.challenger().hiddenZone()).hasSize(1);
	}

	@Test
	@DisplayName("Remnants of Reality: dalla pescata iniziale non scatta nel primo scontro (D-15)")
	void remnants_initialDraw_notInFirstCombat() {
		// Arrange
		MatchState state = startedCombat(plain("A", 3, 20), plain("B", 5, 40));
		state.challenger().setHidden(new SetHidden(REMNANTS, 2));

		// Act
		Penalties.defense(state, self(state), 4, Side.OPPONENT, null);
		deaths.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(16);
		assertThat(state.challenger().hiddenZone()).hasSize(1);
	}

	@Test
	@DisplayName("Remnants of Reality: il Campanello del Torii avversario la annulla e la penalità resta")
	void remnants_toriiBell_cancels() {
		// Arrange
		MatchState state = startedCombat(plain("A", 3, 20), plain("B", 5, 40));
		state.challenger().setHidden(new SetHidden(REMNANTS, 1));
		state.opponent().placeItem(TestCards.item(120, "Campanello del Torii", TestFactions.DREAM, "TORII_BELL"));

		// Act
		Penalties.defense(state, self(state), 4, Side.OPPONENT, null);
		deaths.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(16);
		assertThat(state.challenger().hiddenZone()).isEmpty();
	}

	// ---- Unseen Exit ----

	@Test
	@DisplayName("Unseen Exit: un attacco che ucciderebbe fa 5 danni in meno (8 → 3 su DEF 5)")
	void unseenExit_preventsFive() {
		// Arrange
		MatchState state = startedCombat(plain("A", 1, 5), plain("B", 8, 40));
		state.challenger().setHidden(new SetHidden(UNSEEN_EXIT, 1));

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(2);
		assertThat(state.challenger().hiddenZone()).isEmpty();
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.data())
				.containsEntry("effect", "prevents 5 damage to A"));
	}

	@Test
	@DisplayName("Unseen Exit: un attacco non letale non la attiva")
	void unseenExit_nonLethal_stays() {
		// Arrange
		MatchState state = startedCombat(plain("A", 1, 5), plain("B", 4, 40));
		state.challenger().setHidden(new SetHidden(UNSEEN_EXIT, 1));

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(1);
		assertThat(state.challenger().hiddenZone()).hasSize(1);
	}

	@Test
	@DisplayName("Unseen Exit: si attiva anche se 5 non bastano (12 → 7 su DEF 5, muore)")
	void unseenExit_notEnough_stillUsed() {
		// Arrange
		MatchState state = startedCombat(plain("A", 1, 5), plain("B", 12, 40));
		state.challenger().setHidden(new SetHidden(UNSEEN_EXIT, 1));

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(state.challenger().hiddenZone()).isEmpty();
	}

	// ---- The Undying Blossom ----

	@Test
	@DisplayName("The Undying Blossom: al danno letale resta con 4 DEF, perde i bonus a durata, l'avv. subisce 3 danni")
	void undyingBlossom_survivesOnce() {
		// Arrange: +2 ATK per tre round, scudo di 3 fino a fine scontro, +1 ATK da Spell
		MatchState state = startedCombat(blossom(2, 10), plain("B", 20, 40));
		CreatureInPlay blossom = self(state);
		blossom.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, 2, ModifierDuration.rounds(3)));
		blossom.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.CHALLENGER, 3,
				ModifierDuration.untilCombatEnd()));
		blossom.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.CHALLENGER, 1,
				ModifierDuration.whileInPlay()));

		// Act
		resolver.resolve(state);

		// Assert: B 40 − 5 − 3 = 32
		assertThat(blossom.defense()).isEqualTo(4);
		assertThat(blossom.attack()).isEqualTo(3);
		assertThat(enemy(state).defense()).isEqualTo(32);
		assertThat(messages(state)).contains("The Undying Blossom: survives with 4 DEF and loses its temporary "
				+ "bonuses (ATK 3), deals 3 damage to B (DEF 32)");
		assertThat(messages(state)).doesNotContain("The Undying Blossom is destroyed");
	}

	@Test
	@DisplayName("The Undying Blossom: una volta sola, il secondo colpo letale la uccide")
	void undyingBlossom_onlyOnce() {
		// Arrange
		MatchState state = startedCombat(blossom(2, 10), plain("B", 20, 60));
		resolver.resolve(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
	}

	@Test
	@DisplayName("The Undying Blossom: in una morte doppia lei resta e l'altra muore")
	void undyingBlossom_doubleDeath_sheStays() {
		// Arrange: Blossom 30/10 e B 20/5 si uccidono a vicenda
		MatchState state = startedCombat(blossom(30, 10), plain("B", 20, 5));

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(4);
		assertThat(state.opponent().creature()).isEmpty();
	}

	@Test
	@DisplayName("The Undying Blossom: agisce prima del Badge di Uscita Negata, che resta (D-36)")
	void undyingBlossom_beforeBadge() {
		// Arrange
		MatchState state = startedCombat(blossom(2, 10), plain("B", 20, 40));
		state.challenger().placeItem(TestCards.item(117, "Badge di Uscita Negata", TestFactions.WOUND,
				"DENIED_EXIT_BADGE"));

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(4);
		assertThat(state.challenger().items()).hasSize(1);
	}

	private MatchState startedCombat(CardDefinition challenger, CardDefinition opponent) {
		MatchState state = TestStates.inCombat(registry, challenger, opponent);
		resolver.startCombat(state);
		return state;
	}

	private static CreatureInPlay self(MatchState state) {
		return state.challenger().creature().orElseThrow();
	}

	private static CreatureInPlay enemy(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}

	private static List<String> messages(MatchState state) {
		return state.log().events().stream().map(MatchEvent::message).toList();
	}

	private static CardDefinition blossom(int attack, int defense) {
		return TestCards.creature(0, "The Undying Blossom", TestFactions.DREAM, null, "Mythic", attack, defense,
				UndyingBlossomEffect.CODE);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
