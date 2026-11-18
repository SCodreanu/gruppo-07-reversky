package it.downfall.reversky.engine.core.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link TerrainService}: il bonus base dei Terreni (D-17), la sostituzione (D-19) e lo scudo del Terreno
 * (D-35). Crawlers dello sfidante è Wound, Kappa dell'avversario è Dream.
 */
class TerrainServiceTest {

	private static final CardDefinition ECHO_RIFT = terrain(113, "Echo Rift", TestFactions.WOUND, "ECHO_RIFT");
	private static final CardDefinition HOLLOW_CLEARING = terrain(114, "Hollow Clearing", TestFactions.INVITATION,
			"HOLLOW_CLEARING");
	private static final CardDefinition DEVOURING_ABYSS = terrain(116, "Devouring Abyss", TestFactions.DREAM,
			"DEVOURING_ABYSS");

	private final TerrainService terrains = new TerrainService(EffectRegistry.withAllEffects());

	@ParameterizedTest(name = "{0}")
	@CsvSource({ "ECHO_RIFT, 1, 0", "HOLLOW_CLEARING, 0, 2", "WATCHING_RIFT, 1, 1", "DEVOURING_ABYSS, 1, 0" })
	@DisplayName("I quattro Terreni del gioco e il loro bonus base")
	void registry_baseBonus(String code, int attack, int defense) {
		EffectRegistry registry = EffectRegistry.withAllEffects();

		assertThat(registry.createTerrain(code).attackBonus()).isEqualTo(attack);
		assertThat(registry.createTerrain(code).defenseBonus()).isEqualTo(defense);
	}

	@Test
	@DisplayName("D-17: il bonus va a tutte le creature in campo della fazione, anche a quella dell'avversario")
	void place_bonusToEveryCreatureOfTheFaction() {
		// Arrange: due creature Wound, una per parte; il Terreno lo pesca l'avversario
		MatchState state = TestStates.inCombat(creature("Crawlers", TestFactions.WOUND, 4, 5),
				creature("Faceling", TestFactions.WOUND, 7, 9));

		// Act
		terrains.place(state, state.opponent(), ECHO_RIFT);

		// Assert
		assertThat(challenger(state).attack()).isEqualTo(5);
		assertThat(opponent(state).attack()).isEqualTo(8);
		assertThat(challenger(state).modifiers()).singleElement().satisfies(modifier -> {
			assertThat(modifier.source()).isEqualTo(ModifierSource.TERRAIN);
			assertThat(modifier.side()).isEqualTo(Side.OPPONENT);
		});
		assertThat(state.terrain()).hasValueSatisfying(terrain -> {
			assertThat(terrain.card()).isEqualTo(ECHO_RIFT);
			assertThat(terrain.owner()).isEqualTo(Side.OPPONENT);
		});
		assertThat(state.log().events()).extracting(MatchEvent::message).containsExactly(
				"Echo Rift enters play",
				"Crawlers: +1 ATK from Echo Rift (ATK 5, DEF 5)",
				"Faceling: +1 ATK from Echo Rift (ATK 8, DEF 9)");
	}

