package it.downfall.reversky.api.collection.entity;

import org.springframework.data.domain.Persistable;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Copie di una carta possedute da un utente (docs/collezione). La scrive solo
 * {@link it.downfall.reversky.api.collection.service.CollectionService}.
 * <p>
 * {@code quantity} è sempre maggiore di 0 ({@code chk_collection_cards_quantity_positive}): a 0 il service elimina la
 * riga. L'utente resta un id nella chiave, senza relazione: la collezione non ha bisogno dei suoi dati.
 * <p>
 * Come {@code FactionBonus} implementa {@link Persistable}: con la chiave assegnata a mano, {@code save} di una riga
 * nuova fa subito l'insert invece di cercarla prima.
 */
@Entity
@Table(name = "collection_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionCard implements Persistable<CollectionCardId> {

	@EmbeddedId
	private CollectionCardId id;

	@MapsId("cardId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "card_id", nullable = false)
	private CatalogCard card;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/**
	 * Riga nuova. La carta può essere un riferimento senza dati ({@code getReferenceById}): serve solo il suo id.
	 */
	public CollectionCard(Integer userId, CatalogCard card, int quantity) {
		this.id = new CollectionCardId(userId, card.getId());
		this.card = card;
		this.quantity = quantity;
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

	/** Aggiunge copie; il service controlla che siano più di 0. */
	public void add(int copies) {
		this.quantity += copies;
	}

	/** Toglie copie; il service controlla prima che bastino ed elimina la riga a 0. */
	public void remove(int copies) {
		this.quantity -= copies;
	}

	/** Comodità: l'id composto non serve chiamarlo ogni volta. */
	public Integer getCardId() {
		return id.getCardId();
	}
}
