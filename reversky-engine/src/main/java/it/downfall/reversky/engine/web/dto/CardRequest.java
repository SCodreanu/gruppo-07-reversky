package it.downfall.reversky.engine.web.dto;

import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Una copia di una carta nel mazzo (docs/api-engine). I nomi dei campi seguono il database ({@code atk}, {@code def});
 * nel core diventano {@code attack} e {@code defense}.
 * <p>
 * Qui sono obbligatori solo i campi che servono a ogni carta. Quelli che dipendono dal tipo (fazioni, statistiche,
 * sottotipo, effetto) li controlla il core, nell'ordine dei controlli del contratto. Un {@code type} o un
 * {@code subtype} che non esiste negli enum rende il JSON non valido.
 *
 * @param cardId             id della carta nel catalogo
 * @param name               nome della carta, in inglese, usato nel log
 * @param type               tipo della carta
 * @param rarity             nome della rarità
 * @param rarityOrder        {@code rarities.sorting_order}
 * @param factionId          fazione; {@code null} per Spell e Hidden
 * @param secondaryFactionId seconda fazione, solo per le creature; {@code null} se ne ha una sola
 * @param atk                attacco, solo per le creature
 * @param def                difesa, solo per le creature
 * @param subtype            sottotipo di Spell e Hidden
 * @param effectCode         codice dell'effetto, facoltativo per le creature
 */
public record CardRequest(

		@NotNull(message = "Card id is required")
		Integer cardId,

		@NotBlank(message = "Card name is required")
		String name,

		@NotNull(message = "Card type is required")
		CardType type,

		@NotBlank(message = "Card rarity is required")
		String rarity,

		@NotNull(message = "Card rarity order is required")
		Integer rarityOrder,

		Integer factionId,

		Integer secondaryFactionId,

		Integer atk,

		Integer def,

		CardSubtype subtype,

		String effectCode) {
}
