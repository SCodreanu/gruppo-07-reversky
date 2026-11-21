package it.downfall.reversky.engine.core.effect;

import java.util.Objects;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Quello che un effetto di Spell, Hidden, Terreno od Oggetto vede quando il motore lo chiama: la partita, il
 * giocatore che possiede la carta, il suo avversario e la carta stessa.
 * <p>
 * Un effetto cambia la partita solo tramite questi oggetti, e la casualità solo tramite il generatore della partita
 * ({@link MatchState#random()}), così la partita resta riproducibile.
 */
public final class EffectContext {

	private final MatchState state;
	private final Side owner;
	private final CardDefinition card;

	/**
	 * Crea il contesto.
	 *
	 * @param state partita in corso
	 * @param owner lato del giocatore che possiede la carta
	 * @param card  carta che porta l'effetto
	 */
	public EffectContext(MatchState state, Side owner, CardDefinition card) {
		this.state = Objects.requireNonNull(state, "state");
		this.owner = Objects.requireNonNull(owner, "owner");
		this.card = Objects.requireNonNull(card, "card");
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
	 * Giocatore che possiede la carta.
	 *
	 * @return lo stato del proprietario
	 */
	public PlayerState owner() {
		return state.player(owner);
	}

	/**
	 * Avversario del proprietario.
	 *
	 * @return lo stato dell'avversario
	 */
	public PlayerState opponent() {
		return state.opponentOf(owner);
	}

	/**
	 * Carta che porta l'effetto.
	 *
	 * @return la carta
	 */
	public CardDefinition card() {
		return card;
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
