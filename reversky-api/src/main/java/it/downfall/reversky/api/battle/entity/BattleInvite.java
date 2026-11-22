package it.downfall.reversky.api.battle.entity;

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
 * Invito a una sfida amichevole (tabella {@code battle_invites}, docs/sfide).
 * <p>
 * L'id è un UUID generato qui, per non rendere indovinabili gli inviti degli altri.
 */
@Entity
@Table(name = "battle_invites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleInvite implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column(name = "challenger_id", nullable = false, updatable = false)
	private Integer challengerId;

	/** Diventa null se il mazzo viene eliminato dopo l'accettazione (ON DELETE SET NULL). */
	@Column(name = "challenger_deck_id")
	private Integer challengerDeckId;

	@Column(name = "opponent_id", nullable = false, updatable = false)
	private Integer opponentId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private BattleInviteStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	public BattleInvite(Integer challengerId, Integer challengerDeckId, Integer opponentId, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.challengerId = challengerId;
		this.challengerDeckId = challengerDeckId;
		this.opponentId = opponentId;
		this.status = BattleInviteStatus.PENDING;
		this.createdAt = createdAt;
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

	public boolean isPending() {
		return status == BattleInviteStatus.PENDING;
	}

	public void accept() {
		this.status = BattleInviteStatus.ACCEPTED;
	}
}
