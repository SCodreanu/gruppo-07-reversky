package it.downfall.reversky.engine.core.simulation;

import static it.downfall.reversky.engine.support.TestFactions.ARCHIVE;
import static it.downfall.reversky.engine.support.TestFactions.DREAM;
import static it.downfall.reversky.engine.support.TestFactions.INVITATION;
import static it.downfall.reversky.engine.support.TestFactions.WOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.PlayerResult;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.validation.ErrorCode;
import it.downfall.reversky.engine.core.validation.InvalidSimulationInputException;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestDecks;
import it.downfall.reversky.engine.support.TestEffects;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestInputs;
import it.downfall.reversky.engine.support.TestRules;

/**
 * Test di {@link MatchSimulator}: partite complete con il risultato calcolato a mano negli esempi 01–08 e 10–12 di
 * {@code examples/} (stesse carte, stessi id), determinismo e carte non creatura pescate prima di ENG-19 (con effetti
 * finti, che non fanno niente).
 */
class MatchSimulatorTest {

	private final MatchSimulator simulator = new MatchSimulator(EffectRegistry.withAllEffects());

	@Test
	@DisplayName("Stesso input e stesso seed producono la stessa identica partita, log compreso")
	void simulate_sameSeed_sameResult() {
		// Arrange: mazzi completi del catalogo, anche con Spell, Hidden, Terreno e Oggetto
		SimulationInput input = TestInputs.of(12345L, TestDecks.full(WOUND), TestDecks.full(DREAM));

		MatchSimulator withStubs = new MatchSimulator(TestEffects.stubsFor(input));

		// Act
		MatchResult first = withStubs.simulate(input);
		MatchResult second = withStubs.simulate(input);

		// Assert
		assertThat(second).isEqualTo(first);
	}

	@Test
	@DisplayName("Esempio 08 con seed 43 invece di 42: PV finali 27–23, la partita cambia")
	void simulate_differentSeed_differentMatch() {
		// Act
		MatchResult seed42 = simulator.simulate(example08(42L));
		MatchResult seed43 = simulator.simulate(example08(43L));

		// Assert
		assertThat(seed43.log()).isNotEqualTo(seed42.log());
		assertThat(seed43.challenger().finalLifePoints()).isEqualTo(27);
		assertThat(seed43.opponent().finalLifePoints()).isEqualTo(23);
	}

