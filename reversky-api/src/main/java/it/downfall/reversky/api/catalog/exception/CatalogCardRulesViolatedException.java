package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Campi di gioco non coerenti con il tipo della carta (docs/backoffice, "Carte"): sono le stesse
 * regole dei vincoli CHECK di V5, controllate prima per rispondere 422 con un messaggio chiaro.
 * <p>
 * Si crea con i metodi statici, uno per regola, così i messaggi restano uguali ovunque.
 */
public class CatalogCardRulesViolatedException extends BusinessException {

	private CatalogCardRulesViolatedException(String detail) {
		super(Category.RULE_VIOLATION, "CARD_RULES_VIOLATED", "Card rules violated", detail);
	}

	/**
	 * Creatura senza ATK o senza DEF.
	 */
	public static CatalogCardRulesViolatedException missingStats() {
		return new CatalogCardRulesViolatedException("A creature must have both ATK and DEF");
	}

	/**
	 * ATK o DEF su una carta che non è una creatura.
	 */
	public static CatalogCardRulesViolatedException unexpectedStats(CardType type) {
		return new CatalogCardRulesViolatedException("A " + type + " card must not have ATK or DEF");
	}

	/**
	 * Sottotipo mancante, non ammesso per il tipo, oppure presente su un tipo che non ne ha.
	 */
	public static CatalogCardRulesViolatedException invalidSubtype(CardType type, CardSubtype subtype) {
		if (subtype == null) {
			return new CatalogCardRulesViolatedException("A " + type + " card must have a subtype");
		}

		return new CatalogCardRulesViolatedException("The subtype " + subtype + " is not valid for a " + type + " card");
	}

	/**
	 * Effetto mancante su un tipo che lo richiede.
	 */
	public static CatalogCardRulesViolatedException missingEffect(CardType type) {
		return new CatalogCardRulesViolatedException("A " + type + " card must have an effect code");
	}
}
