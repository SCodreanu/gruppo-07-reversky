package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test di {@link CardDefinition#factions()}: l'elenco delle fazioni di una carta su cui
 * {@code FactionMatrix} somma i bonus di tutte le coppie (docs/fazioni, "Carte con due fazioni", D-22).
 */
class CardDefinitionTest {

	private static final int INVITATION = 2;   // Theory of the Invitation
	private static final int ARCHIVE = 3;      // Theory of the Archive
	private static final int DREAM = 4;        // Theory of the Dream

	@Test
	@DisplayName("Una creatura con una sola fazione (Archive = 3) restituisce [3]")
	void factions_singleFaction_returnsOnlyPrimary() {
		// Arrange
		CardDefinition archivePuppet = creature(304, "Archive Puppet", ARCHIVE, null);

		// Act + Assert
		assertThat(archivePuppet.factions()).containsExactly(ARCHIVE);
	}

	@Test
	@DisplayName("Due fazioni (Dream = 4, Invitation = 2) restituiscono [4, 2]: prima la principale, non per id")
	void factions_twoFactions_returnsPrimaryThenSecondary() {
		// Arrange: la seconda fazione ha l'id più basso, così un ordinamento per id darebbe [2, 4]
		CardDefinition rootbound = creature(601, "Rootbound Doppelganger", DREAM, INVITATION);

		// Act + Assert
		assertThat(rootbound.factions()).containsExactly(DREAM, INVITATION);
	}

	@Test
	@DisplayName("Una Spell non ha fazione: restituisce una lista vuota, quindi 0 coppie e bonus 0")
	void factions_spellWithoutFaction_returnsEmptyList() {
		// Arrange
		CardDefinition veil = new CardDefinition(70, "Veil of the Anchored", CardType.SPELL, "Uncommon", 2,
				null, null, null, null, CardSubtype.SHIELD, "VEIL_OF_ANCHORED");

		// Act + Assert
		assertThat(veil.factions()).isEmpty();
	}

	private static CardDefinition creature(int cardId, String name, int factionId, Integer secondaryFactionId) {
		return new CardDefinition(cardId, name, CardType.CREATURE, "Common", 1,
				factionId, secondaryFactionId, 6, 6, null, null);
	}
}
