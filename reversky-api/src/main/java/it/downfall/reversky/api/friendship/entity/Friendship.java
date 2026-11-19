package it.downfall.reversky.api.friendship.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
 * <p>
 * Implementa {@link Persistable}: la chiave è assegnata dal codice, quindi senza {@link #isNew()}
 * Spring Data considererebbe ogni nuova riga "già esistente" e farebbe {@code merge} invece di
 * {@code persist}. Con il merge una richiesta doppia arrivata nello stesso momento troverebbe la
 * riga appena salvata dall'altra transazione e la aggiornerebbe (200) invece di fallire con il
 * vincolo della chiave primaria (409).
 */
@Entity
@Table(name = "friendships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship implements Persistable<FriendshipId> {

	@EmbeddedId
	private FriendshipId id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private FriendshipStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	public Friendship(Integer requesterId, Integer recipientId) {
		this.id = new FriendshipId(requesterId, recipientId);
		this.status = FriendshipStatus.PENDING;
		// TODO(@acistar0): usare il Clock invece di Instant.now(), come AuthService e RuleSetService.
		// Aggiungere un parametro "Instant createdAt" a questo costruttore; in FriendshipService
		// iniettare "private final Clock clock" (il bean è in common/config/TimeConfig) e chiamare
		// new Friendship(currentUserId, target.getId(), Instant.now(clock)). Così i test possono
		// fissare l'ora con un Clock fisso.
		this.createdAt = Instant.now();
	}

	/** Rende amici: si chiama dopo l'accettazione o dopo una richiesta inversa. */
	public void accept() {
		this.status = FriendshipStatus.ACCEPTED;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	/** Una riga letta dal database o appena salvata non è più nuova. */
	@PostLoad
	@PostPersist
	void markNotNew() {
		this.newEntity = false;
	}
}