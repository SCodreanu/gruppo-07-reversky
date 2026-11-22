package it.downfall.reversky.api.trade.entity;

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
 * Le copie di una carta che un partecipante cede in uno scambio (tabella {@code trade_offers}, docs/scambi "Modello").
 * <p>
 * {@code userId} è sempre chi <b>cede</b> la carta, ed è uno dei due partecipanti. Le righe si creano, modificano ed
 * eliminano solo da {@link Trade#replaceOffers}, che aggiorna quelle esistenti invece di svuotare e reinserire.
 */
@Entity
@Table(name = "trade_offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeOffer {

	@EmbeddedId
	private TradeOfferId id;

	@MapsId("tradeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trade_id", nullable = false)
	private Trade trade;

	@MapsId("cardId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "card_id", nullable = false)
	private CatalogCard card;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	TradeOffer(Trade trade, Integer userId, CatalogCard card, int quantity) {
		this.id = new TradeOfferId(trade.getId(), userId, card.getId());
		this.trade = trade;
		this.card = card;
		this.quantity = quantity;
	}

	void changeQuantity(int quantity) {
		this.quantity = quantity;
	}

	/** Chi cede la carta. */
	public Integer getUserId() {
		return id.getUserId();
	}

	/** Comodità: l'id composto non serve chiamarlo ogni volta. */
	public Integer getCardId() {
		return id.getCardId();
	}
}
