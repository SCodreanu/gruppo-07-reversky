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
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.SetHidden;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestEffects;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link DrawService}: la pescata (regole-base, "Pescare") con le Spell risolte subito (D-10) e le morti
 * controllate dopo ogni Spell (D-08). I mazzi sono nell'ordine dato, senza mescolamento.
 */
class DrawServiceTest {

	private static final CardDefinition CRAWLERS = TestCards.creature("Crawlers", TestFactions.ARCHIVE, 4, 5);
	private static final CardDefinition KAPPA = TestCards.creature("Kappa", TestFactions.ARCHIVE, 6, 4);
	private static final CardDefinition VEIL = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");
	private static final CardDefinition SANCTUARY = TestCards.hidden(105, "Last Sanctuary", CardSubtype.SURVIVAL,
			"LAST_SANCTUARY");
	private static final CardDefinition FRACTURE = TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE,
			"SKIN_FRACTURE");

	private final DrawService draws = new DrawService(EffectRegistry.withAllEffects());

	@Test
	@DisplayName("Una Spell pescata si risolve e si scarta, poi si continua a pescare finché esce una creatura")
	void drawCreature_spellThenCreature() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(VEIL, CRAWLERS, KAPPA), List.of());

		// Act
		boolean entered = draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(entered).isTrue();
		assertThat(state.challenger().creature()).map(CreatureInPlay::name).contains("Crawlers");
		assertThat(state.challenger().deck()).containsExactly(KAPPA);
		assertThat(state.challenger().pendingModifiers()).hasSize(1);
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CARD_DRAWN,
				EventType.SPELL_RESOLVED, EventType.CREATURE_ENTERED);
	}

	@Test
	@DisplayName("D-08: Skin Fracture pescata uccide la creatura avversaria (4 − 5 = −1): −1 PV, conta per chi pesca")
	void drawCreature_damageSpellKillsOpponent() {
		// Arrange: lo sfidante ha il campo vuoto e pesca Skin Fracture; Kappa dell'avversario ha DEF 4
		MatchState state = TestStates.withDecks(List.of(FRACTURE, CRAWLERS), List.of());
		state.opponent().placeCreature(new CreatureInPlay(KAPPA, Side.OPPONENT));

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 1);
		assertThat(state.challenger().creaturesDefeated()).isEqualTo(1);
		assertThat(state.challenger().creature()).isPresent();
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CARD_DRAWN,
				EventType.SPELL_RESOLVED, EventType.CREATURE_DIED, EventType.PLAYER_DAMAGED,
				EventType.CREATURE_ENTERED);
	}

	@Test
	@DisplayName("D-06: con sole carte non creatura nel mazzo si risolvono tutte, e nessuna creatura entra")
	void drawCreature_onlySpells_allResolved() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(VEIL, FRACTURE), List.of());

		// Act
		boolean entered = draws.drawCreature(state, state.challenger());

		// Assert: Skin Fracture senza bersaglio resta in attesa della prossima creatura avversaria
		assertThat(entered).isFalse();
		assertThat(state.challenger().deck()).isEmpty();
		assertThat(state.opponent().pendingModifiers()).hasSize(1);
		assertThat(state.log().events()).filteredOn(event -> event.type() == EventType.SPELL_RESOLVED).hasSize(2);
	}

	@Test
	@DisplayName("D-15: una Hidden pescata è coperta (senza nome nel log), pronta dal prossimo scontro")
	void drawCreature_hidden_setFaceDown() {
		// Arrange: sono già iniziati 3 scontri
		MatchState state = TestStates.withDecks(List.of(SANCTUARY, CRAWLERS), List.of());
		for (int combat = 0; combat < 3; combat++) {
			state.startNextCombat();
		}

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(state.challenger().hiddenZone()).containsExactly(new SetHidden(SANCTUARY, 4));
		assertThat(state.log().events()).extracting(MatchEvent::type)
				.containsExactly(EventType.HIDDEN_SET, EventType.CREATURE_ENTERED);
		assertThat(state.log().events().getFirst().message()).isEqualTo("Challenger sets a Hidden card");
		assertThat(state.log().events().getFirst().data()).isEmpty();
	}

	@Test
	@DisplayName("D-15: una Hidden della pescata iniziale salta il primo scontro, è pronta dal secondo")
	void drawCreature_hiddenInInitialDraw_readyFromSecondCombat() {
		// Arrange: nessuno scontro ancora iniziato
		MatchState state = TestStates.withDecks(List.of(SANCTUARY, CRAWLERS), List.of());

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(state.challenger().hiddenZone()).containsExactly(new SetHidden(SANCTUARY, 2));
	}

	@Test
	@DisplayName("Terreni e Oggetti per ora si registrano con CARD_DRAWN e si scartano (ENG-21, ENG-33)")
	void drawCreature_terrainAndItem_loggedAndSkipped() {
		// Arrange
		CardDefinition rift = TestCards.terrain(113, "Echo Rift", TestFactions.WOUND, "ECHO_RIFT");
		CardDefinition bell = TestCards.item(120, "Torii Bell", TestFactions.DREAM, "TORII_BELL");
		List<CardDefinition> deck = List.of(rift, bell, CRAWLERS);
		MatchState state = TestStates.withDecks(deck, List.of());
		DrawService withStubs = new DrawService(TestEffects.stubsFor(deck));

		// Act
		withStubs.drawCreature(state, state.challenger());

		// Assert
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CARD_DRAWN,
				EventType.CARD_DRAWN, EventType.CREATURE_ENTERED);
		assertThat(state.terrain()).isEmpty();
		assertThat(state.challenger().item()).isEmpty();
	}

	@Test
	@DisplayName("Mazzo vuoto: niente da pescare")
	void drawCreature_emptyDeck_false() {
		MatchState state = TestStates.withDecks(List.of(), List.of());

		assertThat(draws.drawCreature(state, state.challenger())).isFalse();
		assertThat(state.log().events()).isEmpty();
	}
}
