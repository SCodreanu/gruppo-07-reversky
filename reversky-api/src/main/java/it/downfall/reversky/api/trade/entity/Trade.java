package it.downfall.reversky.api.trade.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

	/** Le carte offerte da entrambi i lati, in sola lettura: si cambiano con {@link #replaceOffers}. */
	@OneToMany(mappedBy = "trade", cascade = CascadeType.ALL, orphanRemoval = true)
	@Getter(AccessLevel.NONE)
	private List<TradeOffer> offers = new ArrayList<>();

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

	/** Le offerte attuali, di entrambi i lati, in sola lettura. */
	public List<TradeOffer> getOffers() {
		return Collections.unmodifiableList(offers);
	}

	/**
	 * Le copie per carta che quel partecipante cede nell'offerta attuale.
	 *
	 * @return copie per id di carta, vuota se quel lato non dà niente
	 */
	public Map<Integer, Integer> offeredBy(Integer userId) {
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		offers.stream()
			.filter(offer -> offer.getUserId().equals(userId))
			.forEach(offer -> quantities.put(offer.getCardId(), offer.getQuantity()));
		return quantities;
	}

	/**
	 * Sostituisce tutte le offerte aggiornando le righe esistenti (API-32, docs/scambi "Controproposta"): cambia la
	 * quantità di quelle che restano, elimina quelle tolte e aggiunge quelle nuove. Svuotare e reinserire violerebbe la
	 * chiave primaria, perché Hibernate esegue gli insert prima dei delete.
	 *
	 * @param giverId   chi cede le carte di {@code given}
	 * @param given     copie per carta cedute da {@code giverId}
	 * @param otherGiven copie per carta cedute dall'altro partecipante
	 * @throws IllegalArgumentException se {@code giverId} non partecipa o una quantità è minore di 1 (errore del
	 *                                  chiamante: le richieste lo controllano prima)
	 */
	public void replaceOffers(Integer giverId, Map<CatalogCard, Integer> given, Map<CatalogCard, Integer> otherGiven) {
		if (!involves(giverId)) {
			throw new IllegalArgumentException("User " + giverId + " is not part of trade " + id);
		}
		Map<TradeOfferId, Integer> quantities = new LinkedHashMap<>();
		Map<TradeOfferId, CatalogCard> cards = new LinkedHashMap<>();
		collect(giverId, given, quantities, cards);
		collect(otherThan(giverId), otherGiven, quantities, cards);

		Iterator<TradeOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			TradeOffer offer = iterator.next();
			Integer quantity = quantities.get(offer.getId());
			if (quantity == null) {
				iterator.remove();
			} else {
				offer.changeQuantity(quantity);
				cards.remove(offer.getId());
			}
		}

		cards.forEach((key, card) -> offers.add(new TradeOffer(this, key.getUserId(), card, quantities.get(key))));
	}

	private void collect(Integer userId, Map<CatalogCard, Integer> side, Map<TradeOfferId, Integer> quantities,
			Map<TradeOfferId, CatalogCard> cards) {
		side.forEach((card, quantity) -> {
			if (quantity == null || quantity < 1) {
				throw new IllegalArgumentException("Quantity must be at least 1 for card " + card.getId());
			}
			TradeOfferId key = new TradeOfferId(id, userId, card.getId());
			quantities.put(key, quantity);
			cards.put(key, card);
		});
	}
}
