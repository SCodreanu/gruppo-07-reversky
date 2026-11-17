package it.downfall.reversky.engine.core.model;

import java.util.Objects;

/**
 * Scudo da dare all'inizio di ognuno dei prossimi round di uno scontro (Bussola dei Sentieri Falsi): ogni round lo
 * scudo torna al suo valore e dura solo quel round, quindi non si somma a quello rimasto (D-35).
 *
 * @param source origine dello scudo
 * @param side   lato che l'ha causato
 * @param amount DEF dello scudo, almeno 1
 * @param rounds round in cui dare ancora lo scudo, almeno 1
 */
public record RoundStartShield(ModifierSource source, Side side, int amount, int rounds) {

	/**
	 * Controlla i valori.
	 *
	 * @throws IllegalArgumentException se lo scudo o i round sono minori di 1
	 * @throws NullPointerException     se manca l'origine o il lato
	 */
	public RoundStartShield {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(side, "side");
		if (amount < 1 || rounds < 1) {
			throw new IllegalArgumentException("Scudo e round almeno 1: " + amount + ", " + rounds);
		}
	}

	/**
	 * Il programma dopo aver dato lo scudo di questo round.
	 *
	 * @return il programma per i round rimasti, o {@code null} se era l'ultimo
	 */
	RoundStartShield next() {
		return rounds == 1 ? null : new RoundStartShield(source, side, amount, rounds - 1);
	}
}
