package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * La Spell SCP-500 · Pillola di Emergenza (ENG-40): la prossima creatura subisce 5 danni in meno dal primo danno di
 * qualunque tipo. Si prova attraverso il {@link RoundResolver}.
 */
class Scp500EffectTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	@Test
	@DisplayName("SCP-500: una protezione di 5 dal primo danno in attesa della prossima creatura")
	void scp500_pendingWard() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());

		// Act
		resolveScp500(state, Side.CHALLENGER);

		// Assert
		assertThat(state.challenger().pendingModifiers()).singleElement().satisfies(modifier -> {
			assertThat(modifier.ward()).isEqualTo(5);
			assertThat(modifier.source()).isEqualTo(ModifierSource.SPELL);
			assertThat(modifier.attack()).isZero();
			assertThat(modifier.shield()).isZero();
		});
		assertThat(state.log().events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(EventType.SPELL_RESOLVED);
			assertThat(event.data()).containsEntry("effect", "−5 to the first damage");
		});
	}

	@Test
	@DisplayName("SCP-500: il primo attacco perde 5 danni (7 → 2), il secondo arriva intero")
	void scp500_firstAttackOnly() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 10), plain("B", 7, 40));
		resolveScp500(state, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act + Assert: al round 1 subisce 2
		resolver.resolve(state);
		assertThat(self(state).defense()).isEqualTo(8);
		assertThat(self(state).damageWard()).isZero();
		assertThat(messages(state)).contains("A: prevents 5 of the first damage with SCP-500");

		// Act + Assert: al round 2 subisce 7
		resolver.resolve(state);
		assertThat(self(state).defense()).isEqualTo(1);
		assertThat(messages(state)).filteredOn(message -> message.contains("prevents")).hasSize(1);
	}

	@Test
	@DisplayName("SCP-500: un colpo più piccolo della protezione la usa tutta (3 → 0)")
	void scp500_smallHitUsesWholeWard() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 10), plain("B", 3, 40));
		resolveScp500(state, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(7);
		assertThat(messages(state)).contains("A: prevents 3 of the first damage with SCP-500");
	}

	@Test
	@DisplayName("SCP-500: la riduzione viene prima del dimezzamento di SCP-3000 ((9 − 5) / 2 = 2)")
	void scp500_beforeScp3000Halving() {
		// Arrange
		CardDefinition scp3000 = TestCards.creature(0, "SCP-3000", TestFactions.ARCHIVE, null, "Legendary", 1, 20,
				Scp3000Effect.CODE);
		CardDefinition rare = TestCards.creature(0, "Rare", TestFactions.WOUND, null, "Rare", 9, 40, null);
		MatchState state = inCombat(scp3000, rare);
		resolveScp500(state, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(18);
		assertThat(messages(state)).contains("SCP-3000: halves the damage from Rare (4 → 2)");
	}

	@Test
	@DisplayName("SCP-500: all'entrata protegge anche da un danno avversario in attesa arrivato prima della Spell")
	void scp500_wardFirstOnEntry() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 10), plain("B", 7, 40));
		state.challenger().addPendingModifier(new PendingModifier("Bite", ModifierSource.SPELL, Side.OPPONENT, 0, 0,
				3));
		resolveScp500(state, Side.CHALLENGER);

		// Act
		resolver.startCombat(state);

		// Assert: il danno di entrata è il primo danno, l'attacco del round 1 arriva intero
		assertThat(self(state).defense()).isEqualTo(10);
		assertThat(messages(state)).contains("A: prevents 3 of the first damage with SCP-500");
		resolver.resolve(state);
		assertThat(self(state).defense()).isEqualTo(3);
	}

	@Test
	@DisplayName("SCP-500: una penalità che il Terreno porta a 0 non usa la protezione (Hollow Clearing, 1 → 0)")
	void scp500_terrainFirst_zeroPenaltyKeepsWard() {
		// Arrange
		MatchState state = inCombat(plain("A", TestFactions.INVITATION, 1, 10), plain("B", 7, 40));
		TestStates.placeTerrain(state, TestCards.terrain(110, "Hollow Clearing", TestFactions.INVITATION,
				HollowClearingEffect.CODE), Side.CHALLENGER);
		state.challenger().addPendingModifier(new PendingModifier("Bite", ModifierSource.SPELL, Side.OPPONENT, 0, 0,
				1));
		resolveScp500(state, Side.CHALLENGER);
		resolver.startCombat(state);
		assertThat(self(state).damageWard()).isEqualTo(5);

		// Act
		resolver.resolve(state);

		// Assert: 10 + 2 del Terreno, poi 7 − 5 = 2
		assertThat(self(state).defense()).isEqualTo(10);
		assertThat(self(state).damageWard()).isZero();
	}

	@Test
	@DisplayName("SCP-500: due Spell sulla stessa creatura si sommano sul primo danno (12 − 10 = 2)")
	void scp500_twoWardsSum() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 20), plain("B", 12, 40));
		resolveScp500(state, Side.CHALLENGER);
		resolveScp500(state, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(18);
		assertThat(messages(state)).contains("A: prevents 10 of the first damage with SCP-500");
	}

	@Test
	@DisplayName("SCP-500: la protezione non è un effetto della creatura, Il Predicatore sotto la Pioggia non la tocca")
	void scp500_notCancelledByPreacher() {
		// Arrange
		CardDefinition kappa = TestCards.creature(0, "Kappa", TestFactions.WOUND, null, "Common", 1, 10, "KAPPA");
		CardDefinition preacher = TestCards.creature(0, "The Rain Preacher", TestFactions.WOUND, null, "Rare", 7, 40,
				RainPreacherEffect.CODE);
		MatchState state = inCombat(kappa, preacher);
		resolveScp500(state, Side.CHALLENGER);
		resolver.startCombat(state);
		assertThat(self(state).isEffectCancelled()).isTrue();

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(messages(state)).contains("Kappa: prevents 5 of the first damage with SCP-500");
	}

	private void resolveScp500(MatchState state, Side owner) {
		CardDefinition spell = TestCards.spell(100, "SCP-500", CardSubtype.SHIELD, Scp500Effect.CODE);
		registry.createSpell(Scp500Effect.CODE).resolve(new EffectContext(state, owner, spell));
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
	}

	private static CreatureInPlay self(MatchState state) {
		return state.challenger().creature().orElseThrow();
	}

	private static List<String> messages(MatchState state) {
		return state.log().events().stream().map(MatchEvent::message).toList();
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return plain(name, TestFactions.WOUND, attack, defense);
	}

	private static CardDefinition plain(String name, int faction, int attack, int defense) {
		return TestCards.creature(name, faction, attack, defense);
	}
}
