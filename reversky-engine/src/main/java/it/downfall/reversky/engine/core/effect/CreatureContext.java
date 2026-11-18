package it.downfall.reversky.engine.core.effect;

import java.util.Objects;
import java.util.Optional;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;

/**
 * Quello che l'effetto di una creatura vede quando il motore lo chiama: la creatura stessa, la creatura avversaria, i
 * due giocatori, il round e il log.
 * <p>
 * Si crea un contesto per ogni chiamata, cioè per ogni attivazione: {@link #targetEnemy()} controlla il Fascicolo
 * Sigillato 0 una volta sola per attivazione. Un effetto cambia la partita solo tramite questi oggetti, e la casualità
 * solo tramite {@link MatchState#random()}.
 */
public final class CreatureContext {

	private final MatchState state;
	private final CreatureInPlay self;
	private final EnemyEffectGuard guard;
	private boolean targetChecked;
	private CreatureInPlay target;

	/**
	 * Crea il contesto di un'attivazione.
	 *
	 * @param state partita in corso
	 * @param self  creatura che porta l'effetto
	 * @param guard controllo del motore prima di colpire la creatura avversaria
	 */
	public CreatureContext(MatchState state, CreatureInPlay self, EnemyEffectGuard guard) {
		this.state = Objects.requireNonNull(state, "state");
		this.self = Objects.requireNonNull(self, "self");
		this.guard = Objects.requireNonNull(guard, "guard");
	}

	/**
	 * Partita in corso.
	 *
	 * @return lo stato della partita
	 */
	public MatchState state() {
		return state;
	}

	/**
	 * Creatura che porta l'effetto.
	 *
	 * @return la creatura
	 */
	public CreatureInPlay self() {
		return self;
	}

	/**
	 * Creatura avversaria in campo, anche se ha DEF 0 o meno. Per leggerla; per colpirla si usa
	 * {@link #targetEnemy()}.
	 *
	 * @return la creatura avversaria, o vuoto se il campo avversario è vuoto
	 */
	public Optional<CreatureInPlay> enemy() {
		return opponent().creature();
	}

	/**
	 * Creatura avversaria da colpire (danno, malus): c'è solo se è in campo con DEF sopra 0 e se l'attivazione non è
	 * annullata dal Fascicolo Sigillato 0 dell'avversario. Il controllo si fa alla prima chiamata; le chiamate dopo,
	 * nella stessa attivazione, danno la stessa risposta.
	 *
	 * @return la creatura avversaria da colpire, o vuoto
	 */
	public Optional<CreatureInPlay> targetEnemy() {
		if (!targetChecked) {
			targetChecked = true;
			target = enemy()
					.filter(enemy -> !enemy.isDead())
					.filter(enemy -> !guard.cancels(state, self, enemy))
					.orElse(null);
		}
		return Optional.ofNullable(target);
	}

	/**
	 * Giocatore che possiede la creatura.
	 *
	 * @return lo stato del proprietario
	 */
	public PlayerState owner() {
		return state.player(self.side());
	}

	/**
	 * Avversario del proprietario.
	 *
	 * @return lo stato dell'avversario
	 */
	public PlayerState opponent() {
		return state.opponentOf(self.side());
	}

	/**
	 * Round in corso; 0 prima del primo round.
	 *
	 * @return il numero del round
	 */
	public int round() {
		return state.round();
	}

	/**
	 * Log della partita, per gli eventi dell'effetto.
	 *
	 * @return il log
	 */
	public MatchLog log() {
		return state.log();
	}
}
