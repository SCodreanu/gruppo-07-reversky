package it.downfall.reversky.engine.core.effect;

/**
 * Effetto di un Oggetto: resta accanto al mazzo finché scatta una volta sulla prima creatura del proprietario che ne
 * soddisfa la condizione, poi si consuma (D-47).
 * <p>
 * Per ora serve solo a registrare i codici, così {@code UNKNOWN_EFFECT} vale anche per gli Oggetti: i momenti in cui
 * un Oggetto scatta arrivano con ENG-33.
 */
public non-sealed interface ItemEffect extends CardEffect {
}
