package it.downfall.reversky.engine.core.effect;

import java.util.Objects;
import java.util.Optional;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.PlayerState;

/**
 * Quello che l'effetto di una creatura vede quando il motore lo chiama: la creatura stessa, la creatura avversaria, i
 * due giocatori, il round e il log.
 * <p>
 * Si crea un contesto per ogni chiamata, cioè per ogni attivazione: {@link #targetEnemy()} controlla il Fascicolo
 * Sigillato 0 una volta sola per attivazione. Un effetto cambia la partita solo tramite questi oggetti, e la casualità
 * solo tramite {@link MatchState#random()}. Un effetto rubato o copiato si crea dal registro con
 * {@link #borrowEffect(String, ModifierDuration)} (ENG-38).
 */
public final class CreatureContext {

	private final MatchState state;
	private final CreatureInPlay self;
	private final EnemyEffectGuard guard;
	private final EffectRegistry effects;
	private boolean targetChecked;
	private CreatureInPlay target;

	/**
	 * Crea il contesto di un'attivazione.
	 *
	 * @param state partita in corso
	 * @param self  creatura che porta l'effetto
	 * @param guard   controllo del motore prima di colpire la creatura avversaria
	 * @param effects registro, per gli effetti rubati o copiati (ENG-38)
	 */
	public CreatureContext(MatchState state, CreatureInPlay self, EnemyEffectGuard guard, EffectRegistry effects) {
		this.state = Objects.requireNonNull(state, "state");
		this.self = Objects.requireNonNull(self, "self");
		this.guard = Objects.requireNonNull(guard, "guard");
		this.effects = Objects.requireNonNull(effects, "effects");
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
	 * Creatura avversaria da colpire (danno, malus, sigillo, annullamento): c'è solo se è in campo con DEF sopra 0, se
	 * gli effetti delle creature avversarie la colpiscono (non Il Custode delle Porte Incrociate, ENG-38) e se
	 * l'attivazione non è annullata dal Fascicolo Sigillato 0 dell'avversario. Il Custode si controlla prima, così il
	 * Fascicolo non si consuma. Il controllo si fa alla prima chiamata; le chiamate dopo, nella stessa attivazione,
	 * danno la stessa risposta.
	 *
	 * @return la creatura avversaria da colpire, o vuoto
	 */
	public Optional<CreatureInPlay> targetEnemy() {
		if (!targetChecked) {
			targetChecked = true;
			target = enemy()
					.filter(enemy -> !enemy.isDead())
					.filter(enemy -> !enemy.isImmuneToEnemyCreatureEffects())
					.filter(enemy -> !guard.cancels(state, self, enemy))
					.orElse(null);
		}
		return Optional.ofNullable(target);
	}

	/**
	 * Prende in prestito l'effetto di un'altra carta (ENG-38, deciso il 19 novembre): una nuova istanza dal registro,
	 * con contatori nuovi, che il motore chiama per questa creatura come se fosse suo. Non chiama nessun momento: per
	 * farlo partire "come appena entrato" l'effetto chiama i suoi momenti con {@link #nextActivation()}.
	 *
	 * @param code     codice dell'effetto di creatura
	 * @param duration {@code whileInPlay} (SCP-2521) o {@code untilCombatEnd} (Kitsune)
	 * @return il nuovo effetto, o vuoto se la creatura ne ha già uno in prestito (un effetto in prestito non ne prende
	 *         un altro)
	 */
	public Optional<CreatureEffect> borrowEffect(String code, ModifierDuration duration) {
		if (self.borrowedEffect().isPresent()) {
			return Optional.empty();
		}
		CreatureEffect borrowed = effects.createCreature(code);
		self.borrowEffect(borrowed, duration);
		return Optional.of(borrowed);
	}

	/**
	 * Contesto di una nuova attivazione della stessa creatura, per far partire un effetto in prestito: il Fascicolo
	 * Sigillato 0 si controlla di nuovo.
	 *
	 * @return un nuovo contesto
	 */
	public CreatureContext nextActivation() {
		return new CreatureContext(state, self, guard, effects);
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
