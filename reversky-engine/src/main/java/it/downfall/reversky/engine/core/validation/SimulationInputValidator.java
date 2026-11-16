package it.downfall.reversky.engine.core.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.Faction;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.rules.FactionBonus;

/**
 * Controlla che l'input di una simulazione sia coerente prima di giocare la partita (docs/api-engine, "Errori").
 * <p>
 * I controlli seguono l'ordine dei codici di {@link ErrorCode} e si ferma al primo che fallisce, così lo stesso
 * input dà sempre lo stesso errore. Dentro un controllo si guardano prima le carte dello sfidante e poi quelle
 * dell'avversario, nell'ordine del mazzo ricevuto.
 * <p>
 * L'engine non valida i mazzi secondo il regolamento (numero di carte, copie, rarità): lo fa l'API (AGENTS.md,
 * regola 6). {@link ErrorCode#UNKNOWN_EFFECT} arriva con il registro degli effetti (ENG-22).
 */
public final class SimulationInputValidator {

	/**
	 * Controlla l'input.
	 *
	 * @param input input della simulazione
	 * @throws InvalidSimulationInputException al primo controllo che fallisce, con il suo codice
	 */
	public void validate(SimulationInput input) {
		checkPresence(input);
		checkKnownFactions(input);
		checkFactionBonuses(input.factionBonuses());
		checkCardFactions(input);
		checkCreaturesInDecks(input);
		checkEachCard(input, SimulationInputValidator::hasInvalidStats, ErrorCode.INVALID_STATS,
				card -> "ATK o DEF non validi sulla carta " + card.cardId() + " (ATK almeno 0, DEF almeno 1)");
		checkEachCard(input, card -> isShared(card.type()) && card.subtype() == null, ErrorCode.MISSING_SUBTYPE,
				card -> "Sottotipo mancante sulla carta " + card.cardId());
		checkEachCard(input, card -> card.type() != CardType.CREATURE && card.effectCode() == null,
				ErrorCode.MISSING_EFFECT, card -> "Codice effetto mancante sulla carta " + card.cardId());
	}

	/** Controllo 1: dati obbligatori presenti, mazzi non vuoti, nessuna fazione ripetuta. */
	private static void checkPresence(SimulationInput input) {
		if (input.rules() == null) {
			throw fail(ErrorCode.VALIDATION_FAILED, "Regole mancanti");
		}
		requirePlayer(input.challenger(), "sfidante");
		requirePlayer(input.opponent(), "avversario");
		Set<Integer> seen = new HashSet<>();
		for (Faction faction : input.factions()) {
			if (!seen.add(faction.id())) {
				throw fail(ErrorCode.VALIDATION_FAILED, "Fazione " + faction.id() + " ripetuta");
			}
		}
		checkEachCard(input, card -> card.type() == null || card.name() == null, ErrorCode.VALIDATION_FAILED,
				card -> "Tipo o nome mancante sulla carta " + card.cardId());
	}

	private static void requirePlayer(PlayerInput player, String role) {
		if (player == null) {
			throw fail(ErrorCode.VALIDATION_FAILED, "Giocatore mancante: " + role);
		}
		if (player.deck().isEmpty()) {
			throw fail(ErrorCode.VALIDATION_FAILED, "Mazzo vuoto: " + role);
		}
	}

	/** Controllo 2: ogni fazione citata da una carta o da un bonus è tra le fazioni ricevute. */
	private static void checkKnownFactions(SimulationInput input) {
		Set<Integer> known = new HashSet<>();
		input.factions().forEach(faction -> known.add(faction.id()));
		checkEachCard(input, card -> isUnknown(card.factionId(), known) || isUnknown(card.secondaryFactionId(), known),
				ErrorCode.UNKNOWN_FACTION, card -> "Fazione sconosciuta sulla carta " + card.cardId());
		for (FactionBonus bonus : input.factionBonuses()) {
			if (!known.contains(bonus.originFactionId()) || !known.contains(bonus.targetFactionId())) {
				throw fail(ErrorCode.UNKNOWN_FACTION, "Fazione sconosciuta nel bonus " + describe(bonus));
			}
		}
	}

