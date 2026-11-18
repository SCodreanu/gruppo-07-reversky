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
import it.downfall.reversky.engine.core.model.PlacedTerrain;
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
	private static final CardDefinition SEALED_FILE = TestCards.item(119, "Fascicolo Sigillato 0", TestFactions.ARCHIVE,
			"SEALED_FILE_ZERO");
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
	@DisplayName("D-31: una creatura pescata entra con una nuova istanza del suo effetto; senza codice non ne ha")
	void drawCreature_createsEffectPerEntry() {
		// Arrange: due copie di Hidebehind, una Common senza codice
		CardDefinition hidebehind = TestCards.creature(23, "Hidebehind", TestFactions.INVITATION, null, "Uncommon", 4,
				6, "HIDEBEHIND");
		MatchState state = TestStates.withDecks(List.of(hidebehind, CRAWLERS), List.of(hidebehind));

		// Act
		draws.drawCreature(state, state.challenger());
		draws.drawCreature(state, state.opponent());

		// Assert
		CreatureInPlay first = state.challenger().creature().orElseThrow();
		CreatureInPlay second = state.opponent().creature().orElseThrow();
		assertThat(first.effect()).hasValueSatisfying(effect -> assertThat(effect.code()).isEqualTo("HIDEBEHIND"));
		assertThat(second.effect()).isPresent();
		assertThat(first.effect().get()).isNotSameAs(second.effect().get());
		state.challenger().removeCreature();
		draws.drawCreature(state, state.challenger());
		assertThat(state.challenger().creature().orElseThrow().effect()).isEmpty();
	}

	@Test
	@DisplayName("D-08: una Spell che uccide SCP-2317 fa scattare il suo effetto alla morte prima che lasci il campo")
	void drawCreature_damageSpellKillsCreatureWithDeathEffect() {
		// Arrange: SCP-2317 dell'avversario ha DEF 4; lo sfidante pesca Skin Fracture, poi Crawlers
		MatchState state = TestStates.withDecks(List.of(FRACTURE, CRAWLERS), List.of());
		EffectRegistry registry = EffectRegistry.withAllEffects();
		state.opponent().placeCreature(TestStates.withEffect(registry, TestCards.creature(0, "SCP-2317",
				TestFactions.ARCHIVE, null, "Mythic", 3, 4, "SCP_2317"), Side.OPPONENT));

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert: nessuna creatura dello sfidante da colpire; il +1 ATK resta in attesa per l'avversario
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.opponent().pendingModifiers()).singleElement()
				.satisfies(modifier -> assertThat(modifier.attack()).isEqualTo(1));
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
	@DisplayName("D-17: un Terreno pescato entra in gioco e dà subito il bonus alla creatura avversaria della fazione")
	void drawCreature_terrain_placedWithImmediateBonus() {
		// Arrange: Kappa dell'avversario è Archive come Watching Rift (+1 ATK, +1 DEF)
		CardDefinition rift = TestCards.terrain(115, "Watching Rift", TestFactions.ARCHIVE, "WATCHING_RIFT");
		MatchState state = TestStates.withDecks(List.of(rift, CRAWLERS), List.of());
		CreatureInPlay kappa = new CreatureInPlay(KAPPA, Side.OPPONENT);
		state.opponent().placeCreature(kappa);

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert: anche Crawlers è Archive, ma il suo bonus arriva all'inizio dello scontro
		assertThat(state.terrain()).contains(new PlacedTerrain(rift, Side.CHALLENGER));
		assertThat(kappa.attack()).isEqualTo(7);
		assertThat(kappa.defense()).isEqualTo(5);
		assertThat(state.challenger().creature().orElseThrow().attack()).isEqualTo(4);
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CARD_DRAWN,
				EventType.TERRAIN_PLACED, EventType.EFFECT_TRIGGERED, EventType.CREATURE_ENTERED);
	}

	@Test
	@DisplayName("D-47: un Oggetto pescato resta accanto al mazzo, non si scarta e non conta come creatura")
	void drawCreature_item_placedNextToDeck() {
		// Arrange
		CardDefinition bell = TestCards.item(120, "Campanello del Torii", TestFactions.DREAM, "TORII_BELL");
		MatchState state = TestStates.withDecks(List.of(bell, CRAWLERS), List.of());

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(state.challenger().items()).containsExactly(bell);
		assertThat(state.challenger().creature()).map(CreatureInPlay::name).contains("Crawlers");
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CARD_DRAWN,
				EventType.ITEM_PLACED, EventType.CREATURE_ENTERED);
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0 annulla del tutto una Spell DAMAGE che colpirebbe la creatura protetta")
	void drawCreature_damageSpellOnSealedFile_cancelled() {
		// Arrange: Kappa dell'avversario è protetta dal Fascicolo
		MatchState state = TestStates.withDecks(List.of(FRACTURE, CRAWLERS), List.of());
		CreatureInPlay kappa = new CreatureInPlay(KAPPA, Side.OPPONENT);
		state.opponent().placeCreature(kappa);
		state.opponent().placeItem(SEALED_FILE);

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(kappa.defense()).isEqualTo(4);
		assertThat(state.opponent().items()).isEmpty();
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.CARD_DRAWN,
				EventType.ITEM_CONSUMED, EventType.SPELL_RESOLVED, EventType.CREATURE_ENTERED);
		assertThat(state.log().events().get(1).data()).containsEntry("effect", "cancels Skin Fracture");
		assertThat(state.log().events().get(2).data()).containsEntry("effect", "cancelled");
	}

	@Test
	@DisplayName("Fascicolo Sigillato 0 non ferma una Spell del proprietario né una Spell SHIELD avversaria")
	void drawCreature_shieldSpell_notCancelled() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(VEIL, CRAWLERS), List.of());
		state.opponent().placeCreature(new CreatureInPlay(KAPPA, Side.OPPONENT));
		state.opponent().placeItem(SEALED_FILE);

		// Act
		draws.drawCreature(state, state.challenger());

		// Assert
		assertThat(state.opponent().items()).containsExactly(SEALED_FILE);
		assertThat(state.challenger().pendingModifiers()).hasSize(1);
	}

	@Test
	@DisplayName("Mazzo vuoto: niente da pescare")
	void drawCreature_emptyDeck_false() {
		MatchState state = TestStates.withDecks(List.of(), List.of());

		assertThat(draws.drawCreature(state, state.challenger())).isFalse();
		assertThat(state.log().events()).isEmpty();
	}
}
