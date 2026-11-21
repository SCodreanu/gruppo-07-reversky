package it.downfall.reversky.api.deck.dto;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creazione o modifica di un mazzo (POST /api/v1/decks, PUT /api/v1/decks/{id}, docs/mazzi).
 *
 * @param name  1-50 caratteri, univoco tra i mazzi dell'utente; gli spazi all'inizio e alla fine si tolgono
 * @param cards il contenuto completo del mazzo: in modifica sostituisce quello salvato
 */
public record DeckRequest(

	@Schema(example = "Pioggia di Archivio")
	@NotBlank(message = "Name is required")
	@Size(max = 50, message = "Name must be at most 50 characters")
	String name,

	@NotNull(message = "Cards are required")
	List<@NotNull(message = "Card is required") @Valid DeckCardRequest> cards
) {

	// Hibernate Validator legge i metodi "is..." come proprietà: l'errore arriva sul campo "cardIdsUnique"
	@Schema(hidden = true)
	@AssertTrue(message = "Each card must appear only once: use quantity for more copies")
	public boolean isCardIdsUnique() {
		return hasUniqueCardIds(cards);
	}

	/** Comune alle richieste con una lista di carte: una lista nulla la segnala già {@code @NotNull}. */
	static boolean hasUniqueCardIds(List<DeckCardRequest> cards) {
		if (cards == null) {
			return true;
		}
		List<Integer> ids = cards.stream().filter(Objects::nonNull).map(DeckCardRequest::cardId).filter(Objects::nonNull).toList();
		return ids.size() == ids.stream().distinct().count();
	}
}