	/** Controllo 3: nessun bonus di una fazione contro se stessa, nessun defBonus negativo, nessuna coppia ripetuta. */
	private static void checkFactionBonuses(List<FactionBonus> bonuses) {
		Set<FactionPair> seen = new HashSet<>();
		for (FactionBonus bonus : bonuses) {
			if (bonus.originFactionId() == bonus.targetFactionId()) {
				throw fail(ErrorCode.INVALID_FACTION_BONUS, "Bonus di una fazione contro se stessa: " + describe(bonus));
			}
			// L'atkBonus negativo è ammesso: è il malus di D-04. Il defBonus si somma alla DEF e non può toglierla.
			if (bonus.defBonus() < 0) {
				throw fail(ErrorCode.INVALID_FACTION_BONUS, "defBonus negativo nel bonus " + describe(bonus));
			}
			if (!seen.add(new FactionPair(bonus.originFactionId(), bonus.targetFactionId()))) {
				throw fail(ErrorCode.INVALID_FACTION_BONUS, "Coppia ripetuta nel bonus " + describe(bonus));
			}
		}
	}

	/** Controllo 4: fazioni della carta coerenti con il suo tipo e tra loro diverse. */
	private static void checkCardFactions(SimulationInput input) {
		checkEachCard(input, SimulationInputValidator::hasInvalidFactions, ErrorCode.INVALID_CARD_FACTIONS,
				card -> "Fazioni non valide per una carta " + card.type() + ": carta " + card.cardId());
	}

	private static boolean hasInvalidFactions(CardDefinition card) {
		boolean hasAnyFaction = card.factionId() != null || card.secondaryFactionId() != null;
		if (isShared(card.type())) {
			// Spell e Hidden sono condivise tra le fazioni (docs/api-engine): nessuna fazione, nemmeno la seconda
			return hasAnyFaction;
		}
		return card.factionId() == null || card.factionId().equals(card.secondaryFactionId());
	}

	/** Controllo 5: ogni mazzo ha almeno una creatura. */
	private static void checkCreaturesInDecks(SimulationInput input) {
		requireCreature(input.challenger(), "sfidante");
		requireCreature(input.opponent(), "avversario");
	}

	private static void requireCreature(PlayerInput player, String role) {
		boolean hasCreature = player.deck().stream().anyMatch(card -> card.type() == CardType.CREATURE);
		if (!hasCreature) {
			throw fail(ErrorCode.NO_CREATURES_IN_DECK, "Mazzo senza creature: " + role);
		}
	}

	private static boolean hasInvalidStats(CardDefinition card) {
		if (card.type() != CardType.CREATURE) {
			return false;
		}
		return card.attack() == null || card.defense() == null || card.attack() < 0 || card.defense() < 1;
	}

	/**
	 * Applica un controllo a ogni carta, prima del mazzo dello sfidante e poi di quello dell'avversario, e si ferma
	 * alla prima carta non valida.
	 */
	private static void checkEachCard(SimulationInput input, Predicate<CardDefinition> isInvalid, ErrorCode code,
			Function<CardDefinition, String> detail) {
		Stream.concat(input.challenger().deck().stream(), input.opponent().deck().stream())
				.filter(isInvalid)
				.findFirst()
				.ifPresent(card -> {
					throw fail(code, detail.apply(card));
				});
	}

	private static boolean isShared(CardType type) {
		return type == CardType.SPELL || type == CardType.HIDDEN;
	}

	private static boolean isUnknown(Integer factionId, Set<Integer> known) {
		return factionId != null && !known.contains(factionId);
	}

	private static String describe(FactionBonus bonus) {
		return bonus.originFactionId() + " → " + bonus.targetFactionId();
	}

	private static InvalidSimulationInputException fail(ErrorCode code, String detail) {
		return new InvalidSimulationInputException(code, detail);
	}

	/** Coppia ordinata (origine, destinazione) per trovare le righe di bonus ripetute. */
	private record FactionPair(int origin, int target) {
	}
}
