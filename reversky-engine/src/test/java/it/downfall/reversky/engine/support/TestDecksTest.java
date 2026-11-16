package it.downfall.reversky.engine.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.validation.SimulationInputValidator;

/**
 * Test di {@link TestDecks#full(int)}: il mazzo completo rispetta la composizione e i limiti di rarità dell'API
 * (reversky-api/docs/mazzi) ed è un input valido per l'engine. Se cambia il catalogo, questo test dice se il
 * mazzo di prova è ancora un mazzo vero.
 */
class TestDecksTest {

	private final SimulationInputValidator validator = new SimulationInputValidator();

	@ParameterizedTest(name = "fazione {0}")
	@ValueSource(ints = { TestFactions.WOUND, TestFactions.INVITATION, TestFactions.ARCHIVE, TestFactions.DREAM })
	void full_hasApiComposition(int factionId) {
		// Act
		List<CardDefinition> deck = TestDecks.full(factionId);

		// Assert: 20 creature, 6 Spell, 2 Hidden, 1 Terreno, 1 Oggetto
		assertThat(deck).hasSize(30);
		assertThat(count(deck, CardType.CREATURE)).isEqualTo(20);
		assertThat(count(deck, CardType.SPELL)).isEqualTo(6);
		assertThat(count(deck, CardType.HIDDEN)).isEqualTo(2);
		assertThat(count(deck, CardType.TERRAIN)).isEqualTo(1);
		assertThat(count(deck, CardType.ITEM)).isEqualTo(1);
	}

	@ParameterizedTest(name = "fazione {0}")
	@ValueSource(ints = { TestFactions.WOUND, TestFactions.INVITATION, TestFactions.ARCHIVE, TestFactions.DREAM })
	void full_respectsRarityLimits(int factionId) {
		// Act
		List<CardDefinition> deck = TestDecks.full(factionId);

		// Assert: massimo 2 Legendary, 2 Mythic, 1 Mutant, 5 Rare (Terreno e Oggetto compresi), 1 Hidden DAMAGE
		assertThat(rarity(deck, "Legendary")).isLessThanOrEqualTo(2);
		assertThat(rarity(deck, "Mythic")).isLessThanOrEqualTo(2);
		assertThat(rarity(deck, "Mutant")).isLessThanOrEqualTo(1);
		assertThat(rarity(deck, "Rare")).isLessThanOrEqualTo(5);
		assertThat(deck.stream().filter(card -> card.type() == CardType.HIDDEN
				&& card.subtype() == CardSubtype.DAMAGE).count()).isLessThanOrEqualTo(1);
	}

	@ParameterizedTest(name = "fazione {0}")
	@ValueSource(ints = { TestFactions.WOUND, TestFactions.INVITATION, TestFactions.ARCHIVE, TestFactions.DREAM })
	void full_isValidEngineInput(int factionId) {
		// Arrange
		List<CardDefinition> deck = TestDecks.full(factionId);

		// Act + Assert
		assertThatCode(() -> validator.validate(TestInputs.of(deck, TestDecks.full(TestFactions.WOUND))))
				.doesNotThrowAnyException();
	}

	private static long count(List<CardDefinition> deck, CardType type) {
		return deck.stream().filter(card -> card.type() == type).count();
	}

	private static long rarity(List<CardDefinition> deck, String rarity) {
		return deck.stream().filter(card -> rarity.equals(card.rarity())).count();
	}
}
