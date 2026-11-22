package it.downfall.reversky.engine.core.effect;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;

/**
 * Controllo che il motore fa prima che l'effetto di una creatura colpisca la creatura avversaria: un Fascicolo
 * Sigillato 0 dell'avversario annulla del tutto la prima di queste attivazioni (D-47). Il motore lo dà a
 * {@link CreatureContext}, così gli effetti non conoscono gli Oggetti.
 */
@FunctionalInterface
public interface EnemyEffectGuard {

	/** Nessun controllo: non annulla mai (per i test degli effetti senza Oggetti). */
	EnemyEffectGuard NONE = (state, source, target) -> false;

	/**
	 * Indica se l'attivazione è annullata. Se lo è, chi annulla registra il fatto nel log.
	 *
	 * @param state  partita in corso
	 * @param source creatura il cui effetto sta per colpire
	 * @param target creatura avversaria che sarebbe colpita
	 * @return {@code true} se l'attivazione è annullata
	 */
	boolean cancels(MatchState state, CreatureInPlay source, CreatureInPlay target);
}
