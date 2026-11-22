package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * {@code rarityId} non valido per una carta (docs/backoffice, "Carte", regola 4).
 * <p>
 * Anche la rarità inesistente è 422 e non 404, come per {@link InvalidCardVariantException}:
 * la carta dell'URL esiste, è sbagliato un riferimento nel corpo della richiesta.
 */
public class InvalidCardRarityException extends BusinessException {

	private InvalidCardRarityException(String detail) {
		super(Category.RULE_VIOLATION, "CARD_RARITY_INVALID", "Invalid card rarity", detail);
	}

	/**
	 * La rarità indicata non esiste.
	 */
	public static InvalidCardRarityException notFound(Integer rarityId) {
		return new InvalidCardRarityException("The rarity " + rarityId + " does not exist");
	}

	/**
	 * La rarità esiste ma il tipo della carta non la ammette (Spell e Hidden solo Common o
	 * Uncommon, Terreni solo Rare).
	 */
	public static InvalidCardRarityException notAllowedForType(CardType type, Rarity rarity) {
		return new InvalidCardRarityException("The rarity " + rarity.getName() + " is not allowed for a " + type + " card");
	}
}
