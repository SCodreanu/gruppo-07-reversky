package it.downfall.reversky.api.catalog.exception;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * {@code effectCode} che non è nella tabella {@code effects}, o che c'è per un altro tipo di carta (docs/backoffice,
 * regola 2, API-36): una carta così farebbe fallire ogni partita con {@code 400 UNKNOWN_EFFECT} dell'engine.
 * <p>
 * Si crea con i metodi statici, uno per caso, così i messaggi restano uguali ovunque.
 */
public class InvalidCardEffectException extends BusinessException {

	private InvalidCardEffectException(String detail) {
		super(Category.RULE_VIOLATION, "CARD_EFFECT_INVALID", "Invalid card effect", detail);
	}

	/**
	 * Codice che non compare nella tabella {@code effects}.
	 */
	public static InvalidCardEffectException unknown(String effectCode) {
		return new InvalidCardEffectException("The effect " + effectCode + " is not implemented by the game engine");
	}

	/**
	 * Codice che esiste per un altro tipo di carta.
	 */
	public static InvalidCardEffectException wrongType(String effectCode, CardType appliesTo, CardType type) {
		return new InvalidCardEffectException(
			"The effect " + effectCode + " is for " + appliesTo + " cards, not " + type);
	}
}
