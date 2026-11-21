package it.downfall.reversky.engine.balance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestEffects;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Misura del valore di una carta a partite appaiate ({@code ENG-42}, fase 2, scelta il 21 novembre).
 * <p>
 * Si giocano una volta le partite di una configurazione, con i mazzi casuali di {@link BalanceRunner}. Per misurare
 * una variante di una carta (senza effetto, o con +1 DEF per la taratura) si rigiocano, con gli stessi seed e gli
 * stessi mazzi, solo le partite in cui i due mazzi hanno un numero diverso di copie della carta; nelle altre la
 * variante tocca i due lati allo stesso modo. Il guadagno dello sfidante (punteggio con la variante meno punteggio
 * di prima) si mette in regressione sulla differenza di copie (sfidante meno avversario), senza intercetta: la
 * pendenza è il guadagno della variante per copia, in frazione di partita.
 * <p>
 * Le creature senza effetto perdono solo il codice; Spell, Hidden, Terreni e Oggetti prendono un effetto registrato
 * che non fa niente ({@link #STUB_CODES}), quindi occupano ancora il posto nel mazzo e il loro valore è quello di
 * tutta la carta (un Terreno perde anche il bonus base).
 */
public final class EffectAblation {

	/** Codice dell'effetto che non fa niente, per tipo di carta non creatura. */
	public static final Map<CardType, String> STUB_CODES = stubCodes();

	private final BalanceRunner runner;
	private final BalanceConfig config;
	private final List<BalanceMatch> matches = new ArrayList<>();
	private final List<Double> baseline = new ArrayList<>();

	/**
	 * Gioca le partite di base della configurazione. Le prime partite si giocano due volte e devono dare lo stesso
	 * risultato.
	 *
	 * @param runner            runner con il registro di {@link #registry()}
	 * @param config            regole e bonus di fazione
	 * @param determinismSample quante partite, dalla prima, giocare due volte
	 * @throws IllegalStateException per un errore dell'engine, una partita che non finisce o un risultato diverso
	 *                               alla seconda esecuzione
	 */
	public EffectAblation(BalanceRunner runner, BalanceConfig config, int determinismSample) {
		this.runner = runner;
		this.config = config;
		for (BalancePairing pairing : BalancePairing.all()) {
			for (int i = 0; i < runner.matchesPerPairing(); i++) {
				BalanceMatch match = runner.match(pairing, i);
				MatchResult result = runner.simulate(config, match);
				if (matches.size() < determinismSample && !runner.simulate(config, match).equals(result)) {
					throw new IllegalStateException("Risultato diverso alla seconda esecuzione, seed " + match.seed());
				}
				matches.add(match);
				baseline.add(challengerScore(result));
			}
		}
	}

	/**
	 * Registro del gioco più gli effetti che non fanno niente di {@link #STUB_CODES}.
	 *
	 * @return un nuovo registro
	 */
	public static EffectRegistry registry() {
		int faction = TestFactions.WOUND;
		return TestEffects.gameWithStubsFor(List.of(
				TestCards.spell(0, "No spell", CardSubtype.DAMAGE, STUB_CODES.get(CardType.SPELL)),
				TestCards.hidden(0, "No hidden", CardSubtype.DAMAGE, STUB_CODES.get(CardType.HIDDEN)),
				TestCards.terrain(0, "No terrain", faction, STUB_CODES.get(CardType.TERRAIN)),
				TestCards.item(0, "No item", faction, STUB_CODES.get(CardType.ITEM))));
	}

	/**
	 * La carta senza il suo effetto: una creatura senza codice, le altre carte con l'effetto che non fa niente.
	 *
	 * @param card carta del catalogo
	 * @return la variante, uguale alla carta salvo il codice dell'effetto
	 */
	public static CardDefinition withoutEffect(CardDefinition card) {
		return withCode(card, card.type() == CardType.CREATURE ? null : STUB_CODES.get(card.type()));
	}

	/**
	 * La creatura con +1 DEF, per la taratura: 1 DEF vale 1 punto di V.
	 *
	 * @param card creatura del catalogo
	 * @return la variante
	 * @throws IllegalArgumentException se la carta non è una creatura
	 */
	public static CardDefinition withExtraDefense(CardDefinition card) {
		if (card.type() != CardType.CREATURE) {
			throw new IllegalArgumentException("La taratura con +1 DEF vale solo per le creature: " + card.name());
		}
		return new CardDefinition(card.cardId(), card.name(), card.type(), card.rarity(), card.rarityOrder(),
				card.factionId(), card.secondaryFactionId(), card.attack(), card.defense() + 1, card.subtype(),
				card.effectCode());
	}

	/**
	 * Regressione senza intercetta del guadagno sulla differenza di copie.
	 *
	 * @param copyGaps differenza di copie della carta, sfidante meno avversario, mai zero
	 * @param gains    guadagno dello sfidante con la variante, nella stessa partita
	 * @return pendenza ed errore standard; {@code NaN} senza partite (l'errore anche con una sola)
	 */
	public static Measure fit(double[] copyGaps, double[] gains) {
		int n = copyGaps.length;
		double sumGapGain = 0;
		double sumGapSquared = 0;
		for (int i = 0; i < n; i++) {
			sumGapGain += copyGaps[i] * gains[i];
			sumGapSquared += copyGaps[i] * copyGaps[i];
		}
		if (n == 0) {
			return new Measure(0, Double.NaN, Double.NaN);
		}
		double slope = sumGapGain / sumGapSquared;
		double residuals = 0;
		for (int i = 0; i < n; i++) {
			double residual = gains[i] - slope * copyGaps[i];
			residuals += residual * residual;
		}
		double error = n > 1 ? Math.sqrt(residuals / (n - 1) / sumGapSquared) : Double.NaN;
		return new Measure(n, slope, error);
	}

	/**
	 * Partite di base giocate.
	 *
	 * @return 16 accoppiamenti × partite per accoppiamento
	 */
	public int matches() {
		return matches.size();
	}

	/**
	 * Guadagno per copia di una variante della carta, rigiocando le partite in cui i due mazzi ne hanno un numero
	 * diverso di copie.
	 *
	 * @param card    carta del catalogo
	 * @param variant la variante da provare al posto della carta, in tutti e due i mazzi
	 * @return guadagno per copia della variante rispetto alla carta, in frazione di partita
	 * @throws IllegalStateException per un errore dell'engine o una partita che non finisce
	 */
	public Measure measure(CardDefinition card, UnaryOperator<CardDefinition> variant) {
		CardDefinition replacement = variant.apply(card);
		List<Double> copyGaps = new ArrayList<>();
		List<Double> gains = new ArrayList<>();
		for (int i = 0; i < matches.size(); i++) {
			BalanceMatch match = matches.get(i);
			int gap = copies(match.challengerDeck(), card) - copies(match.opponentDeck(), card);
			if (gap != 0) {
				BalanceMatch replayed = new BalanceMatch(match.pairing(), match.seed(),
						replace(match.challengerDeck(), card, replacement),
						replace(match.opponentDeck(), card, replacement));
				copyGaps.add((double) gap);
				gains.add(challengerScore(runner.simulate(config, replayed)) - baseline.get(i));
			}
		}
		return fit(copyGaps.stream().mapToDouble(Double::doubleValue).toArray(),
				gains.stream().mapToDouble(Double::doubleValue).toArray());
	}

	private static int copies(List<CardDefinition> deck, CardDefinition card) {
		return (int) deck.stream().filter(c -> c.cardId() == card.cardId()).count();
	}

	private static List<CardDefinition> replace(List<CardDefinition> deck, CardDefinition card,
			CardDefinition replacement) {
		return deck.stream().map(c -> c.cardId() == card.cardId() ? replacement : c).toList();
	}

	private static double challengerScore(MatchResult result) {
		return switch (result.result()) {
			case CHALLENGER_WIN -> 1.0;
			case DRAW -> 0.5;
			case OPPONENT_WIN -> 0.0;
		};
	}

	private static CardDefinition withCode(CardDefinition card, String code) {
		return new CardDefinition(card.cardId(), card.name(), card.type(), card.rarity(), card.rarityOrder(),
				card.factionId(), card.secondaryFactionId(), card.attack(), card.defense(), card.subtype(), code);
	}

	private static Map<CardType, String> stubCodes() {
		Map<CardType, String> codes = new EnumMap<>(CardType.class);
		codes.put(CardType.SPELL, "ABLATION_NO_SPELL");
		codes.put(CardType.HIDDEN, "ABLATION_NO_HIDDEN");
		codes.put(CardType.TERRAIN, "ABLATION_NO_TERRAIN");
		codes.put(CardType.ITEM, "ABLATION_NO_ITEM");
		return Map.copyOf(codes);
	}

	/**
	 * Guadagno per copia di una variante.
	 *
	 * @param matches       partite rigiocate
	 * @param perCopy       guadagno per copia in più nel mazzo, in frazione di partita
	 * @param standardError errore standard della pendenza
	 */
	public record Measure(int matches, double perCopy, double standardError) {

		/**
		 * Lo stesso guadagno con il segno cambiato: il valore dell'effetto è il guadagno della carta senza effetto
		 * cambiato di segno.
		 *
		 * @return nuova misura
		 */
		public Measure negated() {
			return new Measure(matches, -perCopy, standardError);
		}
	}
}
