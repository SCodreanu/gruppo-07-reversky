package it.downfall.reversky.api.notification.entity;

/**
 * Tipi di notifica (docs/notifiche). La colonna {@code notifications.type} è testo e non un enum del database: un tipo
 * nuovo si aggiunge qui senza una migrazione. Il nome è anche il nome dell'evento sul canale SSE.
 */
public enum NotificationType {

	/** Al destinatario di una richiesta di amicizia (API-67). */
	FRIEND_REQUEST_RECEIVED,

	/** A chi ha inviato la richiesta di amicizia, quando viene accettata (API-67). */
	FRIEND_REQUEST_ACCEPTED,

	/** All'avversario invitato a una sfida amichevole (API-68). */
	BATTLE_INVITE_RECEIVED,

	/** Allo sfidante, quando l'avversario accetta l'invito (API-68). */
	BATTLE_INVITE_ACCEPTED,

	/** Ai due giocatori abbinati dalla coda classificata (API-68). */
	MATCH_FOUND,

	/** Ai due giocatori, quando la partita ha un risultato, anche dopo un retry (API-68). */
	BATTLE_COMPLETED,

	/** A chi riceve una proposta di scambio (API-69). */
	TRADE_PROPOSED,

	/** A chi deve rispondere a una controproposta (API-69). */
	TRADE_COUNTERED,

	/** All'altra parte, quando lo scambio viene accettato (API-69). */
	TRADE_ACCEPTED,

	/** All'altra parte, quando lo scambio viene rifiutato (API-69). */
	TRADE_REJECTED
}
