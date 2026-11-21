package it.downfall.reversky.api.deck.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un mazzo di un utente con le sue carte (tabelle {@code decks} e {@code deck_cards}, docs/mazzi).
 * Il nome è univoco per utente.
 */
@Entity
@Table(name = "decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deck {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Integer userId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DeckCard> cards = new ArrayList<>();

	public Deck(Integer userId, String name) {
		this.userId = userId;
		this.name = name;
	}

	public void rename(String name) {
		this.name = name;
	}

	/** Le carte del mazzo, in sola lettura: si cambiano con {@link #replaceCards}. */
	public List<DeckCard> getCards() {
		return Collections.unmodifiableList(cards);
	}

	/** Numero totale di carte, sommando le copie. */
	public int getCardCount() {
		return cards.stream().mapToInt(DeckCard::getQuantity).sum();
	}

	/**
	 * Sostituisce il contenuto del mazzo aggiornando le righe esistenti: cambia la quantità delle carte che restano,
	 * elimina quelle tolte e aggiunge quelle nuove. Svuotare e reinserire violerebbe la chiave primaria, perché
	 * Hibernate esegue gli insert prima dei delete (docs/mazzi).
	 *
	 * @param quantities copie per carta, tutte maggiori di 0 (lo controlla la richiesta)
	 */
	public void replaceCards(Map<CatalogCard, Integer> quantities) {
		Map<Integer, CatalogCard> remaining = new LinkedHashMap<>();
		Map<Integer, Integer> quantityById = new LinkedHashMap<>();
		quantities.forEach((card, quantity) -> {
			remaining.put(card.getId(), card);
			quantityById.put(card.getId(), quantity);
		});

		Iterator<DeckCard> iterator = cards.iterator();
		while (iterator.hasNext()) {
			DeckCard row = iterator.next();
			Integer quantity = quantityById.get(row.getCardId());
			if (quantity == null) {
				iterator.remove();
			} else {
				row.changeQuantity(quantity);
				remaining.remove(row.getCardId());
			}
		}

		remaining.values().forEach(card -> cards.add(new DeckCard(this, card, quantityById.get(card.getId()))));
	}
}
