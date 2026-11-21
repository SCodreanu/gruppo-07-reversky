package it.downfall.reversky.api.deck.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave di {@code deck_cards}: mazzo e carta.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckCardId implements Serializable {

	@Column(name = "deck_id", nullable = false)
	private Integer deckId;

	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	public DeckCardId(Integer deckId, Integer cardId) {
		this.deckId = deckId;
		this.cardId = cardId;
	}
}
