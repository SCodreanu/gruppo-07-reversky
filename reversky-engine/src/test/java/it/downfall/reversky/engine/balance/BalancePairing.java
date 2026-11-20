package it.downfall.reversky.engine.balance;

import java.util.ArrayList;
import java.util.List;

/**
 * Un accoppiamento ordinato di fazioni: la fazione del mazzo dello sfidante (che pesca per primo) e quella
 * dell'avversario.
 *
 * @param index               posizione nell'elenco di {@link #all()}, da 0; entra nel seed delle partite
 * @param challengerFactionId fazione dello sfidante
 * @param opponentFactionId   fazione dell'avversario
 */
public record BalancePairing(int index, int challengerFactionId, int opponentFactionId) {

	/**
	 * I 16 accoppiamenti ordinati delle quattro fazioni, speculari compresi, nell'ordine di
	 * {@link BalanceConfig#FACTIONS}: prima tutti quelli con lo sfidante Wound, poi Invitation, Archive, Dream.
	 *
	 * @return lista immutabile di 16 accoppiamenti
	 */
	public static List<BalancePairing> all() {
		List<BalancePairing> pairings = new ArrayList<>();
		for (int challenger : BalanceConfig.FACTIONS) {
			for (int opponent : BalanceConfig.FACTIONS) {
				pairings.add(new BalancePairing(pairings.size(), challenger, opponent));
			}
		}
		return List.copyOf(pairings);
	}

	/**
	 * Indica se le due fazioni sono la stessa: queste partite non contano per le vittorie per fazione.
	 *
	 * @return {@code true} se lo sfidante e l'avversario hanno la stessa fazione
	 */
	public boolean isMirror() {
		return challengerFactionId == opponentFactionId;
	}
}
