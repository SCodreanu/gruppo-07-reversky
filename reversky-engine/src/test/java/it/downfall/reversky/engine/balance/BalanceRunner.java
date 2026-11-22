package it.downfall.reversky.engine.balance;

import java.util.List;
import java.util.SplittableRandom;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.simulation.MatchSimulator;
import it.downfall.reversky.engine.support.TestFactions;
import it.downfall.reversky.engine.support.TestInputs;

/**
 * Gioca le partite del bilanciamento con il {@link MatchSimulator} vero e il registro di tutti gli effetti.
 * <p>
 * La partita {@code i} dell'accoppiamento {@code p} ha seed {@code base + p × partitePerAccoppiamento + i}: lo stesso
 * seed costruisce i due mazzi e mescola l'engine, ed è uguale in tutte le configurazioni, così le configurazioni
 * giocano gli stessi mazzi e la stessa partita cambia solo per le regole (deciso il 20 novembre). I mazzi si
 * costruiscono con uno {@link SplittableRandom} creato dal seed: l'engine mescola con un {@code java.util.Random} dello
 * stesso seed, e con lo stesso generatore la scelta delle carte e l'ordine del mescolamento sarebbero correlati.
 * <p>
 * Ogni partita deve finire con {@code MATCH_ENDED} entro il limite di round; un'eccezione dell'engine diventa una
 * {@link IllegalStateException} con configurazione, accoppiamento e seed, per riprodurla.
 */
public final class BalanceRunner {

	/** Seed base del bilanciamento. */
	public static final long BASE_SEED = 20_261_120L;

	private final BalanceDeckBuilder decks;
	private final int matchesPerPairing;
	private final long baseSeed;
	private final MatchSimulator simulator;

	/**
	 * Crea il runner con gli effetti del gioco.
	 *
	 * @param catalog           carte del catalogo, nell'ordine del CSV
	 * @param matchesPerPairing partite per ogni accoppiamento di fazioni
	 * @param baseSeed          seed della prima partita
	 */
	public BalanceRunner(List<CardDefinition> catalog, int matchesPerPairing, long baseSeed) {
		this(catalog, matchesPerPairing, baseSeed, EffectRegistry.withAllEffects());
	}

	/**
	 * Crea il runner con un registro degli effetti dato, per esempio con gli effetti finti di {@link EffectAblation}.
	 *
	 * @param catalog           carte del catalogo, nell'ordine del CSV
	 * @param matchesPerPairing partite per ogni accoppiamento di fazioni
	 * @param baseSeed          seed della prima partita
	 * @param registry          effetti che l'engine usa
	 */
	public BalanceRunner(List<CardDefinition> catalog, int matchesPerPairing, long baseSeed,
			EffectRegistry registry) {
		this.decks = new BalanceDeckBuilder(catalog);
		this.matchesPerPairing = matchesPerPairing;
		this.baseSeed = baseSeed;
		this.simulator = new MatchSimulator(registry);
	}

	/**
	 * Partite per accoppiamento.
	 *
	 * @return numero di partite per ognuno dei 16 accoppiamenti
	 */
	public int matchesPerPairing() {
		return matchesPerPairing;
	}

	/**
	 * Prepara una partita: seed e mazzi.
	 *
	 * @param pairing accoppiamento di fazioni
	 * @param match   numero della partita nell'accoppiamento, da 0
	 * @return la partita da simulare
	 */
	public BalanceMatch match(BalancePairing pairing, int match) {
		long seed = baseSeed + (long) pairing.index() * matchesPerPairing + match;
		SplittableRandom random = new SplittableRandom(seed);
		List<CardDefinition> challengerDeck = decks.build(pairing.challengerFactionId(), random);
		List<CardDefinition> opponentDeck = decks.build(pairing.opponentFactionId(), random);
		return new BalanceMatch(pairing, seed, challengerDeck, opponentDeck);
	}

	/**
	 * Simula una partita con le regole di una configurazione e ne controlla le invarianti.
	 *
	 * @param config regole e bonus di fazione
	 * @param match  partita preparata da {@link #match}
	 * @return il risultato dell'engine
	 * @throws IllegalStateException se l'engine lancia un'eccezione o la partita non finisce come deve
	 */
	public MatchResult simulate(BalanceConfig config, BalanceMatch match) {
		SimulationInput input = new SimulationInput(match.seed(), config.rules(), TestFactions.all(),
				config.factionBonuses(), new PlayerInput(TestInputs.CHALLENGER_USER_ID, match.challengerDeck()),
				new PlayerInput(TestInputs.OPPONENT_USER_ID, match.opponentDeck()));
		MatchResult result;
		try {
			result = simulator.simulate(input);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Errore dell'engine in " + describe(config, match), e);
		}
		if (result.log().isEmpty() || result.log().getLast().type() != EventType.MATCH_ENDED
				|| result.totalRounds() > config.rules().maxRounds()) {
			throw new IllegalStateException("Partita non finita correttamente in " + describe(config, match) + ": "
					+ result.totalRounds() + " round, motivo " + result.endReason());
		}
		return result;
	}

	/**
	 * Gioca tutte le partite di una configurazione. Le prime partite si giocano due volte e devono dare lo stesso
	 * risultato, log compreso (controllo del determinismo deciso il 20 novembre).
	 *
	 * @param config            regole e bonus di fazione
	 * @param determinismSample quante partite, dalla prima, giocare due volte
	 * @return le statistiche della configurazione
	 * @throws IllegalStateException per un errore dell'engine, una partita che non finisce o un risultato diverso
	 *                               alla seconda esecuzione
	 */
	public BalanceStats run(BalanceConfig config, int determinismSample) {
		BalanceStats stats = new BalanceStats(BalanceConfig.FACTIONS, config.rules().maxRounds());
		int played = 0;
		for (BalancePairing pairing : BalancePairing.all()) {
			for (int i = 0; i < matchesPerPairing; i++) {
				BalanceMatch match = match(pairing, i);
				MatchResult result = simulate(config, match);
				if (played < determinismSample && !simulate(config, match).equals(result)) {
					throw new IllegalStateException("Risultato diverso alla seconda esecuzione in "
							+ describe(config, match));
				}
				stats.record(match, result);
				played++;
			}
		}
		return stats;
	}

	private static String describe(BalanceConfig config, BalanceMatch match) {
		BalancePairing pairing = match.pairing();
		return "configurazione " + config.id() + ", " + BalanceConfig.factionName(pairing.challengerFactionId())
				+ " contro " + BalanceConfig.factionName(pairing.opponentFactionId()) + ", seed " + match.seed();
	}
}
