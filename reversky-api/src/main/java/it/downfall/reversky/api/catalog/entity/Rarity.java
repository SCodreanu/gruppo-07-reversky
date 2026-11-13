package it.downfall.reversky.api.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rarità delle carte (tabella {@code rarities}, docs/pacchetti e docs/mazzi).
 * <p>
 * {@code sortingOrder} si sceglie alla creazione e non cambia più: arriva all'engine come
 * {@code rarityOrder}, e alcuni effetti lo confrontano con soglie scritte nel codice.
 */
@Entity
@Table(name = "rarities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rarity {

	/**
	 * {@code sorting_order} di Common, Uncommon e Rare: le rarità a cui sono legate le regole di tipo
	 * (docs/backoffice, regola 4; vedi {@link CardType#acceptsRarity(Rarity)}).
	 * <p>
	 * Si confrontano gli ordini e mai i nomi: il nome si modifica dal backoffice, sortingOrder no.
	 * Anche l'engine confronta le rarità solo con {@code rarityOrder}. Il seed delle rarità (#85)
	 * deve usare questi valori.
	 */
	public static final int COMMON_ORDER = 1;

	/**
	 * Vedi {@link #COMMON_ORDER}.
	 */
	public static final int UNCOMMON_ORDER = 2;

	/**
	 * Vedi {@link #COMMON_ORDER}.
	 */
	public static final int RARE_ORDER = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "deck_limit", nullable = false)
	private Integer deckLimit;

	@Column(name = "max_per_deck")
	private Integer maxPerDeck;

	@Column(name = "sorting_order", nullable = false, unique = true, updatable = false)
	private Integer sortingOrder;

	public Rarity(String name, Integer deckLimit, Integer maxPerDeck, Integer sortingOrder) {
		this.name = name;
		this.deckLimit = deckLimit;
		this.maxPerDeck = maxPerDeck;
		this.sortingOrder = sortingOrder;
	}

	/**
	 * Modifica dal backoffice (PUT /admin/rarities/{id}): si salva da sola alla fine della transazione.
	 * sortingOrder non c'è di proposito.
	 */
	public void update(String name, Integer deckLimit, Integer maxPerDeck) {
		this.name = name;
		this.deckLimit = deckLimit;
		this.maxPerDeck = maxPerDeck;
	}

}
