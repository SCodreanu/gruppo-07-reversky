package it.downfall.reversky.engine.core.log;

/**
 * Tipo di un evento del log di partita (docs/log-partita, "Tipi di evento"). I nomi sono le stringhe del
 * contratto JSON: l'API li salva in {@code battles.rounds_log}.
 */
public enum EventType {

	/** Inizio della partita, con i punti vita iniziali. */
	MATCH_STARTED,

	/** Un giocatore pesca una Spell, una Hidden, un Terreno o un Oggetto (per le creature c'è CREATURE_ENTERED). */
	CARD_DRAWN,

	/** Una creatura entra in campo. */
	CREATURE_ENTERED,

	/** Una Spell viene risolta e scartata. */
	SPELL_RESOLVED,

	/** Una Spell BURN rivela la prossima carta del mazzo (D-13). */
	BURN_REVEALED,

	/** Una Hidden viene messa coperta. */
	HIDDEN_SET,

	/** Una Hidden si attiva. */
	HIDDEN_TRIGGERED,

	/** Un Terreno entra in gioco e sostituisce quello già in gioco (D-19). */
	TERRAIN_PLACED,

	/** Un effetto toglie il Terreno in gioco. */
	TERRAIN_REMOVED,

	/** Un effetto sposta una carta nel mazzo di un giocatore. */
	DECK_REORDERED,

	/** Un Oggetto entra in gioco. */
	ITEM_PLACED,

	/** Un Oggetto applica il suo effetto e si consuma (D-47). */
	ITEM_CONSUMED,

	/** Inizio di un round. */
	ROUND_STARTED,

	/** Calcolo dell'attacco di una creatura. */
	ATTACK_COMPUTED,

	/** Una creatura colpisce l'altra. */
	DAMAGE_DEALT,

	/** Un'abilità agisce. */
	EFFECT_TRIGGERED,

	/** Una creatura muore. */
	CREATURE_DIED,

	/** Un giocatore perde punti vita. */
	PLAYER_DAMAGED,

	/** Fine della partita, con vincitore e motivo. */
	MATCH_ENDED
}
