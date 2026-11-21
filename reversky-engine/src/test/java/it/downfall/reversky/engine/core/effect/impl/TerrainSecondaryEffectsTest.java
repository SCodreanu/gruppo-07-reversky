package it.downfall.reversky.engine.core.effect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.simulation.DeathResolver;
import it.downfall.reversky.engine.core.simulation.ItemService;
import it.downfall.reversky.engine.core.simulation.RoundResolver;
import it.downfall.reversky.engine.core.simulation.TerrainService;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Effetti secondari dei Terreni (ENG-27, D-18), decisi il 18 novembre: Echo Rift, Hollow Clearing, Watching Rift e
 * Devouring Abyss. Si provano attraverso il {@link RoundResolver}, con le Spell e le Hidden risolte come le risolve il
 * motore.
 */
class TerrainSecondaryEffectsTest {

	private static final CardDefinition ECHO_RIFT = TestCards.terrain(113, "Echo Rift", TestFactions.WOUND,
			"ECHO_RIFT");
	private static final CardDefinition HOLLOW_CLEARING = TestCards.terrain(114, "Hollow Clearing",
			TestFactions.INVITATION, "HOLLOW_CLEARING");
	private static final CardDefinition WATCHING_RIFT = TestCards.terrain(115, "Watching Rift", TestFactions.ARCHIVE,
			"WATCHING_RIFT");
	private static final CardDefinition DEVOURING_ABYSS = TestCards.terrain(116, "Devouring Abyss",
			TestFactions.DREAM, "DEVOURING_ABYSS");

	private final EffectRegistry registry = EffectRegistry.withAllEffects();
	private final RoundResolver resolver = new RoundResolver(TestFactions.noBonuses(), registry);
	private final DeathResolver deaths = new DeathResolver(new ItemService(registry));

	// ---- Echo Rift ----

	@Test
	@DisplayName("Echo Rift: la prima ferita dello scontro dà +1 ATK fino a fine scontro (4 → 5), la seconda niente")
	void echoRift_firstWoundOfCombat() {
		// Arrange
		MatchState state = TestStates.inCombat(TestCards.creature("Crawlers", TestFactions.WOUND, 3, 20),
				TestCards.creature("B", TestFactions.INVITATION, 2, 40));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);
		CreatureInPlay crawlers = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		assertThat(crawlers.attack()).isEqualTo(4);

