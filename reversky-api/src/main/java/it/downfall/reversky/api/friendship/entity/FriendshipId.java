package it.downfall.reversky.api.friendship.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave composta di {@code friendships} (docs/database/schema.dbml).
 * <p>
 * {@code (requester_id, recipient_id)}: chi ha inviato la richiesta e chi l'ha ricevuta.
 * Tra due utenti esiste al massimo una riga, in qualunque direzione
 * (indice {@code uq_friendships_pair} su LEAST/GREATEST).
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendshipId implements Serializable {

	@Column(name = "requester_id", nullable = false)
	private Integer requesterId;

	@Column(name = "recipient_id", nullable = false)
	private Integer recipientId;

	public FriendshipId(Integer requesterId, Integer recipientId) {
		this.requesterId = requesterId;
		this.recipientId = recipientId;
	}
}