package it.downfall.reversky.engine.balance;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Statistiche di una configurazione del bilanciamento, raccolte partita per partita senza tenere i log in memoria.
 * <p>
 * Ogni percentuale è un punteggio: vittoria 1, pareggio ½, sconfitta 0 (deciso il 20 novembre), così i due lati di
 * una partita sommano sempre al 100%. Le vittorie per fazione e gli scontri diretti non contano le partite speculari
 * (stessa fazione); il primo giocatore, la durata e le carte le contano tutte.
 * <p>
 * Una carta conta una volta per lato e per partita, anche con più copie: "nel mazzo" se è nel mazzo di quel lato,
 * "giocata" se il log ha un evento di quel lato con il suo nome tra {@code CREATURE_ENTERED}, {@code SPELL_RESOLVED},
 * {@code HIDDEN_TRIGGERED}, {@code TERRAIN_PLACED} e {@code ITEM_PLACED}. Le attivazioni contano gli eventi
 * {@code EFFECT_TRIGGERED}, {@code SPELL_RESOLVED}, {@code HIDDEN_TRIGGERED} e {@code ITEM_CONSUMED} con il suo nome;
 * per le creature {@code EFFECT_TRIGGERED} comprende anche i bonus ricevuti da Spell, Terreni e Oggetti, perché il
 * log li scrive con il nome della creatura che li riceve.
 */
public final class BalanceStats {

	private static final Set<EventType> PLAYED = EnumSet.of(EventType.CREATURE_ENTERED, EventType.SPELL_RESOLVED,
			EventType.HIDDEN_TRIGGERED, EventType.TERRAIN_PLACED, EventType.ITEM_PLACED);
	private static final Set<EventType> TRIGGERS = EnumSet.of(EventType.EFFECT_TRIGGERED, EventType.SPELL_RESOLVED,
			EventType.HIDDEN_TRIGGERED, EventType.ITEM_CONSUMED);

	private final List<Integer> factionIds;
	private final int[] roundsHistogram;
	private final Map<EndReason, Integer> endReasons = new EnumMap<>(EndReason.class);
	private final Map<Integer, Counter> factions = new HashMap<>();
	private final Map<List<Integer>, Counter> matchups = new HashMap<>();
	private final Counter firstPlayer = new Counter();
	private final Map<Integer, Counter> inDeck = new HashMap<>();
	private final Map<Integer, Counter> played = new HashMap<>();
	private final Map<Integer, Integer> triggers = new HashMap<>();
	private int matches;
	private long totalRounds;

	/**
	 * Crea statistiche vuote.
	 *
	 * @param factionIds fazioni del bilanciamento, nell'ordine del report
	 * @param maxRounds  limite di round della configurazione: nessuna partita può durare di più
	 */
	public BalanceStats(List<Integer> factionIds, int maxRounds) {
		this.factionIds = List.copyOf(factionIds);
		this.roundsHistogram = new int[maxRounds + 1];
	}

	/**
	 * Aggiunge una partita simulata.
	 *
	 * @param match  accoppiamento e mazzi della partita
	 * @param result risultato dell'engine
	 * @throws IllegalStateException se la partita dura più del limite di round
	 */
	public void record(BalanceMatch match, MatchResult result) {
		if (result.totalRounds() < 0 || result.totalRounds() >= roundsHistogram.length) {
			throw new IllegalStateException("Partita di " + result.totalRounds() + " round, oltre il limite di "
					+ (roundsHistogram.length - 1));
		}
		matches++;
		totalRounds += result.totalRounds();
		roundsHistogram[result.totalRounds()]++;
		endReasons.merge(result.endReason(), 1, Integer::sum);
		BattleResult outcome = result.result();
		firstPlayer.add(outcome, Side.CHALLENGER);
		BalancePairing pairing = match.pairing();
		if (!pairing.isMirror()) {
			int challenger = pairing.challengerFactionId();
			int opponent = pairing.opponentFactionId();
			factions.computeIfAbsent(challenger, id -> new Counter()).add(outcome, Side.CHALLENGER);
			factions.computeIfAbsent(opponent, id -> new Counter()).add(outcome, Side.OPPONENT);
			matchups.computeIfAbsent(List.of(challenger, opponent), key -> new Counter()).add(outcome, Side.CHALLENGER);
			matchups.computeIfAbsent(List.of(opponent, challenger), key -> new Counter()).add(outcome, Side.OPPONENT);
		}
		recordCards(match.challengerDeck(), Side.CHALLENGER, result);
		recordCards(match.opponentDeck(), Side.OPPONENT, result);
	}

	/**
	 * Partite registrate.
	 *
	 * @return numero di partite
	 */
	public int matches() {
		return matches;
	}

	/**
	 * Fazioni del bilanciamento.
	 *
	 * @return id nell'ordine del report
	 */
	public List<Integer> factionIds() {
		return factionIds;
	}

	/**
	 * Partite finite per un motivo.
	 *
	 * @param reason motivo di fine
	 * @return numero di partite, 0 se nessuna
	 */
	public int endReasonCount(EndReason reason) {
		return endReasons.getOrDefault(reason, 0);
	}

