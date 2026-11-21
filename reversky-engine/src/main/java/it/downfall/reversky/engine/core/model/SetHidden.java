package it.downfall.reversky.engine.core.model;

import java.util.Objects;

/**
 * Una Hidden coperta nella zona Hidden di un giocatore (D-15).
 * <p>
 * Le Hidden senza condizione si attivano all'inizio del prossimo scontro del proprietario; una Hidden della pescata
 * iniziale salta il primo scontro. Per questo la carta ricorda da quale scontro può attivarsi: gli scontri si contano
 * da 1 con {@link MatchState#combatsStarted()}.
 *
 * @param card            carta della Hidden
 * @param readyFromCombat primo scontro in cui può attivarsi, almeno 1
 */
public record SetHidden(CardDefinition card, int readyFromCombat) {

	/**
	 * Controlla i valori.
	 *
	 * @throws IllegalArgumentException se la carta non è una Hidden o lo scontro è minore di 1
	 * @throws NullPointerException     se manca la carta
	 */
	public SetHidden {
		Objects.requireNonNull(card, "card");
		if (card.type() != CardType.HIDDEN) {
			throw new IllegalArgumentException("Non è una Hidden: carta " + card.cardId());
		}
		if (readyFromCombat < 1) {
			throw new IllegalArgumentException("Gli scontri si contano da 1: " + readyFromCombat);
		}
	}

	/**
	 * Indica se la Hidden può attivarsi in uno scontro.
	 *
	 * @param combat numero dello scontro che sta iniziando
	 * @return {@code true} se è il suo scontro o uno successivo
	 */
	public boolean isReadyIn(int combat) {
		return combat >= readyFromCombat;
	}
}
