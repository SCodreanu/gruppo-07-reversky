package it.downfall.reversky.api.trade.entity;

/**
 * Stato di uno scambio (enum PostgreSQL {@code trade_status}, V1, docs/scambi "Stati e turni").
 * <p>
 * I due stati aperti dicono anche di chi è il turno: {@code WAITING_RECIPIENT} tocca al destinatario,
 * {@code WAITING_REQUESTER} al richiedente. {@code ACCEPTED} e {@code REJECTED} sono finali.
 */
public enum TradeStatus {
	WAITING_RECIPIENT,
	WAITING_REQUESTER,
	ACCEPTED,
	REJECTED
}
