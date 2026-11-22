package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test di {@link PlayerInput}: il mazzo ricevuto è immutabile (CodingRequirements §7, input immutabili).
 */
class PlayerInputTest {

	private static final CardDefinition CRAWLERS = new CardDefinition(4, "Crawlers", CardType.CREATURE, "Common", 1,
			1, null, 4, 5, null, null);
	private static final CardDefinition KAPPA = new CardDefinition(401, "Kappa", CardType.CREATURE, "Common", 1,
			4, null, 6, 7, null, null);

	@Test
	@DisplayName("Il mazzo è una copia: aggiungere Kappa alla lista originale lascia il mazzo con 1 carta")
	void deck_sourceListChangedAfterConstruction_deckUnchanged() {
		// Arrange
		List<CardDefinition> source = new ArrayList<>(List.of(CRAWLERS));
		PlayerInput challenger = new PlayerInput(7, source);

		// Act
		source.add(KAPPA);

		// Assert
		assertThat(challenger.deck()).containsExactly(CRAWLERS);
	}

	@Test
	@DisplayName("Il mazzo restituito non si può modificare")
	void deck_modifiedFromOutside_throwsUnsupportedOperation() {
		// Arrange: lista modificabile, così il test fallisce se il record la tiene così com'è
		PlayerInput challenger = new PlayerInput(7, new ArrayList<>(List.of(CRAWLERS)));

		// Act + Assert
		assertThatThrownBy(() -> challenger.deck().add(KAPPA))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
