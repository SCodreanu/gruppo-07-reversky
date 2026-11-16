package it.downfall.reversky.engine.support;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;

/**
 * Carte di prova in una riga.
 * <p>
 * I metodi senza id danno {@code cardId} 0: l'engine non ha bisogno di id unici (le copie di una carta hanno lo
 * stesso id). Le carte del catalogo in {@link TestDecks} usano invece il numero di riga di
 * {@code docs/bilanciamento/catalogo-finale.csv}, così si ritrovano.
 * <p>
 * Rarità: Common 1, Uncommon 2, Rare 3, Mythic 4, Legendary 5, Mutant 6. Spell e Hidden sono Uncommon, come Veil
 * of the Anchored in docs/api-engine; Terreni e Oggetti sono Rare. Sono valori scritti a mano che fanno le veci
 * della risposta dell'API: i dati veri arrivano con i seed Flyway delle rarità e del catalogo (vedi i TODO).
 */
public final class TestCards {

	private static final int NO_ID = 0;
	private static final String COMMON = "Common";
	// TODO(@tommasofatticcioni): rarità di Spell e Hidden scritta a mano, il catalogo non la dà ancora;
	// sostituire con quella del catalogo in Flyway (API-55, #125)
	private static final String SHARED_CARD_RARITY = "Uncommon";
	private static final String RARE = "Rare";

	private TestCards() {
	}

	/**
	 * Creatura Common senza effetto e con una sola fazione.
	 *
	 * @param name      nome della carta
	 * @param factionId fazione
	 * @param attack    ATK
	 * @param defense   DEF
	 * @return la carta, con {@code cardId} 0
	 */
	public static CardDefinition creature(String name, int factionId, int attack, int defense) {
		return creature(NO_ID, name, factionId, null, COMMON, attack, defense, null);
	}

	/**
	 * Creatura Common senza effetto e con due fazioni (D-22).
	 *
	 * @param name               nome della carta
	 * @param factionId          fazione principale
	 * @param secondaryFactionId seconda fazione
	 * @param attack             ATK
	 * @param defense            DEF
	 * @return la carta, con {@code cardId} 0
	 */
	public static CardDefinition creature(String name, int factionId, int secondaryFactionId, int attack,
			int defense) {
		return creature(NO_ID, name, factionId, secondaryFactionId, COMMON, attack, defense, null);
	}

	/**
	 * Creatura con tutti i campi, per le carte del catalogo.
	 *
	 * @param cardId             id della carta
	 * @param name               nome della carta
	 * @param factionId          fazione principale
	 * @param secondaryFactionId seconda fazione, o {@code null}
	 * @param rarity             nome della rarità
	 * @param attack             ATK
	 * @param defense            DEF
	 * @param effectCode         codice dell'effetto, o {@code null}
	 * @return la carta
	 */
	public static CardDefinition creature(int cardId, String name, int factionId, Integer secondaryFactionId,
			String rarity, int attack, int defense, String effectCode) {
		return new CardDefinition(cardId, name, CardType.CREATURE, rarity, rarityOrder(rarity), factionId,
				secondaryFactionId, attack, defense, null, effectCode);
	}

	/**
	 * Spell senza fazione.
	 *
	 * @param cardId     id della carta
	 * @param name       nome della carta
	 * @param subtype    {@code DAMAGE}, {@code SHIELD} o {@code BURN}
	 * @param effectCode codice dell'effetto
	 * @return la carta
	 */
	public static CardDefinition spell(int cardId, String name, CardSubtype subtype, String effectCode) {
		return sharedCard(cardId, name, CardType.SPELL, subtype, effectCode);
	}

	/**
	 * Hidden senza fazione.
	 *
	 * @param cardId     id della carta
	 * @param name       nome della carta
	 * @param subtype    {@code SURVIVAL} o {@code DAMAGE}
	 * @param effectCode codice dell'effetto
	 * @return la carta
	 */
	public static CardDefinition hidden(int cardId, String name, CardSubtype subtype, String effectCode) {
		return sharedCard(cardId, name, CardType.HIDDEN, subtype, effectCode);
	}

	/**
	 * Terreno di una fazione.
	 *
	 * @param cardId     id della carta
	 * @param name       nome della carta
	 * @param factionId  fazione del Terreno
	 * @param effectCode codice dell'effetto
	 * @return la carta
	 */
	public static CardDefinition terrain(int cardId, String name, int factionId, String effectCode) {
		return new CardDefinition(cardId, name, CardType.TERRAIN, RARE, rarityOrder(RARE), factionId, null, null,
				null, null, effectCode);
	}

	/**
	 * Oggetto di una fazione (D-47).
	 *
	 * @param cardId     id della carta
	 * @param name       nome della carta
	 * @param factionId  fazione dell'Oggetto
	 * @param effectCode codice dell'effetto
	 * @return la carta
	 */
	public static CardDefinition item(int cardId, String name, int factionId, String effectCode) {
		return new CardDefinition(cardId, name, CardType.ITEM, RARE, rarityOrder(RARE), factionId, null, null,
				null, null, effectCode);
	}

	/**
	 * Ordine della rarità ({@code rarities.sorting_order}) usato nei test.
	 *
	 * @param rarity nome della rarità
	 * @return da 1 (Common) a 6 (Mutant)
	 */
	public static int rarityOrder(String rarity) {
		// TODO(@tommasofatticcioni): ordini scritti a mano al posto di rarities.sorting_order; allinearli al seed
		// delle rarità in Flyway (API-15, #85)
		return switch (rarity) {
			case "Common" -> 1;
			case "Uncommon" -> 2;
			case "Rare" -> 3;
			case "Mythic" -> 4;
			case "Legendary" -> 5;
			case "Mutant" -> 6;
			default -> throw new IllegalArgumentException("Rarità sconosciuta nei test: " + rarity);
		};
	}

	private static CardDefinition sharedCard(int cardId, String name, CardType type, CardSubtype subtype,
			String effectCode) {
		return new CardDefinition(cardId, name, type, SHARED_CARD_RARITY, rarityOrder(SHARED_CARD_RARITY), null,
				null, null, null, subtype, effectCode);
	}
}
