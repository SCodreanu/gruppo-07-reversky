package it.downfall.reversky.engine.core.model;

/**
 * Durata di un modificatore o di una sua sospensione (D-35).
 *
 * @param kind   tipo di durata
 * @param rounds round rimasti per {@link Kind#ROUNDS}, almeno 1; 0 per gli altri tipi
 */
public record ModifierDuration(Kind kind, int rounds) {

	/** Tipi di durata. */
	public enum Kind {

		/** Finché la creatura resta in campo, anche negli scontri successivi (D-12). */
		WHILE_IN_PLAY,

		/** Fino alla fine dello scontro corrente, cioè finché una delle due creature muore. */
		UNTIL_COMBAT_END,

		/**
		 * Per un numero di round: il contatore scende di 1 alla fine di ogni round risolto (dopo il passo 6), quindi
		 * {@code rounds(1)} vale esattamente per il prossimo round. Non dipende dallo scontro.
		 */
		ROUNDS
	}

	/**
	 * Controlla che il numero di round sia coerente con il tipo.
	 *
	 * @throws IllegalArgumentException se {@code ROUNDS} ha meno di 1 round o un altro tipo ha dei round
	 */
	public ModifierDuration {
		if (kind == Kind.ROUNDS ? rounds < 1 : rounds != 0) {
			throw new IllegalArgumentException("Durata non valida: " + kind + " con " + rounds + " round");
		}
	}

	/**
	 * Durata finché la creatura resta in campo.
	 *
	 * @return la durata
	 */
	public static ModifierDuration whileInPlay() {
		return new ModifierDuration(Kind.WHILE_IN_PLAY, 0);
	}

	/**
	 * Durata fino alla fine dello scontro corrente.
	 *
	 * @return la durata
	 */
	public static ModifierDuration untilCombatEnd() {
		return new ModifierDuration(Kind.UNTIL_COMBAT_END, 0);
	}

	/**
	 * Durata di un numero di round.
	 *
	 * @param rounds numero di round, almeno 1
	 * @return la durata
	 */
	public static ModifierDuration rounds(int rounds) {
		return new ModifierDuration(Kind.ROUNDS, rounds);
	}
}
