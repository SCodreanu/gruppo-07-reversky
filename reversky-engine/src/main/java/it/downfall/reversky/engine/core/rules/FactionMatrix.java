package it.downfall.reversky.engine.core.rules;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Bonus di fazione di una partita, costruiti dalle righe {@code factionBonuses} della richiesta (docs/fazioni).
 * <p>
 * Lavora su liste di fazioni, una o due per carta ({@code CardDefinition.factions()}): il bonus è la somma delle
 * righe di tutte le coppie (fazione di chi riceve il bonus, fazione affrontata), così le carte con due fazioni
 * prendono vantaggi e svantaggi di entrambe (D-22). Una coppia senza riga vale 0.
 * <p>
 * Il bonus restituito può essere negativo (malus, D-04): il minimo 0 dell'attacco effettivo si applica nel
 * calcolo del round, dopo tutti i modificatori.
 */
public final class FactionMatrix {

	private final Map<FactionPair, FactionBonus> bonuses;

	/**
	 * Costruisce la matrice. Le righe arrivano già controllate dal validatore dell'input (nessuna coppia
	 * ripetuta): una coppia ripetuta qui è un bug e lancia {@link IllegalStateException}.
	 *
	 * @param bonusRows righe della tabella {@code faction_bonuses}, anche vuota
	 */
	public FactionMatrix(List<FactionBonus> bonusRows) {
		// La mappa si usa solo per le ricerche per chiave, mai per iterare: l'ordine non conta (docs/mescolamento)
		this.bonuses = bonusRows.stream()
				.collect(Collectors.toMap(
						row -> new FactionPair(row.originFactionId(), row.targetFactionId()),
						Function.identity()));
	}

	/**
	 * Bonus ATK di una carta contro un'altra, che si somma all'attacco in ogni round dello scontro.
	 *
	 * @param attackerFactions fazioni di chi attacca, nell'ordine di {@code CardDefinition.factions()}
	 * @param defenderFactions fazioni di chi viene attaccato
	 * @return somma degli {@code atkBonus} di tutte le coppie; può essere negativa
	 */
	public int atkBonus(List<Integer> attackerFactions, List<Integer> defenderFactions) {
		return sum(attackerFactions, defenderFactions, FactionBonus::atkBonus);
	}

	/**
	 * Bonus DEF di una carta contro la sua avversaria, che si somma alla DEF una volta, all'inizio dello scontro.
	 *
	 * @param ownFactions      fazioni della carta che riceve il bonus
	 * @param opponentFactions fazioni della carta affrontata
	 * @return somma dei {@code defBonus} di tutte le coppie; mai negativa, perché il validatore rifiuta i
	 *         {@code defBonus} negativi
	 */
	public int defBonus(List<Integer> ownFactions, List<Integer> opponentFactions) {
		return sum(ownFactions, opponentFactions, FactionBonus::defBonus);
	}

	private int sum(List<Integer> originFactions, List<Integer> targetFactions, ToIntFunction<FactionBonus> value) {
		int total = 0;
		for (int origin : originFactions) {
			for (int target : targetFactions) {
				FactionBonus row = bonuses.get(new FactionPair(origin, target));
				total += row == null ? 0 : value.applyAsInt(row);
			}
		}
		return total;
	}

	/** Coppia ordinata (fazione che riceve il bonus, fazione affrontata): chiave della mappa dei bonus. */
	private record FactionPair(int origin, int target) {
	}
}
