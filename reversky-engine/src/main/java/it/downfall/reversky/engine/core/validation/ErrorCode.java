package it.downfall.reversky.engine.core.validation;

/**
 * Codici di errore di un input non valido: sono il campo {@code code} del {@code ProblemDetail} restituito con
 * lo stato {@code 400} (docs/api-engine, "Errori").
 * <p>
 * I valori sono nell'ordine in cui si controllano: si restituisce il primo che fallisce, così lo stesso input dà
 * sempre lo stesso errore.
 */
public enum ErrorCode {

	/**
	 * 1. Dato mancante o fuori dai limiti: regole mancanti o con valori minori di 1, giocatore mancante, mazzo
	 * vuoto, carta senza tipo o senza nome, fazione ripetuta. Nello strato {@code web} lo segnala anche la Bean
	 * Validation.
	 */
	VALIDATION_FAILED,

	/** 2. Una fazione di una carta o di un bonus non è tra le fazioni ricevute. */
	UNKNOWN_FACTION,

	/** 3. Bonus di una fazione contro se stessa, {@code defBonus} negativo o coppia ripetuta. */
	INVALID_FACTION_BONUS,

	/**
	 * 4. Seconda fazione uguale alla principale; Creatura, Terreno od Oggetto senza fazione; Spell o Hidden con una
	 * fazione.
	 */
	INVALID_CARD_FACTIONS,

	/** 5. Un mazzo senza creature. */
	NO_CREATURES_IN_DECK,

	/** 6. Creatura senza ATK o DEF, con ATK minore di 0 o DEF minore di 1. */
	INVALID_STATS,

	/** 7. Spell o Hidden senza sottotipo. */
	MISSING_SUBTYPE,

	/** 8. Spell, Hidden, Terreno od Oggetto senza codice effetto. */
	MISSING_EFFECT,

	/** 9. Codice effetto che non esiste nel registro degli effetti (ENG-22). */
	UNKNOWN_EFFECT
}
