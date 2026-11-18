package it.downfall.reversky.engine.core.effect;

/**
 * Effetto di una Hidden: la carta resta coperta nella zona Hidden del proprietario e si attiva una volta sola, poi si
 * scarta (D-15).
 * <p>
 * Le Hidden senza condizione si attivano all'inizio del prossimo scontro del proprietario (ENG-20); le condizioni di
 * Remnants of Reality e Unseen Exit arrivano con i loro effetti ({@code P2}).
 */
public non-sealed interface HiddenEffect extends CardEffect {

	/**
	 * Attiva la Hidden.
	 *
	 * @param ctx contesto con la partita, il proprietario e la carta
	 */
	void trigger(EffectContext ctx);
}
