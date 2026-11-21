package it.downfall.reversky.engine.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static it.downfall.reversky.engine.support.TestFactions.ARCHIVE;
import static it.downfall.reversky.engine.support.TestFactions.DREAM;
import static it.downfall.reversky.engine.support.TestFactions.INVITATION;
import static it.downfall.reversky.engine.support.TestFactions.WOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.Faction;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestEffects;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestRules;

/**
 * Test di {@link SimulationInputValidator}: un test per ogni codice di errore di docs/api-engine ("Errori") e per
 * l'ordine dei controlli, così lo stesso input dà sempre lo stesso errore.
 * <p>
 * Il registro ha un effetto finto per ogni codice di Spell, Hidden, Terreni e Oggetti usato qui: gli effetti veri
 * arrivano con ENG-19, ENG-20, ENG-21 ed ENG-33.
 */
class SimulationInputValidatorTest {

	private static final int UNKNOWN = 99;

	private static final List<Faction> FACTIONS = TestFactions.all();
	private static final List<FactionBonus> BONUSES = TestFactions.standardBonuses();
	private static final RuleConfig RULES = TestRules.standard();

	private static final CardDefinition CRAWLERS = creature(4, WOUND, null, 4, 5);
	private static final CardDefinition KAPPA = creature(401, DREAM, null, 6, 7);
	private static final CardDefinition VEIL = TestCards.spell(70, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");
	private static final CardDefinition LAST_SANCTUARY = TestCards.hidden(80, "Last Sanctuary",
			CardSubtype.SURVIVAL, "LAST_SANCTUARY");
	private static final CardDefinition ECHO_RIFT = TestCards.terrain(90, "Echo Rift", WOUND, "ECHO_RIFT");
	private static final CardDefinition TORII_BELL = TestCards.item(95, "Campanello del Torii", DREAM,
			"TORII_BELL");

	private final SimulationInputValidator validator = new SimulationInputValidator(TestEffects.stubsFor(List.of(
			VEIL, LAST_SANCTUARY, ECHO_RIFT, TORII_BELL,
			TestCards.spell(93, "Bite of the Reverse", CardSubtype.DAMAGE, "BITE_OF_REVERSE"),
			TestCards.hidden(109, "Hostile Presence", CardSubtype.DAMAGE, "HOSTILE_PRESENCE"))));

	@Test
	@DisplayName("Un input coerente con tutti i tipi di carta (creature, Spell, Hidden, Terreno, Oggetto) è accettato")
	void validate_coherentInput_doesNotThrow() {
		// Arrange
		SimulationInput input = input(deck(CRAWLERS, VEIL, LAST_SANCTUARY, ECHO_RIFT, TORII_BELL), deck(KAPPA));

		// Act + Assert
		assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Una carta con due fazioni diverse (Mutant Dream + Invitation) è accettata")
	void validate_twoDifferentFactions_doesNotThrow() {
		// Arrange
		CardDefinition rootbound = creature(601, DREAM, INVITATION, 8, 9);

		// Act + Assert
		assertThatCode(() -> validator.validate(input(deck(rootbound), deck(KAPPA)))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Un atkBonus negativo (malus −1) è accettato; anche una lista di bonus vuota")
	void validate_negativeAtkBonusOrNoBonuses_doesNotThrow() {
		// Arrange
		SimulationInput withMalus = input(FACTIONS, List.of(new FactionBonus(DREAM, WOUND, -1, 0)),
				deck(CRAWLERS), deck(KAPPA));
		SimulationInput withoutBonuses = input(FACTIONS, List.of(), deck(CRAWLERS), deck(KAPPA));

		// Act + Assert
		assertThatCode(() -> validator.validate(withMalus)).doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(withoutBonuses)).doesNotThrowAnyException();
	}

	// ---- 1. VALIDATION_FAILED ----

	@Test
	@DisplayName("Regole mancanti → VALIDATION_FAILED")
	void validate_nullRules_validationFailed() {
		// Arrange
		SimulationInput input = new SimulationInput(42L, null, FACTIONS, BONUSES,
				new PlayerInput(7, deck(CRAWLERS)), new PlayerInput(12, deck(KAPPA)));

		// Act + Assert
		assertFailsWith(input, ErrorCode.VALIDATION_FAILED);
	}

	@Test
	@DisplayName("Sfidante o avversario mancante → VALIDATION_FAILED")
	void validate_nullPlayer_validationFailed() {
		// Arrange
		SimulationInput noChallenger = new SimulationInput(42L, RULES, FACTIONS, BONUSES, null,
				new PlayerInput(12, deck(KAPPA)));
		SimulationInput noOpponent = new SimulationInput(42L, RULES, FACTIONS, BONUSES,
				new PlayerInput(7, deck(CRAWLERS)), null);

		// Act + Assert
		assertFailsWith(noChallenger, ErrorCode.VALIDATION_FAILED);
		assertFailsWith(noOpponent, ErrorCode.VALIDATION_FAILED);
	}

	@Test
	@DisplayName("Mazzo vuoto → VALIDATION_FAILED, non NO_CREATURES_IN_DECK (docs/api-engine, controllo 1)")
	void validate_emptyDeck_validationFailed() {
		assertFailsWith(input(deck(), deck(KAPPA)), ErrorCode.VALIDATION_FAILED);
	}

	@Test
	@DisplayName("Carta senza tipo o senza nome → VALIDATION_FAILED")
	void validate_cardWithoutTypeOrName_validationFailed() {
		// Arrange
		CardDefinition noType = new CardDefinition(4, "Crawlers", null, "Common", 1, WOUND, null, 4, 5, null, null);
		CardDefinition noName = new CardDefinition(4, null, CardType.CREATURE, "Common", 1, WOUND, null, 4, 5, null,
				null);

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, noType), deck(KAPPA)), ErrorCode.VALIDATION_FAILED);
		assertFailsWith(input(deck(CRAWLERS), deck(noName)), ErrorCode.VALIDATION_FAILED);
	}

