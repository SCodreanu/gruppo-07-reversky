package it.downfall.reversky.api.friendship.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Relazione tra due utenti (tabella {@code friendships}, docs/amicizie).
 * <p>
 * Al massimo una riga per coppia, in qualunque direzione (vincolo {@code uq_friendships_pair}).
 * {@code requesterId} è chi ha inviato la richiesta, {@code recipientId} chi l'ha ricevuta.
 * <p>
 * {@code status} è l'enum PostgreSQL nativo {@code friendship_status} (definito in V1):
 * {@code @JdbcTypeCode(SqlTypes.NAMED_ENUM)} dice a Hibernate di usare il tipo nativo invece di
 * mandare un VARCHAR, che PostgreSQL rifiuterebbe con "column is of type friendship_status".
 */
@Entity
@Table(name = "friendships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

	@EmbeddedId
	private FriendshipId id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private FriendshipStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Friendship(Integer requesterId, Integer recipientId) {
		this.id = new FriendshipId(requesterId, recipientId);
		this.status = FriendshipStatus.PENDING;
		this.createdAt = Instant.now();
	}

	/** Rende amici: si chiama dopo l'accettazione o dopo una richiesta inversa. */
	public void accept() {
		this.status = FriendshipStatus.ACCEPTED;
	}
}