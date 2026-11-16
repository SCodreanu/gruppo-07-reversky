package it.downfall.reversky.engine.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.Faction;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;

/**
 * Test di {@link SimulationInputValidator}: un test per ogni codice di errore di docs/api-engine ("Errori") e per
 * l'ordine dei controlli, così lo stesso input dà sempre lo stesso errore. {@code UNKNOWN_EFFECT} arriva con
 * ENG-22, che crea il registro degli effetti.
 */
class SimulationInputValidatorTest {

	private static final int WOUND = 1;
	private static final int INVITATION = 2;
	private static final int ARCHIVE = 3;
	private static final int DREAM = 4;
	private static final int UNKNOWN = 99;

	private static final List<Faction> FACTIONS = List.of(
			new Faction(WOUND, "Theory of the Wound"),
			new Faction(INVITATION, "Theory of the Invitation"),
			new Faction(ARCHIVE, "Theory of the Archive"),
			new Faction(DREAM, "Theory of the Dream"));
	private static final List<FactionBonus> BONUSES = List.of(
			new FactionBonus(WOUND, DREAM, 2, 0),
			new FactionBonus(DREAM, WOUND, -1, 0));
	private static final RuleConfig RULES = new RuleConfig("1.0", 30, 75);

	private static final CardDefinition CRAWLERS = creature(4, WOUND, null, 4, 5);
	private static final CardDefinition KAPPA = creature(401, DREAM, null, 6, 7);
	private static final CardDefinition VEIL = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL,
			"Uncommon", 2, null, null, null, null, CardSubtype.SHIELD, "VEIL_OF_ANCHORED");
	private static final CardDefinition LAST_SANCTUARY = new CardDefinition(80, "Last Sanctuary", CardType.HIDDEN,
			"Rare", 3, null, null, null, null, CardSubtype.SURVIVAL, "LAST_SANCTUARY");
	private static final CardDefinition ECHO_RIFT = new CardDefinition(90, "Echo Rift", CardType.TERRAIN,
			"Rare", 3, WOUND, null, null, null, null, "ECHO_RIFT");
	private static final CardDefinition TORII_BELL = new CardDefinition(95, "Campanello del Torii", CardType.ITEM,
			"Rare", 3, DREAM, null, null, null, null, "TORII_BELL");

	private final SimulationInputValidator validator = new SimulationInputValidator();

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
	@DisplayName("Una creatura senza effectCode è valida: per le creature è facoltativo")
	void validate_creatureWithoutEffect_doesNotThrow() {
		assertThatCode(() -> validator.validate(input(deck(CRAWLERS), deck(KAPPA)))).doesNotThrowAnyException();
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
				.hasMessageContaining("carta 11");
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
