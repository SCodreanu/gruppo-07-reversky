package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * SCP-3999 (I Am At The Center Of Everything That Happens To Me), Legendary (Theory of the Archive), <i>Riscrittura
 * della realtà</i>: "all'entrata e in ogni round dal suo 2° in campo l'avv. perde tutti i bonus ATK e DEF, da
 * qualunque fonte, e torna ai valori base (i danni restano); ogni volta SCP-3999 ottiene +1 ATK, massimo +3". Prima era
 * ogni due round (3°, 5°, …); cambiato con ENG-42 (proposta del 21 novembre).
 * <p>
 * Si tolgono i modificatori di ATK positivi e gli scudi ({@link CreatureInPlay#removeBonuses()}, ENG-35, deciso il 18
 * novembre): la DEF permanente, bonus di fazione compreso, resta; il bonus di un Terreno non torna finché non arriva
 * un altro Terreno. Il +1 ATK arriva anche se il Fascicolo Sigillato 0 annulla l'attivazione sull'avversaria, come
 * il bonus di Smiler. Stato: i round in campo e l'ATK già guadagnato.
 */
public final class Scp3999Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_3999";

	private static final int FIRST_REPEAT_ROUND = 2;
	private static final int REPEAT_EVERY = 1;
	private static final int ATTACK_STEP = 1;
	private static final int MAX_ATTACK = 3;

	private int roundsInPlay;
	private int attackGained;

	/** Crea l'effetto. */
	public Scp3999Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		rewrite(ctx);
	}

	@Override
	public void onRoundStart(CreatureContext ctx) {
		roundsInPlay++;
		if (roundsInPlay >= FIRST_REPEAT_ROUND && (roundsInPlay - FIRST_REPEAT_ROUND) % REPEAT_EVERY == 0) {
			rewrite(ctx);
		}
	}

	private void rewrite(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		Optional<CreatureInPlay> enemy = ctx.targetEnemy();
		if (enemy.isPresent() && enemy.get().removeBonuses()) {
			CreatureInPlay target = enemy.get();
			parts.add(target.name() + " loses all ATK and DEF bonuses (ATK " + MatchLog.number(target.attack())
					+ ", DEF " + MatchLog.number(target.defense()) + ")");
		}
		if (attackGained < MAX_ATTACK) {
			attackGained += ATTACK_STEP;
			parts.add(EffectActions.attack(ctx.self(), ctx.self(), ATTACK_STEP, ModifierDuration.whileInPlay(), ""));
		}
		EffectActions.log(ctx, parts);
	}
}
