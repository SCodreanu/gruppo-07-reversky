package it.downfall.reversky.engine.core.model;

import java.util.Optional;
import java.util.random.RandomGenerator;

import it.downfall.reversky.engine.core.effect.TerrainEffect;
import it.downfall.reversky.engine.core.log.MatchLog;

/**
 * Stato di una partita in corso: i due giocatori, il numero del round, il Terreno in gioco, il log e il generatore
 * casuale creato dal seed.
 * <p>
 * Ogni simulazione crea il proprio stato: il simulatore non conserva niente tra una partita e l'altra
 * (CodingRequirements §7). Tutta la casualità passa da {@link #random()} (docs/mescolamento).
 */
public final class MatchState {

	private final PlayerState challenger;
	private final PlayerState opponent;
	private final RandomGenerator random;
	private final MatchLog log = new MatchLog();
	private int round;
	private int combatsStarted;
	/** Uno scontro è in corso: dal suo inizio alla prima morte. */
	private boolean combatRunning;
	private PlacedTerrain terrain;

	/**
	 * Crea lo stato all'inizio della partita, al round 0 (preparazione).
	 *
	 * @param challenger stato dello sfidante
	 * @param opponent   stato dell'avversario
	 * @param random     generatore creato dal seed, l'unico della partita
	 * @throws IllegalArgumentException se i lati dei giocatori non sono quelli attesi
	 */
	public MatchState(PlayerState challenger, PlayerState opponent, RandomGenerator random) {
		if (challenger.side() != Side.CHALLENGER || opponent.side() != Side.OPPONENT) {
			throw new IllegalArgumentException("Lati dei giocatori invertiti");
		}
		this.challenger = challenger;
		this.opponent = opponent;
		this.random = random;
	}

	/**
	 * Sfidante.
	 *
	 * @return lo stato dello sfidante
	 */
	public PlayerState challenger() {
		return challenger;
	}

	/**
	 * Avversario.
	 *
	 * @return lo stato dell'avversario
	 */
	public PlayerState opponent() {
		return opponent;
	}

	/**
	 * Giocatore di un lato.
	 *
	 * @param side il lato
	 * @return lo stato di quel giocatore
	 */
	public PlayerState player(Side side) {
		return side == Side.CHALLENGER ? challenger : opponent;
	}

	/**
	 * L'altro giocatore.
	 *
	 * @param side il lato di un giocatore
	 * @return lo stato del suo avversario
	 */
	public PlayerState opponentOf(Side side) {
		return side == Side.CHALLENGER ? opponent : challenger;
	}

	/**
	 * Generatore casuale della partita, creato dal seed.
	 *
	 * @return il generatore
	 */
	public RandomGenerator random() {
		return random;
	}

	/**
	 * Log della partita: ogni stato ne ha uno nuovo, che parte vuoto.
	 *
	 * @return il log
	 */
	public MatchLog log() {
		return log;
	}

	/**
	 * Round in corso o appena risolto; 0 durante la preparazione.
	 *
	 * @return il numero del round
	 */
	public int round() {
		return round;
	}

	/**
	 * Passa al round successivo.
	 *
	 * @return il numero del nuovo round
	 */
	public int startNextRound() {
		round++;
		return round;
	}

	/**
	 * Scontri iniziati finora, compresi quelli finiti prima del primo round (D-08); 0 durante la pescata iniziale.
	 *
	 * @return il numero dell'ultimo scontro iniziato
	 */
	public int combatsStarted() {
		return combatsStarted;
	}

	/**
	 * Inizia un nuovo scontro: una nuova coppia di creature si affronta.
	 *
	 * @return il numero del nuovo scontro, da 1
	 */
	public int startNextCombat() {
		combatsStarted++;
		combatRunning = true;
		return combatsStarted;
	}

	/** Lo scontro in corso è finito: è morta almeno una creatura (D-08). */
	public void endCombat() {
		combatRunning = false;
	}

	/**
	 * Scontro a cui conta quello che succede adesso, per gli effetti "una volta per scontro" dei Terreni (Echo Rift,
	 * Watching Rift, deciso il 18 novembre): lo scontro in corso o, fra due scontri, quello che sta per iniziare. Una
	 * Spell pescata dopo una morte colpisce la sopravvissuta per il suo prossimo scontro.
	 *
	 * @return il numero dello scontro, da 1
	 */
	public int combatWindow() {
		return combatRunning ? combatsStarted : combatsStarted + 1;
	}

	/**
	 * Terreno in gioco.
	 *
	 * @return il Terreno e chi l'ha pescato, o vuoto
	 */
	public Optional<PlacedTerrain> terrain() {
		return Optional.ofNullable(terrain);
	}

	/**
	 * Mette in gioco un Terreno, che sostituisce quello già in gioco, di chiunque sia (D-19).
	 *
	 * @param card   carta di tipo {@code TERRAIN}
	 * @param owner  chi l'ha pescato
	 * @param effect nuova istanza dell'effetto del Terreno, per questa messa in gioco
	 * @return il Terreno sostituito, o vuoto
	 * @throws IllegalArgumentException se la carta non è un Terreno
	 */
	public Optional<PlacedTerrain> placeTerrain(CardDefinition card, Side owner, TerrainEffect effect) {
		if (card.type() != CardType.TERRAIN) {
			throw new IllegalArgumentException("Non è un Terreno: carta " + card.cardId());
		}
		Optional<PlacedTerrain> replaced = terrain();
		terrain = new PlacedTerrain(card, owner, effect);
		return replaced;
	}

	/**
	 * Toglie il Terreno in gioco (es. The Null Witness, The Hollow Wyrm).
	 *
	 * @return il Terreno tolto, o vuoto se non ce n'era uno
	 */
	public Optional<PlacedTerrain> removeTerrain() {
		Optional<PlacedTerrain> removed = terrain();
		terrain = null;
		return removed;
	}
}
