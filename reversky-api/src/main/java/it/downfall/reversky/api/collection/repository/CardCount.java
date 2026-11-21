package it.downfall.reversky.api.collection.repository;

/**
 * Riga di una query nativa che conta qualcosa per carta (mazzi che la usano, copie riservate).
 */
public interface CardCount {

	/** Id della carta. */
	Integer getCardId();

	/** Il conteggio o la somma per quella carta. */
	Long getTotal();
}
