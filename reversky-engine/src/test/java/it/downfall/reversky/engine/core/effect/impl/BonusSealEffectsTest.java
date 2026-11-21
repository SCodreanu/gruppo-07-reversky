package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
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
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti {@code P2} che sigillano o tolgono i bonus (ENG-35, D-35): L'Addetto alle Uscite, Deathmoths, SCP-3999 e la
 * Hidden Static Interference. Gli effetti di creatura si provano attraverso il {@link RoundResolver}.
 */
class BonusSealEffectsTest {

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);

	// ---- L'Addetto alle Uscite ----

	@Test
	@DisplayName("L'Addetto alle Uscite: sigilla 2 punti di bonus ATK, anche di quelli dopo (5 + 2 → 5, +3 → 8)")
	void exitClerk_sealsCurrentAndLaterBonuses() {
		// Arrange: B 5 con +2 ATK da una Spell
		MatchState state = inCombat(creature("Exit Clerk", "Uncommon", 1, 20, ExitClerkEffect.CODE),
				plain("B", 5, 40));
		bonus(enemy(state), 2);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(messages(state)).contains("Exit Clerk: seals 2 points of B's ATK bonuses (ATK 5)");
		bonus(enemy(state), 3);
		assertThat(enemy(state).attack()).isEqualTo(8);
	}

	@Test
	@DisplayName("L'Addetto alle Uscite: senza bonus, −1 ATK nel primo round e il sigillo vale per i bonus dopo")
	void exitClerk_noBonus_firstRoundPenaltyAndSeal() {
		// Arrange
		MatchState state = inCombat(creature("Exit Clerk", "Uncommon", 1, 20, ExitClerkEffect.CODE),
				plain("B", 5, 40));

		// Act + Assert: −1 ATK solo nel primo round
		resolver.startCombat(state);
		assertThat(enemy(state).attack()).isEqualTo(4);
		assertThat(messages(state)).contains(
				"Exit Clerk: seals 2 points of B's ATK bonuses (ATK 5), B −1 ATK in the first round (ATK 4)");
		resolver.resolve(state);
		assertThat(enemy(state).attack()).isEqualTo(5);

		// Act + Assert: un bonus di 2 ricevuto dopo è sigillato
		bonus(enemy(state), 2);
		assertThat(enemy(state).attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("L'Addetto alle Uscite: il sigillo resta sull'avversaria anche dopo la sua morte")
	void exitClerk_sealOutlivesTheClerk() {
		// Arrange: l'Addetto 1/1 muore al round 1
		MatchState state = inCombat(creature("Exit Clerk", "Uncommon", 1, 1, ExitClerkEffect.CODE),
				plain("B", 5, 40));
		bonus(enemy(state), 2);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);
		state.challenger().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.CHALLENGER));
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature()).map(CreatureInPlay::name).contains("C");
		assertThat(enemy(state).attack()).isEqualTo(5);
	}

	// ---- Deathmoths ----

	@Test
	@DisplayName("Deathmoths: avv. −2 ATK finché resta in campo e 2 punti di bonus sigillati (5 + 2 − 2 − 2 = 3)")
	void deathmoths_penaltyAndSeal() {
		// Arrange
		MatchState state = inCombat(creature("Deathmoths", "Mythic", 1, 30, DeathmothsEffect.CODE),
				plain("B", 5, 40));
		bonus(enemy(state), 2);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(3);
		assertThat(messages(state)).contains(
				"Deathmoths: seals 2 points of B's ATK bonuses (ATK 5), B −2 ATK while in play (ATK 3)");
	}

	// ---- SCP-3999 ----

	@Test
	@DisplayName("SCP-3999: all'entrata l'avv. perde bonus ATK e scudi, tiene malus e danni; SCP-3999 +1 ATK")
	void scp3999_entryRemovesBonuses() {
		// Arrange: B 5/10 con +2 ATK, scudo di 3 e −1 ATK
		MatchState state = inCombat(scp3999(1, 40), plain("B", 5, 10));
		CreatureInPlay b = enemy(state);
		bonus(b, 2);
		b.addModifier(StatModifier.shield(ModifierSource.SPELL, Side.OPPONENT, 3, ModifierDuration.whileInPlay()));
		b.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, -1,
				ModifierDuration.whileInPlay()));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(b.attack()).isEqualTo(4);
		assertThat(b.defense()).isEqualTo(10);
		assertThat(self(state).attack()).isEqualTo(2);
		assertThat(messages(state)).contains(
				"SCP-3999: B loses all ATK and DEF bonuses (ATK 4, DEF 10), +1 ATK (ATK 2)");
	}

	@Test
	@DisplayName("SCP-3999 (ENG-42): di nuovo in ogni round dal 2° in campo, +1 ATK ogni volta fino a +3")
	void scp3999_everyRoundFromTheSecond_capped() {
		// Arrange
		MatchState state = inCombat(scp3999(1, 60), plain("B", 1, 80));
		resolver.startCombat(state);

		// Act + Assert: round 1 niente
		resolver.resolve(state);
		assertThat(self(state).attack()).isEqualTo(2);

		// Act + Assert: round 2, il bonus ricevuto prima sparisce
		bonus(enemy(state), 3);
		resolver.resolve(state);
		assertThat(enemy(state).attack()).isEqualTo(1);
		assertThat(self(state).attack()).isEqualTo(3);

		// Act + Assert: round 3 di nuovo, +1; round 4 niente di più
		bonus(enemy(state), 2);
		resolver.resolve(state);
		assertThat(enemy(state).attack()).isEqualTo(1);
		assertThat(self(state).attack()).isEqualTo(4);
		resolver.resolve(state);
		assertThat(self(state).attack()).isEqualTo(4);
	}

	@Test
	@DisplayName("SCP-3999: il bonus del Terreno tolto non torna allo scontro dopo")
	void scp3999_terrainBonusGoneForGood() {
		// Arrange: Echo Rift dà +1 ATK a B (Theory of the Wound); SCP-3999 1/1 muore al round 1
		MatchState state = inCombat(scp3999(1, 1), plain("B", 5, 40));
		TestStates.placeTerrain(state, TestCards.terrain(90, "Echo Rift", TestFactions.WOUND, "ECHO_RIFT"),
				Side.OPPONENT);
		resolver.startCombat(state);
		assertThat(enemy(state).attack()).isEqualTo(5);
		resolver.resolve(state);

		// Act
		state.challenger().placeCreature(new CreatureInPlay(plain("C", 1, 30), Side.CHALLENGER));
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("SCP-3999: se il Fascicolo Sigillato 0 annulla l'attivazione, prende lo stesso +1 ATK")
	void scp3999_sealedFile_stillGainsAttack() {
		// Arrange
		MatchState state = inCombat(scp3999(1, 40), plain("B", 5, 40));
		bonus(enemy(state), 2);
		state.opponent().placeItem(TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
				"SEALED_FILE_ZERO"));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(7);
		assertThat(self(state).attack()).isEqualTo(2);
	}

	// ---- Static Interference ----

	@Test
	@DisplayName("Static Interference: con bonus, 5 punti sigillati per lo scontro, anche quelli dopo (5 + 3 → 5)")
	void staticInterference_sealsForTheCombat() {
		// Arrange
		MatchState state = withEnemy(plain("B", 5, 40));
		CreatureInPlay b = enemy(state);
		bonus(b, 3);

		// Act
		triggerStatic(state);

		// Assert: 3 + 4 = 7 punti di bonus, 5 sigillati; a fine scontro tornano tutti
		assertThat(b.attack()).isEqualTo(5);
		bonus(b, 4);
		assertThat(b.attack()).isEqualTo(7);
		b.endCombat();
		assertThat(b.attack()).isEqualTo(12);
		assertThat(state.log().events()).singleElement().satisfies(event -> assertThat(event.data())
				.containsEntry("effect", "seals 5 points of B's ATK bonuses this combat (ATK 5)"));
	}

	@Test
	@DisplayName("Static Interference: senza bonus, o con bonus già tutti sigillati, 5 danni (40 → 35)")
	void staticInterference_noEffectiveBonus_damage() {
		// Arrange: +2 ATK già sigillato da un altro sigillo
		MatchState state = withEnemy(plain("B", 5, 40));
		CreatureInPlay b = enemy(state);
		bonus(b, 2);
		b.addModifier(StatModifier.sealAttack(ModifierSource.EFFECT, Side.CHALLENGER, 2,
				ModifierDuration.whileInPlay()));

		// Act
		triggerStatic(state);

		// Assert
		assertThat(b.defense()).isEqualTo(35);
		assertThat(state.log().events()).singleElement()
				.satisfies(event -> assertThat(event.data()).containsEntry("effect", "−5 DEF to B"));
	}

	@Test
	@DisplayName("Static Interference: coperta dallo sfidante, si attiva a inizio scontro e sigilla il +2 di B")
	void staticInterference_triggersAtCombatStart() {
		// Arrange: B 5 con +2 ATK; Static Interference coperta dallo sfidante, pronta per questo scontro
		MatchState state = inCombat(plain("A", 1, 30), plain("B", 5, 40));
		bonus(enemy(state), 2);
		state.challenger().setHidden(new SetHidden(TestCards.hidden(97, "Static Interference", CardSubtype.DAMAGE,
				StaticInterferenceEffect.CODE), 1));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(state.log().events()).anySatisfy(event -> assertThat(event.data())
				.containsEntry("effect", "seals 5 points of B's ATK bonuses this combat (ATK 5)"));
	}

	private void triggerStatic(MatchState state) {
		CardDefinition hidden = TestCards.hidden(97, "Static Interference", CardSubtype.DAMAGE,
				StaticInterferenceEffect.CODE);
		registry.createHidden(StaticInterferenceEffect.CODE).trigger(new EffectContext(state, Side.CHALLENGER,
				hidden));
	}

	private static MatchState withEnemy(CardDefinition card) {
		MatchState state = TestStates.withDecks(List.of(), List.of());
		state.opponent().placeCreature(new CreatureInPlay(card, Side.OPPONENT));
		return state;
	}

	private static void bonus(CreatureInPlay creature, int amount) {
		creature.addModifier(StatModifier.attack(ModifierSource.SPELL, creature.side(), amount,
				ModifierDuration.whileInPlay()));
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

	private static CardDefinition scp3999(int attack, int defense) {
		return TestCards.creature(0, "SCP-3999", TestFactions.ARCHIVE, null, "Legendary", attack, defense,
				Scp3999Effect.CODE);
	}

	private static CardDefinition creature(String name, String rarity, int attack, int defense, String code) {
		return TestCards.creature(0, name, TestFactions.WOUND, null, rarity, attack, defense, code);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
