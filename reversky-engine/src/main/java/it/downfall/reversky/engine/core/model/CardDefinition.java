package it.downfall.reversky.engine.core.model;

import java.util.List;

/**
 * Carta di un mazzo così come arriva dall'API: una voce per ogni copia (docs/api-engine).
 * <p>
 * I campi che non valgono per un tipo di carta sono nulli: ATK e DEF solo per le creature, {@code subtype} solo
 * per Spell e Hidden, {@code factionId} nullo per Spell e Hidden, che sono condivise tra le fazioni. La coerenza
 * dei campi non si controlla qui ma nel validatore dell'input, così l'errore segue l'ordine del contratto.
 *
 * @param cardId             id della carta nel catalogo dell'API
 * @param name               nome mostrato ai giocatori, in inglese
 * @param type               tipo della carta
 * @param rarity             nome della rarità (es. "Common")
 * @param rarityOrder        {@code rarities.sorting_order}, per gli effetti che confrontano le rarità
 * @param factionId          fazione principale; nulla per Spell e Hidden
 * @param secondaryFactionId seconda fazione, diversa dalla principale; nulla se la carta ne ha una sola (D-22)
 * @param attack             ATK base; solo creature ({@code atk} nel JSON)
 * @param defense            DEF base; solo creature ({@code def} nel JSON)
 * @param subtype            sottotipo; solo Spell e Hidden
 * @param effectCode         codice dell'effetto; obbligatorio per Spell, Hidden, Terreni e Oggetti
 */
public record CardDefinition(
		int cardId,
		String name,
		CardType type,
		String rarity,
		int rarityOrder,
		Integer factionId,
		Integer secondaryFactionId,
		Integer attack,
		Integer defense,
		CardSubtype subtype,
		String effectCode) {

	/**
	 * Fazioni della carta su cui {@code FactionMatrix} somma i bonus di tutte le coppie (docs/fazioni, D-22).
	 * <p>
	 * L'ordine è sempre principale e poi secondaria, mai per id: così il calcolo è deterministico.
	 *
	 * @return lista vuota senza fazione (Spell e Hidden), {@code [factionId]} con una fazione,
	 *         {@code [factionId, secondaryFactionId]} con due
	 */
	public List<Integer> factions() {
		if (factionId == null) {
			return List.of();
		}
		if (secondaryFactionId == null) {
			return List.of(factionId);
		}
		return List.of(factionId, secondaryFactionId);
	}
}
