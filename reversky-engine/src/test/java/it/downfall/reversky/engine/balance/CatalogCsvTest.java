package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.support.TestFactions;

class CatalogCsvTest {

	private static List<CardDefinition> catalog;

	@BeforeAll
	static void loadCatalog() throws IOException {
		catalog = CatalogCsv.load(CatalogCsv.DEFAULT_PATH);
	}

	@Test
	@DisplayName("Il catalogo finale ha 120 carte: 92 creature, 12 Spell, 8 Hidden, 4 Terreni, 4 Oggetti (D-24)")
	void load_finalCatalog_hasTheStructureOfD24() {
		Map<CardType, Long> byType = catalog.stream()
				.collect(Collectors.groupingBy(CardDefinition::type, Collectors.counting()));

		assertThat(catalog).hasSize(120);
		assertThat(byType).containsEntry(CardType.CREATURE, 92L).containsEntry(CardType.SPELL, 12L)
				.containsEntry(CardType.HIDDEN, 8L).containsEntry(CardType.TERRAIN, 4L)
				.containsEntry(CardType.ITEM, 4L);
	}

	@Test
	@DisplayName("L'id di ogni carta è il numero di riga del CSV, come in TestDecks")
	void load_cardIdIsTheRowNumber() {
		assertThat(catalog).extracting(CardDefinition::cardId)
				.containsExactlyElementsOf(IntStream.rangeClosed(1, 120).boxed().toList());
		assertThat(catalog.getFirst()).isEqualTo(new CardDefinition(1, "Squonk", CardType.CREATURE, "Common", 1,
				TestFactions.INVITATION, null, 4, 12, null, null));
	}

	@Test
	@DisplayName("Le Mutant hanno due fazioni; Spell e Hidden nessuna, rarità Uncommon; Terreni e Oggetti Rare")
	void load_factionsAndRarities() {
		Map<String, CardDefinition> byName = catalog.stream()
				.collect(Collectors.toMap(CardDefinition::name, Function.identity()));

		assertThat(byName.get("The Rootbound Doppelganger").factions())
				.containsExactly(TestFactions.INVITATION, TestFactions.DREAM);
		assertThat(byName.get("The Stitched Corridor").factions())
				.containsExactly(TestFactions.WOUND, TestFactions.ARCHIVE);
		CardDefinition spell = byName.get("SCP-500");
		assertThat(spell.factionId()).isNull();
		assertThat(spell.rarity()).isEqualTo("Uncommon");
		assertThat(spell.subtype()).isEqualTo(CardSubtype.SHIELD);
		assertThat(spell.effectCode()).isEqualTo("SCP_500");
		CardDefinition terrain = byName.get("Echo Rift");
		assertThat(terrain.factionId()).isEqualTo(TestFactions.WOUND);
		assertThat(terrain.rarity()).isEqualTo("Rare");
		assertThat(terrain.attack()).isNull();
	}

	@Test
	@DisplayName("Ogni codice effetto del catalogo è registrato nel gioco, con il suo tipo di carta")
	void load_everyEffectCodeIsRegistered() {
		EffectRegistry registry = EffectRegistry.withAllEffects();

		assertThat(catalog).filteredOn(card -> card.effectCode() != null)
				.allSatisfy(card -> assertThat(registry.exists(card.effectCode(), card.type()))
						.as(card.name()).isTrue());
	}

	@Test
	@DisplayName("Una riga si divide alle virgole, tranne dentro le virgolette; due virgolette sono una virgoletta")
	void splitLine_quotedFields() {
		assertThat(CatalogCsv.splitLine("a,\"b, c\",\"d \"\"e\"\"\",,f"))
				.containsExactly("a", "b, c", "d \"e\"", "", "f");
	}
}
