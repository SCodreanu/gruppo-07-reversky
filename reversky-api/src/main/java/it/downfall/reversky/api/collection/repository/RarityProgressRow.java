package it.downfall.reversky.api.collection.repository;

/**
 * Riga di {@link CollectionCardRepository#summarize(Integer)}: carte originali attive di una rarità e quante ne
 * possiede l'utente.
 */
public interface RarityProgressRow {

	Integer getRarityId();

	String getRarityName();

	Long getTotal();

	Long getOwned();
}
