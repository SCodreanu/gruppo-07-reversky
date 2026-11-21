package it.downfall.reversky.engine.core.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
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
 * regola 6).
 */
public final class SimulationInputValidator {

	private final EffectRegistry effects;

	/**
	 * Crea il validatore. Non cambia stato: la stessa istanza controlla qualsiasi numero di input.
	 *
	 * @param effects registro degli effetti, per il controllo {@link ErrorCode#UNKNOWN_EFFECT}
	 */
	public SimulationInputValidator(EffectRegistry effects) {
		this.effects = effects;
	}

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
				card -> "Invalid ATK or DEF on card " + card.cardId() + " (ATK at least 0, DEF at least 1)");
		checkEachCard(input, SimulationInputValidator::hasInvalidSubtype, ErrorCode.MISSING_SUBTYPE,
				card -> "Missing or invalid subtype for a " + card.type() + " card: card " + card.cardId());
		checkEachCard(input, card -> card.type() != CardType.CREATURE && isBlank(card.effectCode()),
				ErrorCode.MISSING_EFFECT, card -> "Missing effect code on card " + card.cardId());
		checkEachCard(input, this::hasUnknownEffect, ErrorCode.UNKNOWN_EFFECT, this::describeUnknownEffect);
	}

	/**
	 * Controllo 9: il codice effetto esiste nel registro, per il tipo della carta. Arriva dopo
	 * {@link ErrorCode#MISSING_EFFECT}, quindi una carta non creatura qui ha sempre un codice; una creatura può non
	 * averlo (es. le Common), ma se ce l'ha deve essere registrato (deciso il 18 novembre).
	 */
	private boolean hasUnknownEffect(CardDefinition card) {
		if (card.type() == CardType.CREATURE && isBlank(card.effectCode())) {
			return false;
		}
		return !effects.exists(card.effectCode(), card.type());
	}

	private String describeUnknownEffect(CardDefinition card) {
		String code = card.effectCode();
		return effects.appliesTo(code)
				.map(type -> "Effect " + code + " is not a " + card.type() + " effect: card " + card.cardId())
				.orElse("Unknown effect " + code + " on card " + card.cardId());
	}

	/**
	 * La gestione delle carte pescate (ENG-19, ENG-20) sceglie cosa fare dal sottotipo: un sottotipo che non
	 * esiste per quel tipo di carta prenderebbe una strada sbagliata senza errori. Le altre carte non leggono il
	 * sottotipo, quindi non si controlla.
	 */
	private static boolean hasInvalidSubtype(CardDefinition card) {
		if (card.type() == CardType.SPELL) {
			return card.subtype() == null || card.subtype() == CardSubtype.SURVIVAL;
		}
		if (card.type() == CardType.HIDDEN) {
			return card.subtype() != CardSubtype.SURVIVAL && card.subtype() != CardSubtype.DAMAGE;
		}
		return false;
	}

	/** Controllo 1: dati obbligatori presenti, mazzi non vuoti, nessuna fazione ripetuta. */
	private static void checkPresence(SimulationInput input) {
		if (input.rules() == null) {
			throw fail(ErrorCode.VALIDATION_FAILED, "Missing rules");
		}
		requirePlayer(input.challenger(), "challenger");
		requirePlayer(input.opponent(), "opponent");
		Set<Integer> seen = new HashSet<>();
		for (Faction faction : input.factions()) {
			if (!seen.add(faction.id())) {
				throw fail(ErrorCode.VALIDATION_FAILED, "Faction " + faction.id() + " is repeated");
			}
		}
		checkEachCard(input, card -> card.type() == null || card.name() == null, ErrorCode.VALIDATION_FAILED,
				card -> "Missing type or name on card " + card.cardId());
	}

	private static void requirePlayer(PlayerInput player, String role) {
		if (player == null) {
			throw fail(ErrorCode.VALIDATION_FAILED, "Missing player: " + role);
		}
		if (player.deck().isEmpty()) {
			throw fail(ErrorCode.VALIDATION_FAILED, "Empty deck: " + role);
		}
	}

	/** Controllo 2: ogni fazione citata da una carta o da un bonus è tra le fazioni ricevute. */
	private static void checkKnownFactions(SimulationInput input) {
		Set<Integer> known = new HashSet<>();
		input.factions().forEach(faction -> known.add(faction.id()));
		checkEachCard(input, card -> isUnknown(card.factionId(), known) || isUnknown(card.secondaryFactionId(), known),
				ErrorCode.UNKNOWN_FACTION, card -> "Unknown faction on card " + card.cardId());
		for (FactionBonus bonus : input.factionBonuses()) {
			if (!known.contains(bonus.originFactionId()) || !known.contains(bonus.targetFactionId())) {
				throw fail(ErrorCode.UNKNOWN_FACTION, "Unknown faction in bonus " + describe(bonus));
			}
		}
	}

	/** Controllo 3: nessun bonus di una fazione contro se stessa, nessun defBonus negativo, nessuna coppia ripetuta. */
	private static void checkFactionBonuses(List<FactionBonus> bonuses) {
		Set<FactionPair> seen = new HashSet<>();
		for (FactionBonus bonus : bonuses) {
			if (bonus.originFactionId() == bonus.targetFactionId()) {
				throw fail(ErrorCode.INVALID_FACTION_BONUS, "Bonus of a faction against itself: " + describe(bonus));
			}
			// L'atkBonus negativo è ammesso: è il malus di D-04. Il defBonus si somma alla DEF e non può toglierla.
			if (bonus.defBonus() < 0) {
				throw fail(ErrorCode.INVALID_FACTION_BONUS, "Negative defBonus in bonus " + describe(bonus));
			}
			if (!seen.add(new FactionPair(bonus.originFactionId(), bonus.targetFactionId()))) {
				throw fail(ErrorCode.INVALID_FACTION_BONUS, "Repeated pair in bonus " + describe(bonus));
			}
		}
	}

	/** Controllo 4: fazioni della carta coerenti con il suo tipo e tra loro diverse. */
	private static void checkCardFactions(SimulationInput input) {
		checkEachCard(input, SimulationInputValidator::hasInvalidFactions, ErrorCode.INVALID_CARD_FACTIONS,
				card -> "Invalid factions for a " + card.type() + " card: card " + card.cardId());
	}

	private static boolean hasInvalidFactions(CardDefinition card) {
		boolean hasAnyFaction = card.factionId() != null || card.secondaryFactionId() != null;
		if (isShared(card.type())) {
			// Spell e Hidden sono condivise tra le fazioni (docs/api-engine): nessuna fazione, nemmeno la seconda
			return hasAnyFaction;
		}
		if (card.factionId() == null || card.factionId().equals(card.secondaryFactionId())) {
			return true;
		}
		// Solo le creature possono avere due fazioni (D-22): il bonus del Terreno lavora su una fazione sola
		return card.type() != CardType.CREATURE && card.secondaryFactionId() != null;
	}

	/** Controllo 5: ogni mazzo ha almeno una creatura. */
	private static void checkCreaturesInDecks(SimulationInput input) {
		requireCreature(input.challenger(), "challenger");
		requireCreature(input.opponent(), "opponent");
	}

	private static void requireCreature(PlayerInput player, String role) {
		boolean hasCreature = player.deck().stream().anyMatch(card -> card.type() == CardType.CREATURE);
		if (!hasCreature) {
			throw fail(ErrorCode.NO_CREATURES_IN_DECK, "Deck without creatures: " + role);
		}
	}

	private static boolean hasInvalidStats(CardDefinition card) {
		if (card.type() != CardType.CREATURE) {
			// ATK e DEF di una carta non creatura si ignorano: l'engine non li legge
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

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
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
