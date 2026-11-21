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
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti {@code P2} con la guarigione e il momento "a ogni eliminazione" (ENG-34): The Bloodless Hound, Wendigo e la
 * Spell SCP-109. Si provano attraverso il {@link RoundResolver}.
 */
class HealEffectsTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- The Bloodless Hound ----

	@Test
	@DisplayName("The Bloodless Hound: recupera metà del danno inflitto, per difetto (20 − 3 = 17, 5 / 2 = 2 → 19)")
	void bloodlessHound_drainsHalf() {
		// Arrange
		MatchState state = inCombat(hound(5, 20), plain("B", 3, 30));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(19);
		assertThat(messages(state)).contains("The Bloodless Hound: recovers 2 DEF (DEF 19)");
	}

	@Test
	@DisplayName("The Bloodless Hound: non supera la DEF all'entrata (20 − 1 = 19, recupera solo 1 dei 4)")
	void bloodlessHound_cappedAtInitialDefense() {
		// Arrange
		MatchState state = inCombat(hound(9, 20), plain("B", 1, 40));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(20);
		assertThat(messages(state)).contains("The Bloodless Hound: recovers 1 DEF (DEF 20)");
	}

	@Test
	@DisplayName("The Bloodless Hound: un attacco che non fa danni non fa recuperare niente")
	void bloodlessHound_noDamage_noDrain() {
		// Arrange: ATK 0
		MatchState state = inCombat(hound(0, 20), plain("B", 3, 30));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(self(state).defense()).isEqualTo(17);
		assertThat(messages(state)).noneMatch(message -> message.contains("recovers"));
	}

	@Test
	@DisplayName("The Bloodless Hound: dopo un'eliminazione recupera altri 4 (20 − 8 = 12, +3, +4 = 19)")
	void bloodlessHound_killHeals() {
		// Arrange
		MatchState state = inCombat(hound(6, 20), plain("B", 8, 5));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(self(state).defense()).isEqualTo(19);
		assertThat(messages(state)).containsSubsequence("The Bloodless Hound: recovers 3 DEF (DEF 15)",
				"The Bloodless Hound: recovers 4 DEF after defeating B (DEF 19)");
	}

	@Test
	@DisplayName("The Bloodless Hound: in una morte doppia non recupera niente")
	void bloodlessHound_doubleDeath_noHeal() {
		// Arrange
		MatchState state = inCombat(hound(6, 5), plain("B", 8, 5));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(messages(state)).noneMatch(message -> message.contains("recovers"));
	}

	// ---- Wendigo ----

	@Test
	@DisplayName("Wendigo: a fine round 4 danni all'avversaria (40 − 1 − 4 = 35; ENG-42, erano 3)")
	void wendigo_roundEndDamage() {
		// Arrange
		MatchState state = inCombat(wendigo(1, 30), plain("B", 1, 40));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(35);
		assertThat(messages(state)).contains("Wendigo: deals 4 damage to B (DEF 35)");
	}

	@Test
	@DisplayName("Wendigo: un'eliminazione fa recuperare 5 DEF fino all'entrata e aggiunge 1 al danno di fine round")
	void wendigo_killHealsAndGrows() {
		// Arrange: Wendigo 10/30 uccide B 2/5 al round 1
		MatchState state = inCombat(wendigo(10, 30), plain("B", 2, 5));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Act: entra C 1/40
		state.opponent().placeCreature(new CreatureInPlay(plain("C", 1, 40), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: 30 − 2 = 28, +2 fino a 30; C 40 − 10 − 5 = 25
		assertThat(messages(state)).contains("Wendigo: recovers 2 DEF after defeating B (DEF 30)",
				"Wendigo: deals 5 damage to C (DEF 25)");
		assertThat(enemy(state).defense()).isEqualTo(25);
	}

	@Test
	@DisplayName("Wendigo: conta anche l'eliminazione fatta dal suo danno di fine round")
	void wendigo_killByOwnDrain_counts() {
		// Arrange: B 1/5 scende a 4 con l'attacco, poi a 0 a fine round
		MatchState state = inCombat(wendigo(1, 30), plain("B", 1, 5));
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(messages(state)).containsSubsequence("Wendigo: deals 4 damage to B (DEF 0)",
				"Wendigo: recovers 1 DEF after defeating B (DEF 30)");
	}

	@Test
	@DisplayName("Wendigo: il danno di fine round arriva al massimo a 7, anche dopo 4 eliminazioni")
	void wendigo_damageCappedAtSeven() {
		// Arrange: Wendigo 10/30 elimina quattro creature 0/1
		MatchState state = inCombat(wendigo(10, 30), plain("V1", 0, 1));
		resolver.startCombat(state);
		resolver.resolve(state);
		for (int victim = 2; victim <= 4; victim++) {
			state.opponent().placeCreature(new CreatureInPlay(plain("V" + victim, 0, 1), Side.OPPONENT));
			resolver.startCombat(state);
			resolver.resolve(state);
		}

		// Act
		state.opponent().placeCreature(new CreatureInPlay(plain("Last", 0, 40), Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert: 40 − 10 − 7 = 23
		assertThat(enemy(state).defense()).isEqualTo(23);
		assertThat(messages(state)).contains("Wendigo: deals 7 damage to Last (DEF 23)");
	}

	// ---- SCP-109 ----

	@Test
	@DisplayName("SCP-109: +2 DEF in attesa della prossima creatura, con il recupero collegato")
	void scp109_pendingWithAttachedEffect() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());

		// Act
		resolveScp109(state, Side.CHALLENGER);

		// Assert
		assertThat(state.challenger().pendingModifiers()).singleElement().satisfies(modifier -> {
			assertThat(modifier.shield()).isEqualTo(2);
			assertThat(modifier.source()).isEqualTo(ModifierSource.SPELL);
			assertThat(modifier.attached()).isNotNull();
		});
		assertThat(state.log().events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(EventType.SPELL_RESOLVED);
			assertThat(event.data()).containsEntry("effect", "+2 DEF, recovers 3 DEF once at half DEF");
		});
	}

	@Test
	@DisplayName("SCP-109: una volta, a metà DEF o meno, recupera 3 DEF (12 all'entrata, 12 − 7 = 5 → 8)")
	void scp109_healsOnceAtHalf() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 10), plain("B", 7, 40));
		resolveScp109(state, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act + Assert: al round 1 scende a 5 e recupera 3
		resolver.resolve(state);
		assertThat(self(state).defense()).isEqualTo(8);
		assertThat(messages(state)).contains("A: recovers 3 DEF from SCP-109 (DEF 8)");

		// Act + Assert: al round 2 scende a 1, ma il recupero è già usato
		resolver.resolve(state);
		assertThat(self(state).defense()).isEqualTo(1);
		assertThat(messages(state)).filteredOn(message -> message.contains("recovers")).hasSize(1);
	}

	@Test
	@DisplayName("SCP-109: una creatura uccisa dal colpo non recupera niente")
	void scp109_deadCreature_noHeal() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 10), plain("B", 20, 40));
		resolveScp109(state, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert
		assertThat(state.challenger().creature()).isEmpty();
		assertThat(messages(state)).noneMatch(message -> message.contains("recovers"));
	}

	private void resolveScp109(MatchState state, Side owner) {
		CardDefinition spell = TestCards.spell(96, "SCP-109", CardSubtype.SHIELD, Scp109Effect.CODE);
		registry.createSpell(Scp109Effect.CODE).resolve(new EffectContext(state, owner, spell));
	}

	private MatchState inCombat(CardDefinition challenger, CardDefinition opponent) {
		return TestStates.inCombat(registry, challenger, opponent);
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

	private static CardDefinition hound(int attack, int defense) {
		return TestCards.creature(0, "The Bloodless Hound", TestFactions.INVITATION, null, "Rare", attack, defense,
				BloodlessHoundEffect.CODE);
	}

	private static CardDefinition wendigo(int attack, int defense) {
		return TestCards.creature(0, "Wendigo", TestFactions.INVITATION, null, "Legendary", attack, defense,
				WendigoEffect.CODE);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
