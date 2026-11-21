package it.downfall.reversky.api.collection.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave composta di {@code collection_cards} (docs/database/schema.dbml): una riga per utente e carta.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionCardId implements Serializable {

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	public CollectionCardId(Integer userId, Integer cardId) {
		this.userId = userId;
		this.cardId = cardId;
	}
}
