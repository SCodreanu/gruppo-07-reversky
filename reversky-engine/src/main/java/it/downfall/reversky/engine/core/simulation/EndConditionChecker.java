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
 * Si chiama dopo ogni round, quando i giocatori senza creatura in campo hanno già provato a pescarne una: così
 * "non ha più creature" vuol dire nessuna in campo e nessuna nel mazzo. Le condizioni si controllano in ordine e
 * vale la prima vera.
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
		PlayerState challenger = state.challenger();
		PlayerState opponent = state.opponent();
		boolean challengerDown = challenger.lifePoints() <= 0;
		boolean opponentDown = opponent.lifePoints() <= 0;
		// 1 e 2: punti vita
		if (challengerDown || opponentDown) {
			return Optional.of(challengerDown && opponentDown
					? new MatchEnd(BattleResult.DRAW, EndReason.BOTH_LIFE_POINTS_ZERO)
					: new MatchEnd(challengerDown ? BattleResult.OPPONENT_WIN : BattleResult.CHALLENGER_WIN,
							EndReason.LIFE_POINTS_ZERO));
		}
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

	/** Vince chi ha più punti vita; se sono uguali è pareggio (condizioni 3 e 5). */
	private static BattleResult byLifePoints(PlayerState challenger, PlayerState opponent) {
		int difference = Integer.compare(challenger.lifePoints(), opponent.lifePoints());
		if (difference == 0) {
			return BattleResult.DRAW;
		}
		return difference > 0 ? BattleResult.CHALLENGER_WIN : BattleResult.OPPONENT_WIN;
	}
}
