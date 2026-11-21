package it.downfall.reversky.api.deck.entity;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una carta di un mazzo con le sue copie (tabella {@code deck_cards}, docs/mazzi).
 * <p>
 * Le righe si creano, modificano ed eliminano solo da {@link Deck#replaceCards}, che aggiorna quelle esistenti
 * invece di svuotare e reinserire.
 */
@Entity
@Table(name = "deck_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckCard {

	@EmbeddedId
	private DeckCardId id;

	@MapsId("deckId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "deck_id", nullable = false)
	private Deck deck;

	@MapsId("cardId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "card_id", nullable = false)
	private CatalogCard card;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	// Per un mazzo nuovo l'id del mazzo non esiste ancora: lo riempie @MapsId al salvataggio
	DeckCard(Deck deck, CatalogCard card, int quantity) {
		this.id = new DeckCardId(deck.getId(), card.getId());
		this.deck = deck;
		this.card = card;
		this.quantity = quantity;
	}

	void changeQuantity(int quantity) {
		this.quantity = quantity;
	}

	/** Comodità: l'id composto non serve chiamarlo ogni volta. */
	public Integer getCardId() {
		return id.getCardId();
	}
}