	@Test
	@DisplayName("Esempio 01: CHALLENGER_WIN, NO_CREATURES_LEFT, 1 round, PV 30–27, con il log minimo atteso")
	void simulate_example01() {
		// Act
		MatchResult result = simulator.simulate(TestInputs.of(1L,
				List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5))));

		// Assert
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1, 30, 27, 1, 0);
		assertThat(result.log()).extracting(MatchEvent::type).containsExactly(
				EventType.MATCH_STARTED, EventType.CREATURE_ENTERED, EventType.CREATURE_ENTERED,
				EventType.ROUND_STARTED, EventType.ATTACK_COMPUTED, EventType.ATTACK_COMPUTED,
				EventType.DAMAGE_DEALT, EventType.DAMAGE_DEALT, EventType.CREATURE_DIED,
				EventType.PLAYER_DAMAGED, EventType.MATCH_ENDED);
		assertThat(result.seed()).isEqualTo(1L);
		assertThat(result.rulesVersion()).isEqualTo(TestRules.VERSION);
		assertThat(result.challenger().userId()).isEqualTo(TestInputs.CHALLENGER_USER_ID);
	}

	@Test
	@DisplayName("Esempio 02: CHALLENGER_WIN, NO_CREATURES_LEFT, 3 round, PV 30–28")
	void simulate_example02() {
		MatchResult result = simulator.simulate(TestInputs.of(1L,
				List.of(creature(304, "Archive Puppet", ARCHIVE, 3, 10)),
				List.of(creature(302, "SCP-999", ARCHIVE, 2, 7))));

		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 3, 30, 28, 1, 0);
	}

	@Test
	@DisplayName("Esempio 03: DRAW, BOTH_DECKS_EMPTY, 2 round, PV 30–30, due sconfitte a testa")
	void simulate_example03() {
		CardDefinition dullers = creature(101, "Dullers", WOUND, 5, 5);
		CardDefinition faceling = creature(103, "Faceling", WOUND, 5, 5);

		MatchResult result = simulator.simulate(TestInputs.of(1L, List.of(dullers, dullers),
				List.of(faceling, faceling)));

		assertResult(result, BattleResult.DRAW, EndReason.BOTH_DECKS_EMPTY, 2, 30, 30, 2, 2);
	}

	@Test
	@DisplayName("Esempio 04: CHALLENGER_WIN, NO_CREATURES_LEFT, 1 round, PV 30–30 (DEF finale 0)")
	void simulate_example04() {
		MatchResult result = simulator.simulate(TestInputs.of(1L,
				List.of(creature(102, "Crawlers", WOUND, 4, 5)),
				List.of(creature(404, "Kodama", DREAM, 4, 6))));

		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1, 30, 30, 1, 0);
	}

	@Test
	@DisplayName("Esempio 05: DRAW, BOTH_DECKS_EMPTY, 3 round, PV 30–30 (defBonus 3 una volta sola)")
	void simulate_example05() {
		SimulationInput input = new SimulationInput(1L, TestRules.standard(), TestFactions.all(),
				List.of(new FactionBonus(ARCHIVE, INVITATION, 0, 3)),
				new PlayerInput(TestInputs.CHALLENGER_USER_ID, List.of(creature(303, "SCP-1762", ARCHIVE, 3, 4))),
				new PlayerInput(TestInputs.OPPONENT_USER_ID, List.of(creature(201, "Squonk", INVITATION, 3, 9))));

		MatchResult result = simulator.simulate(input);

		assertResult(result, BattleResult.DRAW, EndReason.BOTH_DECKS_EMPTY, 3, 30, 30, 1, 1);
	}

	@Test
	@DisplayName("Esempio 06: DRAW, ROUND_LIMIT, 75 round, PV 30–30 (due ATK 0, nessun loop infinito)")
	void simulate_example06() {
		MatchResult result = simulator.simulate(TestInputs.of(1L,
				List.of(creature(401, "Kasa-obake", DREAM, 0, 5)),
				List.of(creature(403, "Zashiki-warashi", DREAM, 0, 5))));

		assertResult(result, BattleResult.DRAW, EndReason.ROUND_LIMIT, 75, 30, 30, 0, 0);
	}

	@Test
	@DisplayName("Esempio 07: CHALLENGER_WIN, ROUND_LIMIT, 75 round, PV 30–29")
	void simulate_example07() {
		CardDefinition scarecrow = creature(205, "Limping Scarecrow", INVITATION, 0, 200);

		MatchResult result = simulator.simulate(TestInputs.of(1L,
				List.of(creature(204, "Shadowless Dog", INVITATION, 3, 1000)), List.of(scarecrow, scarecrow)));

		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.ROUND_LIMIT, 75, 30, 29, 1, 0);
	}

	@Test
	@DisplayName("Esempio 08 (seed 42): CHALLENGER_WIN, NO_CREATURES_LEFT, 14 round, PV 30–27, sconfitte 10–9")
	void simulate_example08() {
		MatchResult result = simulator.simulate(example08(42L));

		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 14, 30, 27, 10, 9);
	}

	@Test
	@DisplayName("Esempio 10: CHALLENGER_WIN, LIFE_POINTS_ZERO, 1 round, PV 30 e −5 (D-43)")
	void simulate_example10() {
		MatchResult result = simulator.simulate(TestInputs.of(1L,
				List.of(creature(304, "Archive Puppet", ARCHIVE, 40, 50)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 1, 5))));

		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO, 1, 30, -5, 1, 0);
	}

	@Test
	@DisplayName("Esempio 11: CHALLENGER_WIN, NO_CREATURES_LEFT, 1 round, PV 30–28 (vantaggio della seconda fazione)")
	void simulate_example11() {
		MatchResult result = simulator.simulate(TestInputs.of(1L, List.of(rootbound()),
				List.of(creature(304, "Archive Puppet", ARCHIVE, 6, 6))));

		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1, 30, 28, 1, 0);
	}

	@Test
	@DisplayName("Esempio 12: OPPONENT_WIN, NO_CREATURES_LEFT, 2 round, PV 24–30 (svantaggio della seconda fazione)")
	void simulate_example12() {
		MatchResult result = simulator.simulate(TestInputs.of(1L, List.of(rootbound()),
				List.of(creature(404, "Kodama", DREAM, 6, 11))));

		assertResult(result, BattleResult.OPPONENT_WIN, EndReason.NO_CREATURES_LEFT, 2, 24, 30, 0, 1);
	}

	@Test
	@DisplayName("Una Spell pescata prima di ENG-19 si registra con CARD_DRAWN e si scarta; si pesca la creatura dopo")
	void simulate_nonCreatureDrawn_loggedAndSkipped() {
		// Arrange: con il seed 1 il mescolamento mette la Spell prima di SCP-131, quindi si pesca per prima
		CardDefinition veil = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORED");
		SimulationInput input = TestInputs.of(1L, List.of(veil, creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5)));

		// Act
		MatchResult result = new MatchSimulator(TestEffects.stubsFor(input)).simulate(input);

		// Assert: la Spell non ha effetto, la partita è quella dell'esempio 01
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.CARD_DRAWN)
				.singleElement()
				.satisfies(event -> assertThat(event.data()).containsEntry("card", "Veil of the Anchored"));
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1, 30, 27, 1, 0);
	}

	@Test
	@DisplayName("D-06: con sole carte non creatura rimaste si pescano tutte, poi NO_CREATURES_LEFT")
	void simulate_onlyNonCreaturesLeft_drawnThenNoCreaturesLeft() {
		// Arrange: l'avversario ha Eternal Cup e una Hidden; la Hidden si pesca dopo la morte di Eternal Cup
		CardDefinition sanctuary = TestCards.hidden(105, "Last Sanctuary", CardSubtype.SURVIVAL, "LAST_SANCTUARY");
		SimulationInput input = TestInputs.of(1L, List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5), sanctuary));

		// Act
		MatchResult result = new MatchSimulator(TestEffects.stubsFor(input)).simulate(input);

		// Assert
		assertThat(result.endReason()).isEqualTo(EndReason.NO_CREATURES_LEFT);
		assertThat(result.log()).extracting(MatchEvent::type).contains(EventType.CARD_DRAWN);
	}

	@Test
	@DisplayName("Un input non valido non viene simulato: mazzo senza creature → InvalidSimulationInputException")
	void simulate_invalidInput_throws() {
		CardDefinition veil = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORED");

		assertThatThrownBy(() -> simulator.simulate(TestInputs.of(List.of(veil),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5)))))
				.isInstanceOf(InvalidSimulationInputException.class);
	}

	@Test
	@DisplayName("Il simulatore controlla i codici con il suo registro: una Spell che non c'è → UNKNOWN_EFFECT")
	void simulate_effectNotInRegistry_throwsUnknownEffect() {
		// Arrange: il registro del gioco non ha ancora gli effetti delle Spell (ENG-19)
		CardDefinition veil = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORED");
		SimulationInput input = TestInputs.of(List.of(veil, creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5)));

		// Act + Assert
		assertThatThrownBy(() -> simulator.simulate(input))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_EFFECT));
	}

	@Test
	@DisplayName("La stessa istanza gioca partite indipendenti: il secondo risultato non dipende dal primo")
	void simulate_twoMatchesWithSameInstance_independent() {
		// Arrange
		SimulationInput example01 = TestInputs.of(1L, List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5)));

		// Act
		simulator.simulate(example08(42L));
		MatchResult result = simulator.simulate(example01);

		// Assert
		assertThat(result.log().getFirst().sequence()).isEqualTo(1);
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1, 30, 27, 1, 0);
	}

	private static SimulationInput example08(long seed) {
		List<CardDefinition> challenger = List.of(
				creature(101, "Dullers", WOUND, 3, 6),
				creature(102, "Crawlers", WOUND, 4, 5),
				creature(102, "Crawlers", WOUND, 4, 5),
				creature(103, "Faceling", WOUND, 5, 4),
				creature(105, "Yellow Hum", WOUND, 6, 3),
				creature(301, "SCP-131", ARCHIVE, 2, 8),
				creature(305, "Eternal Cup", ARCHIVE, 4, 5),
				creature(111, "Wretches", WOUND, 6, 6),
				creature(311, "SCP-049", ARCHIVE, 7, 5),
				creature(112, "Faded Tag", WOUND, 5, 7));
		List<CardDefinition> opponent = List.of(
				creature(201, "Squonk", INVITATION, 3, 7),
				creature(203, "Ash Raven", INVITATION, 5, 4),
				creature(203, "Ash Raven", INVITATION, 5, 4),
				creature(202, "Will-o'-the-Wisp", INVITATION, 6, 3),
				creature(404, "Kodama", DREAM, 4, 6),
				creature(401, "Kasa-obake", DREAM, 5, 5),
				creature(405, "Temple Tanuki", DREAM, 3, 6),
				creature(212, "Wampus Cat", INVITATION, 7, 5),
				creature(411, "Noppera-bo", DREAM, 6, 6),
				creature(213, "White Stag", INVITATION, 4, 8));
		return TestInputs.of(seed, challenger, opponent);
	}

	private static CardDefinition rootbound() {
		return TestCards.creature(601, "Rootbound Doppelganger", DREAM, INVITATION, "Mutant", 6, 10, null);
	}

	private static CardDefinition creature(int cardId, String name, int factionId, int attack, int defense) {
		return TestCards.creature(cardId, name, factionId, null, "Common", attack, defense, null);
	}

	private static void assertResult(MatchResult result, BattleResult expected, EndReason reason, int rounds,
			int challengerLifePoints, int opponentLifePoints, int challengerDefeated, int opponentDefeated) {
		assertThat(result.result()).isEqualTo(expected);
		assertThat(result.endReason()).isEqualTo(reason);
		assertThat(result.totalRounds()).isEqualTo(rounds);
		assertThat(result.challenger()).isEqualTo(new PlayerResult(TestInputs.CHALLENGER_USER_ID,
				challengerLifePoints, challengerDefeated));
		assertThat(result.opponent()).isEqualTo(new PlayerResult(TestInputs.OPPONENT_USER_ID,
				opponentLifePoints, opponentDefeated));
		assertThat(result.log().getLast().type()).isEqualTo(EventType.MATCH_ENDED);
	}
}