	@Test
	@DisplayName("Una creatura di un'altra fazione non riceve niente")
	void place_otherFaction_noBonus() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("Crawlers", TestFactions.WOUND, 4, 5),
				creature("Kappa", TestFactions.DREAM, 6, 7));

		// Act
		terrains.place(state, state.challenger(), ECHO_RIFT);

		// Assert
		assertThat(opponent(state).attack()).isEqualTo(6);
		assertThat(opponent(state).modifiers()).isEmpty();
	}

	@Test
	@DisplayName("D-19: il nuovo Terreno sostituisce il vecchio, che toglie l'ATK e la parte non usata dello scudo")
	void place_replaces_oldBonusRemoved() {
		// Arrange: Kappa (Dream) con Devouring Abyss; Squonk (Invitation) con Hollow Clearing, scudo usato a metà
		MatchState state = TestStates.inCombat(creature("Squonk", TestFactions.INVITATION, 3, 2),
				creature("Kappa", TestFactions.DREAM, 6, 7));
		terrains.place(state, state.challenger(), HOLLOW_CLEARING);
		challenger(state).takeDamage(3);

		// Act
		terrains.place(state, state.opponent(), DEVOURING_ABYSS);

		// Assert: Squonk aveva 2 + 2 DEF, ne ha persi 3 (2 dallo scudo): resta a 1, l'uscita non la uccide
		assertThat(challenger(state).defense()).isEqualTo(1);
		assertThat(challenger(state).isDead()).isFalse();
		assertThat(opponent(state).attack()).isEqualTo(7);
		assertThat(state.log().events()).extracting(MatchEvent::message).endsWith(
				"Devouring Abyss enters play, replacing Hollow Clearing",
				"Squonk: loses the Hollow Clearing bonus (ATK 3, DEF 1)",
				"Kappa: +1 ATK from Devouring Abyss (ATK 7, DEF 7)");
	}

	@Test
	@DisplayName("Il bonus si riceve una volta per Terreno: uno scudo consumato non torna allo scontro dopo")
	void applyBonus_oncePerTerrain() {
		// Arrange
		MatchState state = TestStates.inCombat(creature("Squonk", TestFactions.INVITATION, 3, 6),
				creature("Kappa", TestFactions.DREAM, 6, 7));
		terrains.place(state, state.challenger(), HOLLOW_CLEARING);
		challenger(state).takeDamage(2);

		// Act
		terrains.applyBonus(state, challenger(state));

		// Assert
		assertThat(challenger(state).defense()).isEqualTo(6);
		assertThat(state.log().events()).filteredOn(event -> event.type() == EventType.EFFECT_TRIGGERED).hasSize(1);
	}

	@Test
	@DisplayName("Una creatura con due fazioni riceve il bonus una volta sola")
	void applyBonus_twoFactions_once() {
		// Arrange: The Rootbound Doppelganger, Invitation e Dream
		MatchState state = TestStates.inCombat(
				TestCards.creature("The Rootbound Doppelganger", TestFactions.INVITATION, TestFactions.DREAM, 8, 9),
				creature("Kappa", TestFactions.DREAM, 6, 7));

		// Act
		terrains.place(state, state.challenger(), DEVOURING_ABYSS);
		terrains.applyBonus(state, challenger(state));

		// Assert
		assertThat(challenger(state).attack()).isEqualTo(9);
	}

	@Test
	@DisplayName("Lo scudo del Terreno assorbe prima di quello di una Spell (deciso il 17 novembre)")
	void terrainShield_absorbsBeforeSpellShield() {
		// Arrange: Squonk ha già 5 DEF di Veil of the Anchored, poi arriva Hollow Clearing (+2)
		MatchState state = TestStates.inCombat(creature("Squonk", TestFactions.INVITATION, 3, 6),
				creature("Kappa", TestFactions.DREAM, 6, 7));
		challenger(state).addModifier(StatModifier.shield(ModifierSource.SPELL, Side.CHALLENGER, 5,
				ModifierDuration.whileInPlay()));
		terrains.place(state, state.challenger(), HOLLOW_CLEARING);

		// Act: 3 danni, poi il Terreno esce
		challenger(state).takeDamage(3);
		challenger(state).loseTerrainBonus();

		// Assert: 2 dallo scudo del Terreno, 1 da quello della Spell: restano 6 + 4
		assertThat(challenger(state).defense()).isEqualTo(10);
	}

	@Test
	@DisplayName("Senza Terreno in gioco applyBonus non fa niente")
	void applyBonus_noTerrain_nothing() {
		MatchState state = TestStates.inCombat(creature("Crawlers", TestFactions.WOUND, 4, 5),
				creature("Kappa", TestFactions.DREAM, 6, 7));

		terrains.applyBonus(state, challenger(state));

		assertThat(challenger(state).modifiers()).isEmpty();
		assertThat(state.log().events()).isEmpty();
	}

	private static CardDefinition terrain(int cardId, String name, int factionId, String code) {
		return TestCards.terrain(cardId, name, factionId, code);
	}

	private static CardDefinition creature(String name, int factionId, int attack, int defense) {
		return TestCards.creature(name, factionId, attack, defense);
	}

	private static CreatureInPlay challenger(MatchState state) {
		return state.challenger().creature().orElseThrow();
	}

	private static CreatureInPlay opponent(MatchState state) {
		return state.opponent().creature().orElseThrow();
	}
}
