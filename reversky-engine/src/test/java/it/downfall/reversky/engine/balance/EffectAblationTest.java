package it.downfall.reversky.engine.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.balance.EffectAblation.Measure;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;

/**
 * Test veloci della misura del valore delle carte ({@code ENG-42}, fase 2): varianti delle carte, regressione e
 * partite rigiocate. La misura vera è in {@link EffectAblationSimulationTest}.
 */
class EffectAblationTest {

	private static final int MATCHES_PER_PAIRING = 2;

	private static List<CardDefinition> catalog;

	@BeforeAll
	static void loadCatalog() throws IOException {
		catalog = CatalogCsv.load(CatalogCsv.DEFAULT_PATH);
	}

	@Test
	@DisplayName("Senza effetto, una creatura perde solo il codice: stesse statistiche, stessa carta")
	void withoutEffect_creatureLosesOnlyTheCode() {
		CardDefinition card = byName("SCP-053");

		CardDefinition off = EffectAblation.withoutEffect(card);

		assertThat(off.effectCode()).isNull();
		assertThat(off).usingRecursiveComparison().ignoringFields("effectCode").isEqualTo(card);
	}

	@Test
	@DisplayName("Senza effetto, Spell, Hidden, Terreni e Oggetti hanno un effetto registrato che non fa niente")
	void withoutEffect_nonCreatureGetsADoNothingStub() {
		EffectRegistry registry = EffectAblation.registry();

		for (String name : List.of("SCP-500", "Last Sanctuary", "Echo Rift", "Fascicolo Sigillato 0")) {
			CardDefinition card = byName(name);

			CardDefinition off = EffectAblation.withoutEffect(card);

			assertThat(off.effectCode()).isEqualTo(EffectAblation.STUB_CODES.get(card.type()));
			assertThat(off).usingRecursiveComparison().ignoringFields("effectCode").isEqualTo(card);
			assertThat(registry.exists(off.effectCode(), card.type())).isTrue();
		}
		// gli effetti veri restano registrati
		assertThat(registry.exists(byName("SCP-500").effectCode(), CardType.SPELL)).isTrue();
	}

	@Test
	@DisplayName("La variante di taratura dà +1 DEF a una creatura; non vale per le altre carte")
	void withExtraDefense_onlyCreatures() {
		CardDefinition card = byName("Squonk");

		CardDefinition plus = EffectAblation.withExtraDefense(card);

		assertThat(plus.defense()).isEqualTo(card.defense() + 1);
		assertThat(plus).usingRecursiveComparison().ignoringFields("defense").isEqualTo(card);
		assertThatThrownBy(() -> EffectAblation.withExtraDefense(byName("SCP-500")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SCP-500");
	}

	@Test
	@DisplayName("Regressione sulla differenza di copie: guadagno esatto di mezzo punto per copia, errore zero")
	void fit_exactLinearGain() {
		Measure measure = EffectAblation.fit(new double[] { 1, 2, -1 }, new double[] { 0.5, 1.0, -0.5 });

		assertThat(measure.matches()).isEqualTo(3);
		assertThat(measure.perCopy()).isCloseTo(0.5, within(1e-12));
		assertThat(measure.standardError()).isCloseTo(0.0, within(1e-12));
	}

	@Test
	@DisplayName("Regressione con rumore: pendenza ai minimi quadrati e errore standard positivo")
	void fit_noisyGain() {
		Measure measure = EffectAblation.fit(new double[] { 1, 1, -1, -1 }, new double[] { 1.0, 0.0, 0.0, -1.0 });

		assertThat(measure.perCopy()).isCloseTo(0.5, within(1e-12));
		// residui 0,5, -0,5, 0,5, -0,5: varianza 1/3, somma dei quadrati delle copie 4
		assertThat(measure.standardError()).isCloseTo(Math.sqrt(1.0 / 3 / 4), within(1e-12));
	}

	@Test
	@DisplayName("Senza partite la misura non c'è: valore ed errore non sono numeri")
	void fit_noSamples() {
		Measure measure = EffectAblation.fit(new double[0], new double[0]);

		assertThat(measure.matches()).isZero();
		assertThat(measure.perCopy()).isNaN();
		assertThat(measure.standardError()).isNaN();
	}

	@Test
	@DisplayName("Il valore dell'effetto è il guadagno della carta senza effetto cambiato di segno")
	void measure_negated() {
		Measure measure = new Measure(10, -0.02, 0.01);

		assertThat(measure.negated()).isEqualTo(new Measure(10, 0.02, 0.01));
	}

	@Test
	@DisplayName("Una variante uguale alla carta non cambia niente: guadagno zero nelle partite rigiocate")
	void measure_identityVariantGainsNothing() {
		EffectAblation ablation = ablation();

		Measure measure = ablation.measure(byName("Squonk"), card -> card);

		assertThat(ablation.matches()).isEqualTo(16 * MATCHES_PER_PAIRING);
		assertThat(measure.matches()).isPositive().isLessThanOrEqualTo(ablation.matches());
		assertThat(measure.perCopy()).isZero();
	}

	@Test
	@DisplayName("Si rigiocano solo le partite in cui i due mazzi hanno un numero diverso di copie della carta")
	void measure_replaysOnlyMatchesWithACopyGap() {
		EffectAblation ablation = ablation();
		CardDefinition card = byName("Squonk");
		BalanceRunner runner = runner();
		long expected = BalancePairing.all().stream()
				.flatMap(pairing -> IntStream.range(0, MATCHES_PER_PAIRING).mapToObj(i -> runner.match(pairing, i)))
				.filter(match -> copies(match.challengerDeck(), card) != copies(match.opponentDeck(), card))
				.count();

		assertThat(ablation.measure(card, same -> same).matches()).isEqualTo((int) expected);
	}

	@Test
	@DisplayName("Le carte senza effetto si giocano con il registro dei finti: nessun errore, valore finito")
	void measure_withoutEffectRunsForEveryType() {
		EffectAblation ablation = ablation();

		for (String name : List.of("SCP-053", "Morso del Rovescio", "Echo Rift", "Badge di Uscita Negata")) {
			Measure measure = ablation.measure(byName(name), EffectAblation::withoutEffect);

			assertThat(measure.matches()).as(name).isPositive();
			assertThat(measure.perCopy()).as(name).isFinite();
		}
	}

	private static EffectAblation ablation() {
		return new EffectAblation(runner(), BalanceGrid.PV30.configs().get(2), 2);
	}

	private static BalanceRunner runner() {
		return new BalanceRunner(catalog, MATCHES_PER_PAIRING, BalanceRunner.BASE_SEED, EffectAblation.registry());
	}

	private static long copies(List<CardDefinition> deck, CardDefinition card) {
		return deck.stream().filter(c -> c.cardId() == card.cardId()).count();
	}

	private static CardDefinition byName(String name) {
		return catalog.stream().filter(card -> card.name().equals(name)).findFirst().orElseThrow();
	}
}
