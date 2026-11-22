package it.downfall.reversky.api.inventory.entity;

/**
 * Motivo di un movimento di crediti (enum PostgreSQL {@code credit_reason}, docs/crediti).
 */
public enum CreditReason {

	/** Crediti di benvenuto alla registrazione, dal rule set attivo. */
	SIGNUP,

	/** Vittoria in classificata, dal rule set della partita. */
	BATTLE_WIN,

	/** Pareggio in classificata, dal rule set della partita. */
	BATTLE_DRAW,

	/** Acquisto di pacchetti: importo negativo, oppure 0 per un pacchetto gratuito. */
	PACK_PURCHASE,

	/** Correzione dal backoffice, positiva o negativa. */
	ADMIN_ADJUSTMENT
}
