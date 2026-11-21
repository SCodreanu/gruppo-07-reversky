package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;

/**
 * Test di {@link SimulationInput}: fazioni e bonus ricevuti sono immutabili (CodingRequirements §7).
 * I mazzi sono coperti da {@link PlayerInputTest}.
 */
class SimulationInputTest {

	private static final int WOUND = 1;   // Theory of the Wound
	private static final int DREAM = 4;   // Theory of the Dream

	private static final Faction WOUND_FACTION = new Faction(WOUND, "Theory of the Wound");
	private static final Faction DREAM_FACTION = new Faction(DREAM, "Theory of the Dream");
	private static final FactionBonus WOUND_BEATS_DREAM = new FactionBonus(WOUND, DREAM, 2, 0);
	private static final FactionBonus DREAM_FEARS_WOUND = new FactionBonus(DREAM, WOUND, -1, 0);

	private static final RuleConfig RULES = new RuleConfig("1.0", 30, 75);
	private static final CardDefinition CRAWLERS = new CardDefinition(4, "Crawlers", CardType.CREATURE, "Common", 1,
			WOUND, null, 4, 5, null, null);

	@Test
	@DisplayName("Fazioni e bonus sono copie: aggiungere una riga alle liste originali non cambia l'input (1 e 1)")
	void lists_sourceListsChangedAfterConstruction_inputUnchanged() {
		// Arrange
		List<Faction> factions = new ArrayList<>(List.of(WOUND_FACTION));
		List<FactionBonus> bonuses = new ArrayList<>(List.of(WOUND_BEATS_DREAM));
		SimulationInput input = input(factions, bonuses);

		// Act
		factions.add(DREAM_FACTION);
		bonuses.add(DREAM_FEARS_WOUND);

		// Assert
		assertThat(input.factions()).containsExactly(WOUND_FACTION);
		assertThat(input.factionBonuses()).containsExactly(WOUND_BEATS_DREAM);
	}

	@Test
	@DisplayName("Le liste di fazioni e bonus restituite non si possono modificare")
	void lists_modifiedFromOutside_throwsUnsupportedOperation() {
		// Arrange: liste modificabili, così il test fallisce se il record le tiene così come sono
		SimulationInput input = input(new ArrayList<>(List.of(WOUND_FACTION)),
				new ArrayList<>(List.of(WOUND_BEATS_DREAM)));

		// Act + Assert
		assertThatThrownBy(() -> input.factions().add(DREAM_FACTION))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> input.factionBonuses().add(DREAM_FEARS_WOUND))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	@DisplayName("Una lista di bonus vuota è ammessa (factionBonuses obbligatoria ma anche vuota)")
	void factionBonuses_emptyList_accepted() {
		// Arrange + Act
		SimulationInput input = input(List.of(WOUND_FACTION), List.of());

		// Assert
		assertThat(input.factionBonuses()).isEmpty();
	}

	private static SimulationInput input(List<Faction> factions, List<FactionBonus> bonuses) {
		PlayerInput challenger = new PlayerInput(7, List.of(CRAWLERS));
		PlayerInput opponent = new PlayerInput(12, List.of(CRAWLERS));
		return new SimulationInput(42L, RULES, factions, bonuses, challenger, opponent);
	}
}
