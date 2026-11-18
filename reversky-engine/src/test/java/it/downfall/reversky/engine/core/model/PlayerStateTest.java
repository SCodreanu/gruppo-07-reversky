package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Test di {@link PlayerState}: pescata in ordine, creatura in campo, Oggetto, punti vita e creature sconfitte.
 */
class PlayerStateTest {

	private static final CardDefinition CRAWLERS = TestCards.creature("Crawlers", TestFactions.WOUND, 4, 5);
	private static final CardDefinition KAPPA = TestCards.creature("Kappa", TestFactions.DREAM, 6, 6);
	private static final CardDefinition VEIL = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");
	private static final CardDefinition TORII_BELL = TestCards.item(120, "Campanello del Torii",
			TestFactions.DREAM, "TORII_BELL");

	@Test
	@DisplayName("Si pesca nell'ordine del mazzo; a mazzo finito non si pesca niente")
	void drawCard_drawsInOrderUntilEmpty() {
		// Arrange
		PlayerState player = player(List.of(VEIL, CRAWLERS));

		// Act + Assert
		assertThat(player.drawCard()).contains(VEIL);
		assertThat(player.drawCard()).contains(CRAWLERS);
		assertThat(player.drawCard()).isEmpty();
	}

	@Test
	@DisplayName("Il mazzo è una copia: cambiare la lista ricevuta non cambia le pescate")
	void deck_sourceListChanged_stateUnchanged() {
		// Arrange
		List<CardDefinition> source = new ArrayList<>(List.of(CRAWLERS));
		PlayerState player = player(source);

		// Act
		source.add(KAPPA);

		// Assert
		assertThat(player.deck()).containsExactly(CRAWLERS);
	}

	@Test
	@DisplayName("Ha creature da schierare se ne ha una in campo o nel mazzo; una Spell sola non basta")
	void hasCreatures_inPlayOrInDeck() {
		// Arrange
		PlayerState onlySpell = player(List.of(VEIL));
		PlayerState creatureInDeck = player(List.of(VEIL, CRAWLERS));
		PlayerState creatureInPlay = player(List.of());
		creatureInPlay.placeCreature(new CreatureInPlay(KAPPA, Side.CHALLENGER));

		// Act + Assert
		assertThat(onlySpell.hasCreatures()).isFalse();
		assertThat(creatureInDeck.hasCreatureInDeck()).isTrue();
		assertThat(creatureInDeck.hasCreatures()).isTrue();
		assertThat(creatureInPlay.hasCreatures()).isTrue();
	}

	@Test
	@DisplayName("Creatura in campo: vuoto all'inizio, presente dopo placeCreature, vuoto dopo removeCreature")
	void creature_placeAndRemove() {
		// Arrange
		PlayerState player = player(List.of());
		CreatureInPlay crawlers = new CreatureInPlay(CRAWLERS, Side.CHALLENGER);

		// Act + Assert
		assertThat(player.creature()).isEmpty();
		player.placeCreature(crawlers);
		assertThat(player.creature()).contains(crawlers);
		assertThat(player.hasCreature()).isTrue();
		player.removeCreature();
		assertThat(player.creature()).isEmpty();
	}

	@Test
	@DisplayName("Due creature in campo, una creatura dell'altro lato o un campo già vuoto sono bug")
	void creature_invalidOperations_throw() {
		// Arrange
		PlayerState player = player(List.of());
		player.placeCreature(new CreatureInPlay(CRAWLERS, Side.CHALLENGER));
		PlayerState empty = player(List.of());

		// Act + Assert
		assertThatThrownBy(() -> player.placeCreature(new CreatureInPlay(KAPPA, Side.CHALLENGER)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> empty.placeCreature(new CreatureInPlay(KAPPA, Side.OPPONENT)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(empty::removeCreature).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("I punti vita possono scendere sotto 0: 30 − 35 = −5 (D-43)")
	void loseLifePoints_belowZero_keepsTrueValue() {
		// Arrange
		PlayerState player = player(List.of());

		// Act
		player.loseLifePoints(35);

		// Assert
		assertThat(player.lifePoints()).isEqualTo(-5);
		assertThatThrownBy(() -> player.loseLifePoints(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("L'Oggetto resta accanto al mazzo finché si consuma; solo un ITEM si può mettere lì")
	void item_placeAndConsume() {
		// Arrange
		PlayerState player = player(List.of());

		// Act + Assert
		player.placeItem(TORII_BELL);
		assertThat(player.item()).contains(TORII_BELL);
		player.consumeItem();
		assertThat(player.item()).isEmpty();
		assertThatThrownBy(player::consumeItem).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> player.placeItem(VEIL)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Le creature sconfitte si contano una alla volta (D-42)")
	void recordDefeatedCreature_counts() {
		// Arrange
		PlayerState player = player(List.of());

		// Act
		player.recordDefeatedCreature();
		player.recordDefeatedCreature();

		// Assert
		assertThat(player.creaturesDefeated()).isEqualTo(2);
	}

	private static PlayerState player(List<CardDefinition> deck) {
		return new PlayerState(Side.CHALLENGER, 7, 30, deck);
	}
}
