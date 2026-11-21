package it.downfall.reversky.engine.core.effect.impl;

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
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.core.simulation.TerrainService;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti {@code P2} che tolgono il Terreno in gioco (ENG-36, D-19, D-38): The Null Witness e The Hollow Wyrm. Si
 * provano attraverso il {@link RoundResolver} e, per un Terreno pescato con il Wyrm in campo, con il
 * {@link TerrainService}.
 */
class TerrainRemovalEffectsTest {

	private static final CardDefinition ECHO_RIFT = TestCards.terrain(113, "Echo Rift", TestFactions.WOUND,
			"ECHO_RIFT");
	private static final CardDefinition WATCHING_RIFT = TestCards.terrain(115, "Watching Rift", TestFactions.ARCHIVE,
			"WATCHING_RIFT");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);
	private final TerrainService terrains = new TerrainService(registry);

	// ---- The Null Witness ----

	@Test
	@DisplayName("The Null Witness: prima 4 danni (lo scudo ne prende 2), poi l'avv. perde i modificatori a durata")
	void nullWitness_damageThenTemporaryModifiers() {
		// Arrange: B 5/20 con scudo di 2 e −1 ATK fino a fine scontro, +2 ATK per due round, +3 ATK da Spell
		MatchState state = inCombat(nullWitness(), plain("B", 5, 20));
		CreatureInPlay b = enemy(state);
		b.addModifier(StatModifier.shield(ModifierSource.HIDDEN, Side.OPPONENT, 2, ModifierDuration.untilCombatEnd()));
		b.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.CHALLENGER, -1,
				ModifierDuration.untilCombatEnd()));
		b.addModifier(StatModifier.attack(ModifierSource.EFFECT, Side.OPPONENT, 2, ModifierDuration.rounds(2)));
		b.addModifier(StatModifier.attack(ModifierSource.SPELL, Side.OPPONENT, 3, ModifierDuration.whileInPlay()));

		// Act
		resolver.startCombat(state);

		// Assert: 20 − 2 = 18; resta solo il +3 della Spell
		assertThat(b.defense()).isEqualTo(18);
		assertThat(b.attack()).isEqualTo(8);
		assertThat(messages(state)).contains(
				"The Null Witness: deals 4 damage to B (DEF 18), B loses its temporary modifiers (ATK 8, DEF 18)");
	}

	@Test
	@DisplayName("The Null Witness: toglie il Terreno pescato dall'avversario, con i suoi bonus")
	void nullWitness_removesEnemyTerrain() {
		// Arrange: Echo Rift dell'avversario dà +1 ATK a B
		MatchState state = inCombat(nullWitness(), plain("B", 5, 40));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.OPPONENT);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.terrain()).isEmpty();
		assertThat(enemy(state).attack()).isEqualTo(5);
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.TERRAIN_REMOVED);
			assertThat(event.side()).isEqualTo(Side.CHALLENGER);
			assertThat(event.message()).isEqualTo("Echo Rift is removed by The Null Witness");
		});
		assertThat(messages(state)).containsSubsequence("Echo Rift is removed by The Null Witness",
				"B: loses the Echo Rift bonus (ATK 5, DEF 36)");
	}

	@Test
	@DisplayName("The Null Witness: il Terreno pescato dal suo proprietario resta")
	void nullWitness_ownTerrainStays() {
		// Arrange
		MatchState state = inCombat(nullWitness(), plain("B", 5, 40));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);

		// Act
		resolver.startCombat(state);

		// Assert: 5 + 1 del bonus base + 1 di Echo Rift per la ferita dei 4 danni
		assertThat(state.terrain()).isPresent();
		assertThat(enemy(state).attack()).isEqualTo(7);
	}

	@Test
	@DisplayName("The Null Witness: il Fascicolo Sigillato 0 salva la creatura, non il Terreno")
	void nullWitness_sealedFile_terrainStillRemoved() {
		// Arrange
		MatchState state = inCombat(nullWitness(), plain("B", 5, 40));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.OPPONENT);
		state.opponent().placeItem(TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
				"SEALED_FILE_ZERO"));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(40);
		assertThat(state.terrain()).isEmpty();
	}

	// ---- The Hollow Wyrm ----

	@Test
	@DisplayName("The Hollow Wyrm: all'entrata inghiotte il Terreno in gioco, anche il suo, e prende +3 ATK e +3 DEF")
	void hollowWyrm_swallowsTerrainOnEntry() {
		// Arrange: Echo Rift del Wyrm, che è della Theory of the Wound
		MatchState state = inCombat(hollowWyrm(), plain("B", 5, 40));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);

		// Act
		resolver.startCombat(state);

		// Assert: 4 + 3 = 7, 5 + 3 = 8; niente danni a B
		assertThat(state.terrain()).isEmpty();
		assertThat(self(state).attack()).isEqualTo(7);
		assertThat(self(state).defense()).isEqualTo(8);
		assertThat(enemy(state).defense()).isEqualTo(40);
		assertThat(messages(state)).containsSubsequence("Echo Rift is removed by The Hollow Wyrm",
				"The Hollow Wyrm: +3 ATK (ATK 7), +3 DEF (DEF 8)");
	}

	@Test
	@DisplayName("The Hollow Wyrm: senza Terreno all'entrata, 4 danni all'avversaria")
	void hollowWyrm_noTerrain_damage() {
		// Arrange
		MatchState state = inCombat(hollowWyrm(), plain("B", 5, 40));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(enemy(state).defense()).isEqualTo(36);
		assertThat(self(state).attack()).isEqualTo(4);
	}

	@Test
	@DisplayName("The Hollow Wyrm: un Terreno pescato mentre è in campo viene inghiottito e non si piazza")
	void hollowWyrm_swallowsArrivingTerrain() {
		// Arrange: entra senza Terreno
		MatchState state = inCombat(hollowWyrm(), plain("B", 5, 40));
		resolver.startCombat(state);

		// Act: l'avversario pesca Watching Rift
		terrains.place(state, state.opponent(), WATCHING_RIFT);

		// Assert
		assertThat(state.terrain()).isEmpty();
		assertThat(self(state).attack()).isEqualTo(7);
		assertThat(self(state).defense()).isEqualTo(8);
		assertThat(state.log().events()).extracting(MatchEvent::type).doesNotContain(EventType.TERRAIN_PLACED);
		assertThat(messages(state)).contains("Watching Rift is removed by The Hollow Wyrm");
	}

	@Test
	@DisplayName("The Hollow Wyrm: ogni Terreno inghiottito dà +3/+3 (7/8, poi 10/11)")
	void hollowWyrm_bonusesStack() {
		// Arrange
		MatchState state = inCombat(hollowWyrm(), plain("B", 5, 40));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.OPPONENT);
		resolver.startCombat(state);

		// Act
		terrains.place(state, state.opponent(), WATCHING_RIFT);

		// Assert
		assertThat(self(state).attack()).isEqualTo(10);
		assertThat(self(state).defense()).isEqualTo(11);
	}

	@Test
	@DisplayName("Senza Wyrm in campo il Terreno pescato si piazza come sempre")
	void noWyrm_terrainPlaced() {
		// Arrange
		MatchState state = inCombat(plain("A", 1, 20), plain("B", 5, 40));
		resolver.startCombat(state);

		// Act
		terrains.place(state, state.opponent(), WATCHING_RIFT);

		// Assert
		assertThat(state.terrain()).isPresent();
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

	private static CardDefinition nullWitness() {
		return TestCards.creature(0, "The Null Witness", TestFactions.ARCHIVE, null, "Mythic", 4, 8,
				NullWitnessEffect.CODE);
	}

	private static CardDefinition hollowWyrm() {
		return TestCards.creature(0, "The Hollow Wyrm", TestFactions.WOUND, null, "Legendary", 4, 5,
				HollowWyrmEffect.CODE);
	}

	private static CardDefinition plain(String name, int attack, int defense) {
		return TestCards.creature(name, TestFactions.WOUND, attack, defense);
	}
}
