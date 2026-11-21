package it.downfall.reversky.api.battle.entity;

/**
 * Stato di un invito di sfida (enum PostgreSQL {@code battle_invite_status}, docs/sfide). Rifiuto e ritiro eliminano
 * la riga, quindi non hanno uno stato.
 */
public enum BattleInviteStatus {

	/** In attesa della risposta dell'avversario. */
	PENDING,

	/** Accettato: esiste la partita in {@code battles}. */
	ACCEPTED
}