	@Test
	@DisplayName("La stessa fazione due volte nella lista delle fazioni → VALIDATION_FAILED")
	void validate_duplicateFactionId_validationFailed() {
		// Arrange
		List<Faction> factions = new ArrayList<>(FACTIONS);
		factions.add(new Faction(WOUND, "Theory of the Wound"));

		// Act + Assert
		assertFailsWith(input(factions, BONUSES, deck(CRAWLERS), deck(KAPPA)), ErrorCode.VALIDATION_FAILED);
	}

	// ---- 2. UNKNOWN_FACTION ----

	@Test
	@DisplayName("factionId di una carta non tra le fazioni (esempio 92) → UNKNOWN_FACTION")
	void validate_cardFactionNotInFactions_unknownFaction() {
		assertFailsWith(input(deck(creature(4, UNKNOWN, null, 4, 5)), deck(KAPPA)), ErrorCode.UNKNOWN_FACTION);
	}

	@Test
	@DisplayName("secondaryFactionId non tra le fazioni → UNKNOWN_FACTION")
	void validate_secondaryFactionNotInFactions_unknownFaction() {
		assertFailsWith(input(deck(CRAWLERS), deck(creature(601, DREAM, UNKNOWN, 8, 9))),
				ErrorCode.UNKNOWN_FACTION);
	}

	@Test
	@DisplayName("Fazione di origine o di destinazione di un bonus non tra le fazioni → UNKNOWN_FACTION")
	void validate_bonusFactionNotInFactions_unknownFaction() {
		// Arrange
		List<FactionBonus> unknownOrigin = List.of(new FactionBonus(UNKNOWN, DREAM, 2, 0));
		List<FactionBonus> unknownTarget = List.of(new FactionBonus(WOUND, UNKNOWN, 2, 0));

		// Act + Assert
		assertFailsWith(input(FACTIONS, unknownOrigin, deck(CRAWLERS), deck(KAPPA)), ErrorCode.UNKNOWN_FACTION);
		assertFailsWith(input(FACTIONS, unknownTarget, deck(CRAWLERS), deck(KAPPA)), ErrorCode.UNKNOWN_FACTION);
	}

	// ---- 3. INVALID_FACTION_BONUS ----

	@Test
	@DisplayName("Bonus di una fazione contro se stessa (esempio 93) → INVALID_FACTION_BONUS")
	void validate_bonusAgainstSameFaction_invalidFactionBonus() {
		assertFailsWith(input(FACTIONS, List.of(new FactionBonus(ARCHIVE, ARCHIVE, 2, 0)), deck(CRAWLERS),
				deck(KAPPA)), ErrorCode.INVALID_FACTION_BONUS);
	}

	@Test
	@DisplayName("defBonus negativo (−1) → INVALID_FACTION_BONUS")
	void validate_negativeDefBonus_invalidFactionBonus() {
		assertFailsWith(input(FACTIONS, List.of(new FactionBonus(WOUND, DREAM, 0, -1)), deck(CRAWLERS),
				deck(KAPPA)), ErrorCode.INVALID_FACTION_BONUS);
	}

