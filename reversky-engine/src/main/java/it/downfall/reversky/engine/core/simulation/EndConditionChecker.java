package it.downfall.reversky.engine.core.simulation;

import java.util.Optional;

import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.rules.RuleConfig;

/**
 * Controlla se e come finisce la partita (docs/regole-base, "Fine della partita"; D-02, D-05).
 * <p>
 * {@link #check} si chiama dopo ogni round, quando i giocatori senza creatura in campo hanno già provato a pescarne
 * una: così "non ha più creature" vuol dire nessuna in campo e nessuna nel mazzo. Le condizioni si controllano in
 * ordine e vale la prima vera. I punti vita si controllano anche prima di ogni pescata ({@link #checkLifePoints},
 * ENG-29): un giocatore a 0 punti vita o meno ha già perso e non pesca più.
 */
public final class EndConditionChecker {

	private final RuleConfig rules;

	/**
	 * Crea il controllo per una partita.
	 *
	 * @param rules regole della partita, per il limite di round
	 */
	public EndConditionChecker(RuleConfig rules) {
		this.rules = rules;
	}

	/**
	 * Controlla le 5 condizioni di fine partita, in ordine.
	 *
	 * @param state stato della partita dopo il round e le pescate
	 * @return l'esito se la partita è finita, altrimenti vuoto
	 */
	public Optional<MatchEnd> check(MatchState state) {
		Optional<MatchEnd> lifePoints = checkLifePoints(state);
		if (lifePoints.isPresent()) {
			return lifePoints;
		}
		PlayerState challenger = state.challenger();
		PlayerState opponent = state.opponent();
		boolean challengerOut = !challenger.hasCreatures();
		boolean opponentOut = !opponent.hasCreatures();
		// 3 e 4: creature da schierare
		if (challengerOut && opponentOut) {
			return Optional.of(new MatchEnd(byLifePoints(challenger, opponent), EndReason.BOTH_DECKS_EMPTY));
		}
		if (challengerOut || opponentOut) {
			return Optional.of(new MatchEnd(challengerOut ? BattleResult.OPPONENT_WIN : BattleResult.CHALLENGER_WIN,
					EndReason.NO_CREATURES_LEFT));
		}
		// 5: limite di round totali della partita (D-02)
		if (state.round() >= rules.maxRounds()) {
			return Optional.of(new MatchEnd(byLifePoints(challenger, opponent), EndReason.ROUND_LIMIT));
		}
		return Optional.empty();
	}

	/**
	 * Controlla solo le condizioni 1 e 2, i punti vita. Il simulatore la chiama prima di ogni pescata (ENG-29): il
	 * danno al giocatore arriva solo da una morte singola, quindi un giocatore scende a 0 o meno da solo, e con questo
	 * controllo la partita finisce prima che il perdente peschi. La condizione 1 resta per sicurezza.
	 *
	 * @param state stato della partita
	 * @return l'esito se un giocatore è a 0 punti vita o meno, altrimenti vuoto
	 */
	public Optional<MatchEnd> checkLifePoints(MatchState state) {
		boolean challengerDown = state.challenger().lifePoints() <= 0;
		boolean opponentDown = state.opponent().lifePoints() <= 0;
		if (!challengerDown && !opponentDown) {
			return Optional.empty();
		}
		return Optional.of(challengerDown && opponentDown
				? new MatchEnd(BattleResult.DRAW, EndReason.BOTH_LIFE_POINTS_ZERO)
				: new MatchEnd(challengerDown ? BattleResult.OPPONENT_WIN : BattleResult.CHALLENGER_WIN,
						EndReason.LIFE_POINTS_ZERO));
	}

	/** Vince chi ha più punti vita; se sono uguali è pareggio (condizioni 3 e 5). */
	private static BattleResult byLifePoints(PlayerState challenger, PlayerState opponent) {
		int difference = Integer.compare(challenger.lifePoints(), opponent.lifePoints());
		if (difference == 0) {
			return BattleResult.DRAW;
		}
		return difference > 0 ? BattleResult.CHALLENGER_WIN : BattleResult.OPPONENT_WIN;
	}
}