	/**
	 * Durata media in round.
	 *
	 * @return media, 0 senza partite
	 */
	public double averageRounds() {
		return matches == 0 ? 0 : (double) totalRounds / matches;
	}

	/**
	 * Partita più corta.
	 *
	 * @return round della partita più corta, 0 senza partite
	 */
	public int shortestMatch() {
		return roundsPercentile(0);
	}

	/**
	 * Partita più lunga.
	 *
	 * @return round della partita più lunga, 0 senza partite
	 */
	public int longestMatch() {
		return roundsPercentile(100);
	}

	/**
	 * Percentile della durata con il metodo del rango più vicino: la durata della partita in posizione
	 * {@code ceil(percent / 100 × partite)} in ordine crescente (almeno la prima).
	 *
	 * @param percent percentile, da 0 a 100
	 * @return round, 0 senza partite
	 */
	public int roundsPercentile(int percent) {
		long rank = Math.max(1, ((long) percent * matches + 99) / 100);
		long seen = 0;
		for (int rounds = 0; rounds < roundsHistogram.length; rounds++) {
			seen += roundsHistogram[rounds];
			if (seen >= rank) {
				return rounds;
			}
		}
		return 0;
	}

	/**
	 * Punteggio di una fazione contro le altre, senza le partite speculari.
	 *
	 * @param factionId id della fazione
	 * @return punteggio, con 0 partite se non ne ha giocate
	 */
	public Score faction(int factionId) {
		return score(factions.get(factionId));
	}

	/**
	 * Punteggio di una fazione contro un'altra, sommando i due posti (sfidante e avversario).
	 *
	 * @param factionId id della fazione
	 * @param againstId id della fazione avversaria
	 * @return punteggio della prima fazione; 0 partite per le speculari
	 */
	public Score matchup(int factionId, int againstId) {
		return score(matchups.get(List.of(factionId, againstId)));
	}

	/**
	 * Punteggio dello sfidante, che pesca per primo, su tutte le partite.
	 *
	 * @return punteggio dello sfidante
	 */
	public Score firstPlayer() {
		return firstPlayer.score();
	}

	/**
	 * Punteggio del lato che ha la carta nel mazzo.
	 *
	 * @param cardId id della carta
	 * @return punteggio, con 0 partite se non è mai stata nel mazzo
	 */
	public Score inDeck(int cardId) {
		return score(inDeck.get(cardId));
	}

	/**
	 * Punteggio del lato che ha giocato la carta.
	 *
	 * @param cardId id della carta
	 * @return punteggio, con 0 partite se non è mai stata giocata
	 */
	public Score played(int cardId) {
		return score(played.get(cardId));
	}

	/**
	 * Attivazioni totali della carta.
	 *
	 * @param cardId id della carta
	 * @return numero di eventi di attivazione con il suo nome
	 */
	public int triggers(int cardId) {
		return triggers.getOrDefault(cardId, 0);
	}

	private void recordCards(List<CardDefinition> deck, Side side, MatchResult result) {
		Map<String, Integer> idsByName = new HashMap<>();
		for (CardDefinition card : deck) {
			idsByName.putIfAbsent(card.name(), card.cardId());
		}
		Set<Integer> playedIds = new HashSet<>();
		for (MatchEvent event : result.log()) {
			Integer cardId = event.side() == side ? idsByName.get(String.valueOf(event.data().get("card"))) : null;
			if (cardId == null) {
				continue;
			}
			if (PLAYED.contains(event.type())) {
				playedIds.add(cardId);
			}
			if (TRIGGERS.contains(event.type())) {
				triggers.merge(cardId, 1, Integer::sum);
			}
		}
		for (Integer cardId : idsByName.values()) {
			inDeck.computeIfAbsent(cardId, id -> new Counter()).add(result.result(), side);
		}
		for (Integer cardId : playedIds) {
			played.computeIfAbsent(cardId, id -> new Counter()).add(result.result(), side);
		}
	}

	private static Score score(Counter counter) {
		return counter == null ? new Score(0, 0, 0) : counter.score();
	}

	/**
	 * Punteggio su un insieme di partite: vittoria 1, pareggio ½, sconfitta 0.
	 *
	 * @param matches partite
	 * @param wins    vittorie
	 * @param draws   pareggi
	 */
	public record Score(int matches, int wins, int draws) {

		/**
		 * Sconfitte.
		 *
		 * @return partite meno vittorie e pareggi
		 */
		public int losses() {
			return matches - wins - draws;
		}

		/**
		 * Punteggio medio.
		 *
		 * @return da 0 a 1; 0 senza partite
		 */
		public double rate() {
			return matches == 0 ? 0 : (wins + draws / 2.0) / matches;
		}
	}

	/** Contatore modificabile di un punteggio. */
	private static final class Counter {

		private int matches;
		private int wins;
		private int draws;

		void add(BattleResult outcome, Side side) {
			matches++;
			if (outcome == BattleResult.DRAW) {
				draws++;
			} else if ((outcome == BattleResult.CHALLENGER_WIN) == (side == Side.CHALLENGER)) {
				wins++;
			}
		}

		Score score() {
			return new Score(matches, wins, draws);
		}
	}
}
