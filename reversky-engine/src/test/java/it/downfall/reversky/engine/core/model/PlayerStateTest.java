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
	private static final CardDefinition SANCTUARY = TestCards.hidden(105, "Last Sanctuary", CardSubtype.SURVIVAL,
			"LAST_SANCTUARY");
	private static final CardDefinition PRESENCE = TestCards.hidden(109, "Hostile Presence", CardSubtype.DAMAGE,
			"HOSTILE_PRESENCE");
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
		assertThat(player.items()).containsExactly(TORII_BELL);
		player.consumeItem(TORII_BELL);
		assertThat(player.items()).isEmpty();
		assertThatThrownBy(() -> player.consumeItem(TORII_BELL)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> player.placeItem(VEIL)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Con due Oggetti non se ne perde nessuno: restano nell'ordine di pescata")
	void item_twoItems_bothKept() {
		// Arrange
		PlayerState player = player(List.of());
		CardDefinition badge = TestCards.item(117, "Badge di Uscita Negata", TestFactions.WOUND, "DENIED_EXIT_BADGE");

		// Act
		player.placeItem(TORII_BELL);
		player.placeItem(badge);

		// Assert
		assertThat(player.items()).containsExactly(TORII_BELL, badge);
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

	@Test
	@DisplayName("peekCard guarda la prima carta senza pescarla; vuoto a mazzo finito")
	void peekCard_doesNotDraw() {
		// Arrange
		PlayerState player = player(List.of(VEIL, CRAWLERS));

		// Act + Assert
		assertThat(player.peekCard()).contains(VEIL);
		assertThat(player.deck()).containsExactly(VEIL, CRAWLERS);
		assertThat(player(List.of()).peekCard()).isEmpty();
	}

	@Test
	@DisplayName("D-34: i modificatori in attesa si sommano sulla prossima creatura, in ordine di arrivo, e si tolgono")
	void pendingModifiers_accumulateThenTaken() {
		// Arrange
		PlayerState player = player(List.of());
		PendingModifier veil = pending("Veil of the Anchored", 0, 5);
		PendingModifier pact = pending("Registry Pact", 1, 2);

		// Act
		player.addPendingModifier(veil);
		player.addPendingModifier(pact);

		// Assert
		assertThat(player.pendingModifiers()).containsExactly(veil, pact);
		assertThat(player.takePendingModifiers()).containsExactly(veil, pact);
		assertThat(player.pendingModifiers()).isEmpty();
		assertThat(player.takePendingModifiers()).isEmpty();
	}

	@Test
	@DisplayName("SCP-458: il modificatore per la creatura dopo la prossima passa alla prossima quando la prima entra")
	void pendingModifiers_secondPositionShifts() {
		// Arrange
		PlayerState player = player(List.of());
		PendingModifier first = pending("SCP-458", 0, 2);
		PendingModifier second = pending("SCP-458", 0, 2);
		player.addPendingModifier(1, second);
		player.addPendingModifier(0, first);

		// Act + Assert
		assertThat(player.takePendingModifiers()).containsExactly(first);
		assertThat(player.pendingModifiers()).containsExactly(second);
		assertThat(player.takePendingModifiers()).containsExactly(second);
		assertThatThrownBy(() -> player.addPendingModifier(-1, first)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("D-15: le Hidden coperte restano nella zona nell'ordine di pescata finché si scartano")
	void hiddenZone_setThenDiscard() {
		// Arrange
		PlayerState player = player(List.of());
		SetHidden sanctuary = new SetHidden(SANCTUARY, 2);
		SetHidden presence = new SetHidden(PRESENCE, 3);

		// Act
		player.setHidden(sanctuary);
		player.setHidden(presence);
		player.discardHidden(sanctuary);

		// Assert
		assertThat(player.hiddenZone()).containsExactly(presence);
		assertThatThrownBy(() -> player.discardHidden(sanctuary)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("Una Hidden è pronta dal suo scontro in poi; serve una carta Hidden e uno scontro da 1")
	void setHidden_readyFromItsCombat() {
		// Arrange
		SetHidden hidden = new SetHidden(SANCTUARY, 2);

		// Act + Assert
		assertThat(hidden.isReadyIn(1)).isFalse();
		assertThat(hidden.isReadyIn(2)).isTrue();
		assertThat(hidden.isReadyIn(5)).isTrue();
		assertThatThrownBy(() -> new SetHidden(VEIL, 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new SetHidden(SANCTUARY, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	private static PendingModifier pending(String card, int attack, int shield) {
		return new PendingModifier(card, ModifierSource.SPELL, Side.CHALLENGER, attack, shield, 0);
	}

	private static PlayerState player(List<CardDefinition> deck) {
		return new PlayerState(Side.CHALLENGER, 7, 30, deck);
	}
}