		// Act + Assert
		resolver.resolve(state);
		assertThat(crawlers.attack()).isEqualTo(5);
		assertThat(messages(state)).contains("Crawlers: +1 ATK this combat from Echo Rift (ATK 5)");
		resolver.resolve(state);
		assertThat(crawlers.attack()).isEqualTo(5);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(31);
	}

	@Test
	@DisplayName("Echo Rift: il +1 finisce con lo scontro e torna alla prima ferita del nuovo scontro (4, poi 5)")
	void echoRift_againInNewCombat() {
		// Arrange: B 2/4 muore al round 1
		MatchState state = TestStates.inCombat(TestCards.creature("Crawlers", TestFactions.WOUND, 3, 20),
				TestCards.creature("B", TestFactions.INVITATION, 2, 4));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);
		CreatureInPlay crawlers = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(crawlers.attack()).isEqualTo(4);

		// Act
		state.opponent().placeCreature(new CreatureInPlay(TestCards.creature("C", TestFactions.INVITATION, 2, 40),
				Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);

		// Assert
		assertThat(crawlers.attack()).isEqualTo(5);
	}

	@Test
	@DisplayName("Echo Rift (deciso il 18 novembre): una ferita fra due scontri conta per lo scontro dopo")
	void echoRift_woundBetweenCombatsCountsForNext() {
		// Arrange: B muore al round 1, poi l'avversario pesca Skin Fracture
		MatchState state = TestStates.inCombat(TestCards.creature("Crawlers", TestFactions.WOUND, 3, 20),
				TestCards.creature("B", TestFactions.INVITATION, 2, 4));
		TestStates.placeTerrain(state, ECHO_RIFT, Side.CHALLENGER);
		CreatureInPlay crawlers = state.challenger().creature().orElseThrow();
		resolver.startCombat(state);
		resolver.resolve(state);

		// Act + Assert: la ferita della Spell dà il +1 per lo scontro con C
		castSpell(state, Side.OPPONENT, "SKIN_FRACTURE", "Skin Fracture");
		deaths.resolve(state);
		assertThat(crawlers.defense()).isEqualTo(13);
		assertThat(crawlers.attack()).isEqualTo(5);
		state.opponent().placeCreature(new CreatureInPlay(TestCards.creature("C", TestFactions.INVITATION, 2, 40),
				Side.OPPONENT));
		resolver.startCombat(state);
		resolver.resolve(state);
		assertThat(crawlers.attack()).isEqualTo(5);
	}

	// ---- Hollow Clearing ----

	@Test
	@DisplayName("Hollow Clearing: il danno di un effetto avversario è ridotto di 1 (12 → 11), l'attacco no (→ 9)")
	void hollowClearing_effectDamageReducedAttackNot() {
		// Arrange: Squonk 3/10 con +2 DEF dal Terreno; SCP-173 fa 2 danni all'entrata
		MatchState state = TestStates.inCombat(registry, TestCards.creature("Squonk", TestFactions.INVITATION, 3, 10),
				TestCards.creature(0, "SCP-173", TestFactions.ARCHIVE, null, "Uncommon", 2, 30, "SCP_173"));
		TestStates.placeTerrain(state, HOLLOW_CLEARING, Side.CHALLENGER);
		CreatureInPlay squonk = state.challenger().creature().orElseThrow();

		// Act + Assert
		resolver.startCombat(state);
		assertThat(squonk.defense()).isEqualTo(11);
		assertThat(messages(state)).containsSubsequence("Squonk: takes 1 less damage (Hollow Clearing)",
				"SCP-173: deals 1 damage to Squonk (DEF 11)");
		resolver.resolve(state);
		assertThat(squonk.defense()).isEqualTo(9);
	}

	@Test
	@DisplayName("Hollow Clearing: anche il danno in attesa all'entrata, che arriva prima del bonus (10 − 4 + 2 = 8)")
	void hollowClearing_pendingDamageBeforeBonus() {
		// Arrange: Skin Fracture dell'avversario in attesa, 5 danni
		MatchState state = TestStates.inCombat(TestCards.creature("Squonk", TestFactions.INVITATION, 3, 10),
				TestCards.creature("B", TestFactions.WOUND, 2, 30));
		TestStates.placeTerrain(state, HOLLOW_CLEARING, Side.OPPONENT);
		state.challenger().addPendingModifier(
				new PendingModifier("Skin Fracture", ModifierSource.SPELL, Side.OPPONENT, 0, 0, 5));

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(8);
	}

	@Test
	@DisplayName("Hollow Clearing: il sacrificio di The Thorned Matriarch non è una penalità (22 − 2 − 2 = 18)")
	void hollowClearing_selfSacrificeNotReduced() {
		// Arrange
		MatchState state = TestStates.inCombat(registry,
				TestCards.creature(0, "The Thorned Matriarch", TestFactions.INVITATION, null, "Mythic", 3, 20,
						"THORNED_MATRIARCH"),
				TestCards.creature("B", TestFactions.WOUND, 2, 10));
		TestStates.placeTerrain(state, HOLLOW_CLEARING, Side.CHALLENGER);
		resolver.startCombat(state);

		// Act
		resolver.resolve(state);

		// Assert: B 10 − 6 − 3 = 1, la Theory of the Wound non è protetta
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(18);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(1);
	}

	@Test
	@DisplayName("Hollow Clearing (deciso il 18 novembre): Breaking Point toglie 7 − 1 = 6, Squonk resta a 1 DEF")
	void hollowClearing_breakingPointLeavesOne() {
		// Arrange: Squonk 3/5 + 2 = 7, quindi Breaking Point la porterebbe a 0
		MatchState state = TestStates.inCombat(TestCards.creature("Squonk", TestFactions.INVITATION, 3, 5),
				TestCards.creature("B", TestFactions.WOUND, 2, 30));
		TestStates.placeTerrain(state, HOLLOW_CLEARING, Side.CHALLENGER);
		resolver.startCombat(state);
		CardDefinition breakingPoint = TestCards.hidden(108, "Breaking Point", CardSubtype.DAMAGE, "BREAKING_POINT");

		// Act
		registry.createHidden("BREAKING_POINT").trigger(new EffectContext(state, Side.OPPONENT, breakingPoint));

		// Assert
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(1);
		assertThat(state.log().events()).anySatisfy(event -> {
			assertThat(event.type()).isEqualTo(EventType.HIDDEN_TRIGGERED);
			assertThat(event.data()).containsEntry("effect", "−6 DEF to Squonk");
		});
	}

	// ---- Watching Rift ----

	@ParameterizedTest(name = "{0}: SCP-131 {1}/{2}")
	@CsvSource({ "SKIN_FRACTURE, 4, 7", "DISSOLUTION, 2, 10", "BITE_OF_REVERSE, 2, 11" })
	@DisplayName("Watching Rift: la prima Spell DAMAGE fa 1 punto in meno, prima dalla DEF, poi dall'ATK (da 4/11)")
	void watchingRift_firstDamageSpellOnePointLess(String code, int attack, int defense) {
		// Arrange: SCP-131 3/11 con +1 ATK dal Terreno (ENG-42: il Terreno non dà più +1 DEF)
		MatchState state = TestStates.inCombat(TestCards.creature("C", TestFactions.WOUND, 2, 30),
				TestCards.creature("SCP-131", TestFactions.ARCHIVE, 3, 11));
		TestStates.placeTerrain(state, WATCHING_RIFT, Side.OPPONENT);
		resolver.startCombat(state);

		// Act
		castSpell(state, Side.CHALLENGER, code, code);

		// Assert
		CreatureInPlay scp = state.opponent().creature().orElseThrow();
		assertThat(scp.attack()).isEqualTo(attack);
		assertThat(scp.defense()).isEqualTo(defense);
	}

	@Test
	@DisplayName("Watching Rift (deciso il 18 novembre): una volta per scontro; fra due scontri conta per il prossimo")
	void watchingRift_oncePerCombatWindow() {
		// Arrange: SCP-131 3/21, il Terreno dà solo +1 ATK (ENG-42)
		MatchState state = TestStates.inCombat(TestCards.creature("C", TestFactions.WOUND, 2, 30),
				TestCards.creature("SCP-131", TestFactions.ARCHIVE, 3, 21));
		TestStates.placeTerrain(state, WATCHING_RIFT, Side.OPPONENT);
		CreatureInPlay scp = state.opponent().creature().orElseThrow();
		resolver.startCombat(state);

		// Act + Assert: nello scontro 1 la prima −4, la seconda −5
		castSpell(state, Side.CHALLENGER, "SKIN_FRACTURE", "Skin Fracture");
		assertThat(scp.defense()).isEqualTo(17);
		castSpell(state, Side.CHALLENGER, "SKIN_FRACTURE", "Skin Fracture");
		assertThat(scp.defense()).isEqualTo(12);

		// Act + Assert: C muore; fra i due scontri la prima conta per lo scontro 2 (−4), nello scontro 2 la seconda −5
		state.challenger().creature().orElseThrow().takeDamage(100);
		deaths.resolve(state);
		castSpell(state, Side.CHALLENGER, "SKIN_FRACTURE", "Skin Fracture");
		assertThat(scp.defense()).isEqualTo(8);
		assertThat(messages(state)).contains("SCP-131: takes 1 point less from Skin Fracture (Watching Rift)");
		state.challenger().placeCreature(new CreatureInPlay(TestCards.creature("D", TestFactions.WOUND, 2, 30),
				Side.CHALLENGER));
		resolver.startCombat(state);
		castSpell(state, Side.CHALLENGER, "SKIN_FRACTURE", "Skin Fracture");
		assertThat(scp.defense()).isEqualTo(3);
	}

	// ---- Devouring Abyss ----

	@Test
	@DisplayName("Devouring Abyss: a ogni inizio scontro la creatura Dream fa 1 danno all'avversaria (B 4, poi C 9)")
	void devouringAbyss_oneDamageEachCombat() {
		// Arrange: B 2/5 muore al round 1; Kodama 4/10, il Terreno le dà uno scudo di 1 DEF (ENG-42), non ATK
		MatchState state = TestStates.inCombat(TestCards.creature("Kodama", TestFactions.DREAM, 4, 10),
				TestCards.creature("B", TestFactions.WOUND, 2, 5));
		TestStates.placeTerrain(state, DEVOURING_ABYSS, Side.OPPONENT);

		// Act + Assert
		resolver.startCombat(state);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(4);
		assertThat(messages(state)).contains("Kodama: deals 1 damage to B with Devouring Abyss (DEF 4)");
		resolver.resolve(state);
		assertThat(state.opponent().creature()).isEmpty();
		state.opponent().placeCreature(new CreatureInPlay(TestCards.creature("C", TestFactions.WOUND, 2, 10),
				Side.OPPONENT));
		resolver.startCombat(state);
		assertThat(state.opponent().creature().orElseThrow().defense()).isEqualTo(9);
	}

	@Test
	@DisplayName("Devouring Abyss (18 novembre): due creature Dream, prima lo sfidante; se uccide, l'altra no")
	void devouringAbyss_challengerFirstThenDeathCheck() {
		// Arrange: la creatura dell'avversario ha 1 DEF: 1 + lo scudo di 1 del Terreno (ENG-42), già consumato
		MatchState state = TestStates.inCombat(TestCards.creature("Kodama", TestFactions.DREAM, 3, 10),
				TestCards.creature("Kasa-obake", TestFactions.DREAM, 3, 1));
		TestStates.placeTerrain(state, DEVOURING_ABYSS, Side.CHALLENGER);
		CreatureInPlay kasa = state.opponent().creature().orElseThrow();
		new TerrainService(registry).applyBonus(state, kasa);
		kasa.takeDamage(1);

		// Act
		resolver.startCombat(state);

		// Assert
		assertThat(state.opponent().creature()).isEmpty();
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(11);
	}

	@Test
	@DisplayName("Devouring Abyss (deciso il 18 novembre): la ferita viene dalla creatura, SCP-096 la marchia (8)")
	void devouringAbyss_woundCausedByCreature() {
		// Arrange
		MatchState state = TestStates.inCombat(registry,
				TestCards.creature(0, "SCP-096", TestFactions.ARCHIVE, null, "Uncommon", 5, 30, "SCP_096"),
				TestCards.creature("Kodama", TestFactions.DREAM, 3, 30));
		TestStates.placeTerrain(state, DEVOURING_ABYSS, Side.OPPONENT);

		// Act
		resolver.startCombat(state);

		// Assert
		CreatureInPlay scp = state.challenger().creature().orElseThrow();
		assertThat(scp.defense()).isEqualTo(29);
		assertThat(scp.attack()).isEqualTo(8);
	}

	private void castSpell(MatchState state, Side caster, String code, String name) {
		CardDefinition spell = TestCards.spell(93, name, CardSubtype.DAMAGE, code);
		registry.createSpell(code).resolve(new EffectContext(state, caster, spell));
	}

	private static List<String> messages(MatchState state) {
		return state.log().events().stream().map(MatchEvent::message).toList();
	}
}
