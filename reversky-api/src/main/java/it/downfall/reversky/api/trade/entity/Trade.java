package it.downfall.reversky.api.trade.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uno scambio di carte tra due utenti (tabella {@code trades}, docs/scambi).
 * <p>
 * Lo stato dice anche di chi è il turno; gli scambi non scadono e restano aperti finché uno dei due non li chiude.
 * L'id è un UUID generato qui, per non rendere indovinabili gli scambi degli altri.
 */
@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column(name = "requester_id", nullable = false, updatable = false)
	private Integer requesterId;

	@Column(name = "recipient_id", nullable = false, updatable = false)
	private Integer recipientId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private TradeStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Ultima proposta, controproposta o risposta: ordina le liste. */
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/**
	 * Una proposta nuova: tocca al destinatario.
	 */
	public Trade(Integer requesterId, Integer recipientId, Instant now) {
		this.id = UUID.randomUUID();
		this.requesterId = requesterId;
		this.recipientId = recipientId;
		this.status = TradeStatus.WAITING_RECIPIENT;
		this.createdAt = now;
		this.updatedAt = now;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.newEntity = false;
	}

	/** L'utente è il richiedente o il destinatario? */
	public boolean involves(Integer userId) {
		return requesterId.equals(userId) || recipientId.equals(userId);
	}

	/** Lo scambio aspetta ancora una risposta? */
	public boolean isOpen() {
		return status == TradeStatus.WAITING_RECIPIENT || status == TradeStatus.WAITING_REQUESTER;
	}

	/** Tocca a questo utente accettare o controproporre? Mai su uno scambio chiuso. */
	public boolean isTurnOf(Integer userId) {
		return (status == TradeStatus.WAITING_RECIPIENT && recipientId.equals(userId))
			|| (status == TradeStatus.WAITING_REQUESTER && requesterId.equals(userId));
	}

	/** L'altro partecipante rispetto a questo utente. */
	public Integer otherThan(Integer userId) {
		return requesterId.equals(userId) ? recipientId : requesterId;
	}

	/** Chiude lo scambio senza scambiare niente: lo può fare chiunque dei due finché è aperto. */
	public void reject(Instant now) {
		this.status = TradeStatus.REJECTED;
		this.updatedAt = now;
	}
}