	@Test
	@DisplayName("Due righe per la stessa coppia Wound → Dream → INVALID_FACTION_BONUS")
	void validate_duplicateBonusPair_invalidFactionBonus() {
		// Arrange
		List<FactionBonus> bonuses = List.of(new FactionBonus(WOUND, DREAM, 2, 0), new FactionBonus(WOUND, DREAM,
				1, 0));

		// Act + Assert
		assertFailsWith(input(FACTIONS, bonuses, deck(CRAWLERS), deck(KAPPA)), ErrorCode.INVALID_FACTION_BONUS);
	}

	// ---- 4. INVALID_CARD_FACTIONS ----

	@Test
	@DisplayName("secondaryFactionId uguale a factionId (esempio 94) → INVALID_CARD_FACTIONS")
	void validate_sameFactionTwice_invalidCardFactions() {
		assertFailsWith(input(deck(creature(601, DREAM, DREAM, 8, 9)), deck(KAPPA)),
				ErrorCode.INVALID_CARD_FACTIONS);
	}

	@Test
	@DisplayName("Creatura, Terreno od Oggetto senza factionId → INVALID_CARD_FACTIONS")
	void validate_factionCardWithoutFaction_invalidCardFactions() {
		// Arrange
		CardDefinition creature = creature(4, null, null, 4, 5);
		CardDefinition terrain = new CardDefinition(90, "Echo Rift", CardType.TERRAIN, "Rare", 3, null, null, null,
				null, null, "ECHO_RIFT");
		CardDefinition item = new CardDefinition(95, "Campanello del Torii", CardType.ITEM, "Rare", 3, null, null,
				null, null, null, "TORII_BELL");

		// Act + Assert
		assertFailsWith(input(deck(creature), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
		assertFailsWith(input(deck(CRAWLERS, terrain), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
		assertFailsWith(input(deck(CRAWLERS, item), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
	}

	@Test
	@DisplayName("Spell o Hidden con una fazione → INVALID_CARD_FACTIONS")
	void validate_sharedCardWithFaction_invalidCardFactions() {
		// Arrange
		CardDefinition spell = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2,
				ARCHIVE, null, null, null, CardSubtype.SHIELD, "VEIL_OF_ANCHORED");
		CardDefinition hidden = new CardDefinition(80, "Last Sanctuary", CardType.HIDDEN, "Rare", 3, null, WOUND,
				null, null, CardSubtype.SURVIVAL, "LAST_SANCTUARY");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, spell), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
		assertFailsWith(input(deck(CRAWLERS, hidden), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
	}

	@Test
	@DisplayName("Terreno od Oggetto con una seconda fazione → INVALID_CARD_FACTIONS: solo le creature ne hanno due")
	void validate_terrainOrItemWithSecondFaction_invalidCardFactions() {
		// Arrange
		CardDefinition terrain = new CardDefinition(90, "Echo Rift", CardType.TERRAIN, "Rare", 3, WOUND, ARCHIVE,
				null, null, null, "ECHO_RIFT");
		CardDefinition item = new CardDefinition(95, "Campanello del Torii", CardType.ITEM, "Rare", 3, DREAM,
				INVITATION, null, null, null, "TORII_BELL");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, terrain), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
		assertFailsWith(input(deck(CRAWLERS, item), deck(KAPPA)), ErrorCode.INVALID_CARD_FACTIONS);
	}

	// ---- 5. NO_CREATURES_IN_DECK ----

	@Test
	@DisplayName("Mazzo con sole carte non creatura (esempio 91) → NO_CREATURES_IN_DECK")
	void validate_deckWithoutCreatures_noCreaturesInDeck() {
		assertFailsWith(input(deck(CRAWLERS), deck(VEIL, LAST_SANCTUARY)), ErrorCode.NO_CREATURES_IN_DECK);
	}

	// ---- 6. INVALID_STATS ----

	@Test
	@DisplayName("Creatura senza atk o senza def → INVALID_STATS")
	void validate_creatureWithoutStats_invalidStats() {
		assertFailsWith(input(deck(CRAWLERS, creature(5, WOUND, null, null, 5)), deck(KAPPA)),
				ErrorCode.INVALID_STATS);
		assertFailsWith(input(deck(CRAWLERS, creature(5, WOUND, null, 4, null)), deck(KAPPA)),
				ErrorCode.INVALID_STATS);
	}

	@Test
	@DisplayName("ATK −1 o DEF 0 → INVALID_STATS; ATK 0 e DEF 1 sono validi")
	void validate_statsOutOfRange_invalidStats() {
		assertFailsWith(input(deck(creature(5, WOUND, null, -1, 5)), deck(KAPPA)), ErrorCode.INVALID_STATS);
		assertFailsWith(input(deck(creature(5, WOUND, null, 4, 0)), deck(KAPPA)), ErrorCode.INVALID_STATS);
		assertThatCode(() -> validator.validate(input(deck(creature(5, WOUND, null, 0, 1)), deck(KAPPA))))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("ATK e DEF su una carta non creatura (Spell con ATK 3, DEF 4) si ignorano: l'engine non li legge")
	void validate_statsOnNonCreature_ignored() {
		// Arrange
		CardDefinition spellWithStats = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon",
				2, null, null, 3, 4, CardSubtype.SHIELD, "VEIL_OF_ANCHORED");

		// Act + Assert
		assertThatCode(() -> validator.validate(input(deck(CRAWLERS, spellWithStats), deck(KAPPA))))
				.doesNotThrowAnyException();
	}

	// ---- 7. MISSING_SUBTYPE ----

	@Test
	@DisplayName("Spell o Hidden senza subtype → MISSING_SUBTYPE")
	void validate_sharedCardWithoutSubtype_missingSubtype() {
		// Arrange
		CardDefinition spell = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2, null,
				null, null, null, null, "VEIL_OF_ANCHORED");
		CardDefinition hidden = new CardDefinition(80, "Last Sanctuary", CardType.HIDDEN, "Rare", 3, null, null,
				null, null, null, "LAST_SANCTUARY");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, spell), deck(KAPPA)), ErrorCode.MISSING_SUBTYPE);
		assertFailsWith(input(deck(CRAWLERS, hidden), deck(KAPPA)), ErrorCode.MISSING_SUBTYPE);
	}

	@Test
	@DisplayName("Spell con SURVIVAL → MISSING_SUBTYPE: una Spell è DAMAGE, SHIELD o BURN")
	void validate_spellWithHiddenSubtype_missingSubtype() {
		// Arrange
		CardDefinition spell = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2, null,
				null, null, null, CardSubtype.SURVIVAL, "VEIL_OF_ANCHORED");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, spell), deck(KAPPA)), ErrorCode.MISSING_SUBTYPE);
	}

	@Test
	@DisplayName("Hidden con SHIELD o BURN → MISSING_SUBTYPE: una Hidden è SURVIVAL o DAMAGE")
	void validate_hiddenWithSpellSubtype_missingSubtype() {
		// Arrange
		CardDefinition shield = new CardDefinition(80, "Last Sanctuary", CardType.HIDDEN, "Rare", 3, null, null,
				null, null, CardSubtype.SHIELD, "LAST_SANCTUARY");
		CardDefinition burn = new CardDefinition(80, "Last Sanctuary", CardType.HIDDEN, "Rare", 3, null, null,
				null, null, CardSubtype.BURN, "LAST_SANCTUARY");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, shield), deck(KAPPA)), ErrorCode.MISSING_SUBTYPE);
		assertFailsWith(input(deck(CRAWLERS, burn), deck(KAPPA)), ErrorCode.MISSING_SUBTYPE);
	}

	@Test
	@DisplayName("DAMAGE vale sia per le Spell sia per le Hidden")
	void validate_damageOnSpellAndHidden_doesNotThrow() {
		// Arrange
		CardDefinition spell = new CardDefinition(93, "Bite of the Reverse", CardType.SPELL, "Uncommon", 2, null,
				null, null, null, CardSubtype.DAMAGE, "BITE_OF_REVERSE");
		CardDefinition hidden = new CardDefinition(109, "Hostile Presence", CardType.HIDDEN, "Uncommon", 2, null,
				null, null, null, CardSubtype.DAMAGE, "HOSTILE_PRESENCE");

		// Act + Assert
		assertThatCode(() -> validator.validate(input(deck(CRAWLERS, spell, hidden), deck(KAPPA))))
				.doesNotThrowAnyException();
	}

	// ---- 8. MISSING_EFFECT ----

	@Test
	@DisplayName("Spell, Hidden, Terreno od Oggetto senza effectCode → MISSING_EFFECT")
	void validate_specialCardWithoutEffect_missingEffect() {
		// Arrange
		CardDefinition spell = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2, null,
				null, null, null, CardSubtype.SHIELD, null);
		CardDefinition hidden = new CardDefinition(80, "Last Sanctuary", CardType.HIDDEN, "Rare", 3, null, null,
				null, null, CardSubtype.SURVIVAL, null);
		CardDefinition terrain = new CardDefinition(90, "Echo Rift", CardType.TERRAIN, "Rare", 3, WOUND, null, null,
				null, null, null);
		CardDefinition item = new CardDefinition(95, "Campanello del Torii", CardType.ITEM, "Rare", 3, DREAM, null,
				null, null, null, null);

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, spell), deck(KAPPA)), ErrorCode.MISSING_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, hidden), deck(KAPPA)), ErrorCode.MISSING_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, terrain), deck(KAPPA)), ErrorCode.MISSING_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, item), deck(KAPPA)), ErrorCode.MISSING_EFFECT);
	}

	@Test
	@DisplayName("effectCode vuoto o di soli spazi → MISSING_EFFECT, come se mancasse")
	void validate_blankEffectCode_missingEffect() {
		// Arrange
		CardDefinition empty = new CardDefinition(90, "Echo Rift", CardType.TERRAIN, "Rare", 3, WOUND, null, null,
				null, null, "");
		CardDefinition spaces = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2, null,
				null, null, null, CardSubtype.SHIELD, "   ");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, empty), deck(KAPPA)), ErrorCode.MISSING_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, spaces), deck(KAPPA)), ErrorCode.MISSING_EFFECT);
	}

	@Test
	@DisplayName("Una creatura senza effectCode è valida: per le creature è facoltativo")
	void validate_creatureWithoutEffect_doesNotThrow() {
		assertThatCode(() -> validator.validate(input(deck(CRAWLERS), deck(KAPPA)))).doesNotThrowAnyException();
	}

	// ---- 9. UNKNOWN_EFFECT ----

	@Test
	@DisplayName("Spell, Hidden, Terreno od Oggetto con un effectCode che non è nel registro → UNKNOWN_EFFECT")
	void validate_specialCardWithUnknownEffect_unknownEffect() {
		// Arrange: codici scritti male
		CardDefinition spell = TestCards.spell(70, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORD");
		CardDefinition hidden = TestCards.hidden(80, "Last Sanctuary", CardSubtype.SURVIVAL, "LAST_SANCTUAR");
		CardDefinition terrain = TestCards.terrain(90, "Echo Rift", WOUND, "ECHO_RIF");
		CardDefinition item = TestCards.item(95, "Campanello del Torii", DREAM, "TORII_BEL");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, spell), deck(KAPPA)), ErrorCode.UNKNOWN_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, hidden), deck(KAPPA)), ErrorCode.UNKNOWN_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, terrain), deck(KAPPA)), ErrorCode.UNKNOWN_EFFECT);
		assertFailsWith(input(deck(CRAWLERS, item), deck(KAPPA)), ErrorCode.UNKNOWN_EFFECT);
		assertThatThrownBy(() -> validator.validate(input(deck(CRAWLERS, spell), deck(KAPPA))))
				.hasMessage("Unknown effect VEIL_OF_ANCHORD on card 70");
	}

	@Test
	@DisplayName("Un codice che esiste ma è di un altro tipo di carta (Terreno su una Spell) → UNKNOWN_EFFECT")
	void validate_effectOfAnotherCardType_unknownEffect() {
		// Arrange
		CardDefinition spell = TestCards.spell(70, "Veil of the Anchored", CardSubtype.SHIELD, "ECHO_RIFT");

		// Act + Assert
		assertFailsWith(input(deck(CRAWLERS, spell), deck(KAPPA)), ErrorCode.UNKNOWN_EFFECT);
		assertThatThrownBy(() -> validator.validate(input(deck(CRAWLERS, spell), deck(KAPPA))))
				.hasMessage("Effect ECHO_RIFT is not a SPELL effect: card 70");
	}

	@Test
	@DisplayName("Una creatura con un codice non registrato (HIDEBEHND) dà UNKNOWN_EFFECT, come le altre carte")
	void validate_creatureWithUnknownEffect_throwsUnknownEffect() {
		// Arrange
		CardDefinition creature = new CardDefinition(23, "Hidebehind", CardType.CREATURE, "Uncommon", 2, INVITATION,
				null, 3, 8, null, "HIDEBEHND");

		// Act + Assert
		assertThatThrownBy(() -> validator.validate(input(deck(creature), deck(KAPPA))))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_EFFECT);
					assertThat(e.getMessage()).isEqualTo("Unknown effect HIDEBEHND on card 23");
				});
	}

	@Test
	@DisplayName("Una creatura con il codice di un altro tipo di carta (VEIL_OF_ANCHORED) dà UNKNOWN_EFFECT")
	void validate_creatureWithSpellCode_throwsUnknownEffect() {
		// Arrange
		CardDefinition creature = new CardDefinition(23, "Hidebehind", CardType.CREATURE, "Uncommon", 2, INVITATION,
				null, 3, 8, null, "VEIL_OF_ANCHORED");

		// Act + Assert
		assertThatThrownBy(() -> validator.validate(input(deck(creature), deck(KAPPA))))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_EFFECT);
					assertThat(e.getMessage()).isEqualTo("Effect VEIL_OF_ANCHORED is not a CREATURE effect: card 23");
				});
	}

	@Test
	@DisplayName("Una creatura con un effetto registrato (HIDEBEHIND) o senza codice è valida")
	void validate_creatureWithRegisteredEffectOrNone_accepted() {
		// Arrange
		CardDefinition creature = new CardDefinition(23, "Hidebehind", CardType.CREATURE, "Uncommon", 2, INVITATION,
				null, 3, 8, null, "HIDEBEHIND");

		SimulationInputValidator game = new SimulationInputValidator(EffectRegistry.withAllEffects());

		// Act + Assert: KAPPA qui è una Common senza codice
		assertThatCode(() -> game.validate(input(deck(creature), deck(KAPPA)))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Con un registro vuoto ogni Spell, Hidden, Terreno e Oggetto è sconosciuto")
	void validate_emptyRegistry_everySpecialCardUnknown() {
		// Arrange
		SimulationInputValidator empty = new SimulationInputValidator(new EffectRegistry());

		// Act + Assert
		assertThatThrownBy(() -> empty.validate(input(deck(CRAWLERS, TORII_BELL), deck(KAPPA))))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_EFFECT));
	}

	// ---- Ordine dei controlli ----

	@Test
	@DisplayName("Con due errori vince il primo dell'ordine: fazione sconosciuta (2) prima di mazzo senza creature (5)")
	void validate_twoErrors_firstInContractOrderWins() {
		// Arrange: lo sfidante ha solo il mazzo senza creature, l'avversario una fazione sconosciuta
		SimulationInput input = input(deck(VEIL), deck(creature(401, UNKNOWN, null, 6, 7)));

		// Act + Assert
		assertFailsWith(input, ErrorCode.UNKNOWN_FACTION);
	}

	@Test
	@DisplayName("Lo stesso errore in entrambi i mazzi: il messaggio cita la carta dello sfidante")
	void validate_sameErrorInBothDecks_reportsChallengerCard() {
		// Arrange
		SimulationInput input = input(deck(creature(11, UNKNOWN, null, 4, 5)), deck(creature(22, UNKNOWN, null,
				6, 7)));

		// Act + Assert
		assertThatThrownBy(() -> validator.validate(input))
				.isInstanceOf(InvalidSimulationInputException.class)
				.hasMessageContaining("card 11");
	}

	private void assertFailsWith(SimulationInput input, ErrorCode expected) {
		assertThatThrownBy(() -> validator.validate(input))
				.isInstanceOfSatisfying(InvalidSimulationInputException.class,
						e -> assertThat(e.code()).isEqualTo(expected));
	}

	private static SimulationInput input(List<CardDefinition> challengerDeck, List<CardDefinition> opponentDeck) {
		return input(FACTIONS, BONUSES, challengerDeck, opponentDeck);
	}

	private static SimulationInput input(List<Faction> factions, List<FactionBonus> bonuses,
			List<CardDefinition> challengerDeck, List<CardDefinition> opponentDeck) {
		return new SimulationInput(42L, RULES, factions, bonuses, new PlayerInput(7, challengerDeck),
				new PlayerInput(12, opponentDeck));
	}

	private static List<CardDefinition> deck(CardDefinition... cards) {
		return Arrays.asList(cards);
	}

	private static CardDefinition creature(int cardId, Integer factionId, Integer secondaryFactionId,
			Integer attack, Integer defense) {
		return new CardDefinition(cardId, "Creature " + cardId, CardType.CREATURE, "Common", 1, factionId,
				secondaryFactionId, attack, defense, null, null);
	}
}
