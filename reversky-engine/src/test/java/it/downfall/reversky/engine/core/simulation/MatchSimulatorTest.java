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

		MatchSimulator withStubs = new MatchSimulator(TestEffects.gameWithStubsFor(input));

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
	@DisplayName("Spell SHIELD pescata prima della creatura: SCP-131 entra con +5 DEF (6 → 11), poi l'esempio 01")
	void simulate_shieldSpellDrawnFirst_appliedToNextCreature() {
		// Arrange: con il seed 1 il mescolamento mette la Spell prima di SCP-131, quindi si pesca per prima
		CardDefinition veil = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORED");
		SimulationInput input = TestInputs.of(1L, List.of(veil, creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert: SCP-131 finisce a 11 − 4 = 7 invece di 2; Eternal Cup muore come nell'esempio 01
		assertThat(result.log()).extracting(MatchEvent::type).startsWith(EventType.MATCH_STARTED,
				EventType.CARD_DRAWN, EventType.SPELL_RESOLVED, EventType.CREATURE_ENTERED,
				EventType.CREATURE_ENTERED, EventType.EFFECT_TRIGGERED, EventType.ROUND_STARTED);
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.EFFECT_TRIGGERED)
				.singleElement()
				.extracting(MatchEvent::message)
				.isEqualTo("SCP-131: +5 DEF from Veil of the Anchored (ATK 8, DEF 11)");
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.DAMAGE_DEALT)
				.extracting(event -> event.data().get("targetDefense"))
				.containsExactly(-3, 7);
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 1, 30, 27, 1, 0);
	}

	@Test
	@DisplayName("D-06: l'ultima carta dell'avversario, Skin Fracture, uccide SCP-131 (2 − 5 = −3): BOTH_DECKS_EMPTY")
	void simulate_onlySpellLeft_resolvedThenDecksEmpty() {
		// Arrange: con il seed 1 l'avversario pesca prima Eternal Cup, poi Skin Fracture dopo la sua morte
		CardDefinition fracture = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE");
		SimulationInput input = TestInputs.of(1L, List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5), fracture));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert: round 1 come l'esempio 01 (avversario −3 PV, SCP-131 a DEF 2), poi la Spell toglie 3 PV allo
		// sfidante; nessuno ha più creature e i punti vita sono pari
		assertResult(result, BattleResult.DRAW, EndReason.BOTH_DECKS_EMPTY, 1, 27, 27, 1, 1);
		assertThat(result.log()).extracting(MatchEvent::type).endsWith(EventType.CARD_DRAWN,
				EventType.SPELL_RESOLVED, EventType.CREATURE_DIED, EventType.PLAYER_DAMAGED, EventType.MATCH_ENDED);
	}

	@Test
	@DisplayName("ENG-29: l'avversario a 0 PV dopo il round non pesca più, la sua Spell non porta a 0 lo sfidante")
	void simulate_opponentAtZeroAfterRound_endsBeforeHisDraw() {
		// Arrange: come il test D-06 (seed 1: Eternal Cup, poi Skin Fracture) ma con 3 PV. Round 1: Eternal Cup
		// 5 − 8 = −3, l'avversario va a 0. Se pescasse, Skin Fracture ucciderebbe SCP-131 (2 − 5 = −3) e anche lo
		// sfidante andrebbe a 0
		CardDefinition fracture = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE");
		SimulationInput input = withRules(3, TestRules.MAX_ROUNDS, 1L,
				List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5), fracture));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert: regole-base, "Danno al giocatore che porta i punti vita sotto zero → la partita finisce subito"
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO, 1, 3, 0, 1, 0);
		assertThat(result.log()).extracting(MatchEvent::type).endsWith(EventType.CREATURE_DIED,
				EventType.PLAYER_DAMAGED, EventType.MATCH_ENDED);
	}

	@Test
	@DisplayName("ENG-29: lo sfidante a 0 PV dopo il round non pesca più, la sua Spell non porta a 0 l'avversario")
	void simulate_challengerAtZeroAfterRound_endsBeforeHisDraw() {
		// Arrange: con il seed 1 lo sfidante pesca Eternal Cup, poi Skin Fracture. Round 1: Eternal Cup 5 − 8 = −3,
		// lo sfidante va a 0. Se pescasse, Skin Fracture ucciderebbe SCP-131 (2 − 5 = −3) e anche l'avversario
		// andrebbe a 0
		CardDefinition fracture = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE");
		SimulationInput input = withRules(3, TestRules.MAX_ROUNDS, 1L,
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5), fracture),
				List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert
		assertResult(result, BattleResult.OPPONENT_WIN, EndReason.LIFE_POINTS_ZERO, 1, 0, 3, 0, 1);
		assertThat(result.log()).extracting(MatchEvent::type).endsWith(EventType.CREATURE_DIED,
				EventType.PLAYER_DAMAGED, EventType.MATCH_ENDED);
	}

	@Test
	@DisplayName("ENG-29: la Spell dello sfidante porta l'avversario a 0 PV mentre pesca: l'avversario non pesca più")
	void simulate_opponentAtZeroDuringChallengerDraw_endsBeforeOpponentDraw() {
		// Arrange: con il seed 14 i mazzi restano nell'ordine dato, 3 PV a testa. Round 1: Crawler 8 − 8 = 0 muore
		// senza danno al giocatore, SCP-131 6 − 4 = 2. Lo sfidante pesca Skin Fracture (SCP-131 2 − 5 = −3,
		// l'avversario va a 0) e poi Faceling. Se l'avversario pescasse, la sua Skin Fracture ucciderebbe Faceling
		// (2 − 5 = −3) e anche lo sfidante andrebbe a 0
		CardDefinition fracture = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE");
		SimulationInput input = withRules(3, TestRules.MAX_ROUNDS, 14L,
				List.of(creature(4, "Crawler", ARCHIVE, 4, 8), fracture, creature(103, "Faceling", ARCHIVE, 4, 2)),
				List.of(creature(301, "SCP-131", ARCHIVE, 8, 6), fracture, creature(306, "Mothling", ARCHIVE, 3, 4)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert: Faceling entra (la pescata dello sfidante finisce), poi la partita finisce senza la pescata
		// dell'avversario
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.LIFE_POINTS_ZERO, 1, 3, 0, 1, 1);
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.CREATURE_ENTERED)
				.extracting(event -> event.data().get("card"))
				.containsExactly("Crawler", "SCP-131", "Faceling");
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.SPELL_RESOLVED).hasSize(1);
	}

	@Test
	@DisplayName("ENG-29: dopo l'ultimo round chi ha perso la creatura pesca ancora e la sua Spell sposta i PV")
	void simulate_drawAfterLastRound_stillResolvesBeforeRoundLimit() {
		// Arrange: limite di 1 round; con il seed 7 i mazzi restano nell'ordine dato. Round 1: Eternal Cup 5 − 8 = −3
		// (avversario 27). L'avversario pesca Skin Fracture (SCP-131 2 − 5 = −3, sfidante 27), poi Mothling
		CardDefinition fracture = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE");
		SimulationInput input = withRules(30, 1, 7L,
				List.of(creature(301, "SCP-131", ARCHIVE, 8, 6), creature(103, "Faceling", ARCHIVE, 4, 2)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5), fracture,
						creature(306, "Mothling", ARCHIVE, 3, 4)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert: deciso per ENG-29, la pescata dopo l'ultimo round si risolve come dopo ogni altro round (D-06);
		// senza la Spell sarebbe CHALLENGER_WIN 30–27
		assertResult(result, BattleResult.DRAW, EndReason.ROUND_LIMIT, 1, 27, 27, 1, 1);
		assertThat(result.log()).extracting(MatchEvent::type).endsWith(EventType.CREATURE_ENTERED,
				EventType.MATCH_ENDED);
	}

	@Test
	@DisplayName("ENG-29: morte simultanea all'ultimo round con creature nei mazzi: si pesca, poi ROUND_LIMIT")
	void simulate_simultaneousDeathInLastRound_roundLimit() {
		// Arrange: limite di 1 round, quattro creature 5/5 (l'ordine non conta). Round 1: muoiono entrambe
		CardDefinition crawler = creature(4, "Crawler", ARCHIVE, 5, 5);
		CardDefinition mothling = creature(306, "Mothling", ARCHIVE, 5, 5);
		SimulationInput input = withRules(30, 1, 1L, List.of(crawler, crawler), List.of(mothling, mothling));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert: nessun danno ai giocatori, entrambi pescano, poi il limite con PV pari
		assertResult(result, BattleResult.DRAW, EndReason.ROUND_LIMIT, 1, 30, 30, 1, 1);
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.CREATURE_ENTERED).hasSize(4);
	}

	@Test
	@DisplayName("D-15: una Hidden della pescata iniziale salta il primo scontro e si attiva nel secondo")
	void simulate_hiddenInInitialDraw_triggersInSecondCombat() {
		// Arrange: con il seed 3 lo sfidante pesca Last Sanctuary prima di SCP-131, l'avversario Eternal Cup prima
		// di Mothling. Round 1 come l'esempio 01; nel secondo scontro Last Sanctuary dà +5 DEF a SCP-131 (2 → 7) e
		// lo scudo assorbe i 3 danni di Mothling; Mothling 4 − 8 = −4
		CardDefinition sanctuary = TestCards.hidden(105, "Last Sanctuary", CardSubtype.SURVIVAL, "LAST_SANCTUARY");
		SimulationInput input = TestInputs.of(3L, List.of(sanctuary, creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 4, 5), creature(306, "Mothling", ARCHIVE, 3, 4)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 2, 30, 23, 2, 0);
		assertThat(result.log()).extracting(MatchEvent::type).startsWith(EventType.MATCH_STARTED,
				EventType.HIDDEN_SET, EventType.CREATURE_ENTERED, EventType.CREATURE_ENTERED, EventType.ROUND_STARTED);
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.HIDDEN_TRIGGERED)
				.singleElement()
				.satisfies(event -> assertThat(event.round()).isEqualTo(1));
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.DAMAGE_DEALT)
				.extracting(event -> event.data().get("targetDefense"))
				.containsExactly(-3, 2, -4, 4);
	}

	@Test
	@DisplayName("D-47: un Oggetto pescato resta accanto al mazzo; il Badge salva Eternal Cup al round 1, poi è DRAW")
	void simulate_itemDrawn_badgeSavesOnce() {
		// Arrange: con il seed 1 l'avversario pesca il Badge prima di Eternal Cup. Round 1: Eternal Cup 5 − 8 = −3,
		// il Badge la lascia a 1. Round 2: Eternal Cup 1 − 8 = −7, SCP-131 2 − 4 = −2: muoiono entrambe
		CardDefinition badge = TestCards.item(117, "Badge di Uscita Negata", WOUND, "DENIED_EXIT_BADGE");
		SimulationInput input = TestInputs.of(1L, List.of(creature(301, "SCP-131", ARCHIVE, 8, 6)),
				List.of(badge, creature(305, "Eternal Cup", ARCHIVE, 4, 5)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert
		assertResult(result, BattleResult.DRAW, EndReason.BOTH_DECKS_EMPTY, 2, 30, 30, 1, 1);
		assertThat(result.log()).extracting(MatchEvent::type).contains(EventType.ITEM_PLACED,
				EventType.ITEM_CONSUMED);
	}

	@Test
	@DisplayName("Una Spell dell'avversario uccide la creatura appena pescata dallo sfidante: lo sfidante ripesca")
	void simulate_spellKillsCreatureJustDrawn_ownerDrawsAgain() {
		// Arrange: con il seed 5 l'avversario pesca nell'ordine dato. Round 1: 5/4 contro 5/5, muoiono entrambe.
		// Lo sfidante pesca un 5/4, l'avversario pesca Skin Fracture (4 − 5 = −1: lo sfidante perde 1 PV) e poi
		// Mothling 3/3. Lo sfidante pesca l'ultimo 5/4, round 2: Mothling 3 − 5 = −2, il 5/4 resta a 1
		CardDefinition crawler = creature(4, "Crawler", ARCHIVE, 5, 4);
		CardDefinition fracture = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE");
		SimulationInput input = TestInputs.of(5L, List.of(crawler, crawler, crawler),
				List.of(creature(305, "Eternal Cup", ARCHIVE, 5, 5), fracture,
						creature(306, "Mothling", ARCHIVE, 3, 3)));

		// Act
		MatchResult result = simulator.simulate(input);

		// Assert
		assertResult(result, BattleResult.CHALLENGER_WIN, EndReason.NO_CREATURES_LEFT, 2, 29, 28, 2, 2);
		assertThat(result.log()).filteredOn(event -> event.type() == EventType.CREATURE_ENTERED)
				.extracting(event -> event.data().get("card"))
				.containsExactly("Crawler", "Eternal Cup", "Crawler", "Mothling", "Crawler");
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
		// Arrange: codice scritto male
		CardDefinition veil = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORD");
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

	/** Input standard con punti vita iniziali e limite di round scelti dal test. */
	private static SimulationInput withRules(int lifePoints, int maxRounds, long seed,
			List<CardDefinition> challengerDeck, List<CardDefinition> opponentDeck) {
		return new SimulationInput(seed, TestRules.of(lifePoints, maxRounds), TestFactions.all(),
				TestFactions.standardBonuses(), new PlayerInput(TestInputs.CHALLENGER_USER_ID, challengerDeck),
				new PlayerInput(TestInputs.OPPONENT_USER_ID, opponentDeck));
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
