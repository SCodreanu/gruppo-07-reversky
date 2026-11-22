package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * The Rootbound Doppelganger, Mutant (Theory of the Invitation + Theory of the Dream), <i>Imboscata a ogni
 * vittima</i>: "contro ogni creatura avversaria previene 3 danni dal suo primo attacco e le dà −2 ATK per i suoi due
 * round seguenti".
 * <p>
 * La prevenzione è quella del gruppo di Hidebehind, contro ogni avversaria (D-33). Il malus arriva all'inizio del
 * round dopo quel primo attacco, per due round (deciso il 18 novembre: i modificatori a round scadono subito dopo il
 * passo 6, quindi darlo a fine round lo farebbe durare un round solo); se intanto l'avversaria è cambiata, non arriva.
 * Il Fascicolo Sigillato 0 annulla il malus, non la prevenzione.
 */
public final class RootboundDoppelgangerEffect extends FirstAttackShieldEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "ROOTBOUND_DOPPELGANGER";

	private static final int PREVENTED = 3;
	private static final int ENEMY_ATTACK = -2;
	private static final int ROUNDS = 2;

	/** Avversaria il cui primo attacco è stato ridotto e che aspetta il malus, o {@code null}. */
	private CreatureInPlay awaitingMalus;

	/** Crea l'effetto. */
	public RootboundDoppelgangerEffect() {
		super(PREVENTED);
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	protected void afterFirstAttack(CreatureContext ctx, CreatureInPlay attacker) {
		awaitingMalus = attacker;
	}

	@Override
	public void onRoundStart(CreatureContext ctx) {
		CreatureInPlay victim = awaitingMalus;
		awaitingMalus = null;
		if (victim == null || ctx.enemy().orElse(null) != victim) {
			return;
		}
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.attack(enemy, ctx.self(), ENEMY_ATTACK,
				ModifierDuration.rounds(ROUNDS), "for its next two rounds")));
		EffectActions.log(ctx, parts);
	}
}
