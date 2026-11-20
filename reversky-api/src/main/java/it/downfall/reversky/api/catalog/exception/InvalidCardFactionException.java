package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * {@code factionId} o {@code secondaryFactionId} di una carta che non esiste (docs/backoffice, "Carte").
 * <p>
 * È 422 e non 404, come per la rarità ({@link InvalidCardRarityException}): la carta dell'URL esiste, è sbagliato
 * un riferimento nel corpo della richiesta. Le regole che legano le fazioni al tipo sono invece
 * {@link CatalogCardRulesViolatedException}.
 */
public class InvalidCardFactionException extends BusinessException {

	private InvalidCardFactionException(String detail) {
		super(Category.RULE_VIOLATION, "CARD_FACTION_INVALID", "Invalid card faction", detail);
	}

	/**
	 * La fazione indicata, prima o seconda, non esiste.
	 */
	public static InvalidCardFactionException notFound(Integer factionId) {
		return new InvalidCardFactionException("The faction " + factionId + " does not exist");
	}
}
