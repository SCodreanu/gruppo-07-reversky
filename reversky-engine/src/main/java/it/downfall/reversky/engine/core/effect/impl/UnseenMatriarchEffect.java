package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * The Unseen Matriarch, Mythic (Theory of the Dream), <i>Velo spezzato</i>: "la prima volta che la sua DEF scende a
 * metà o meno, l'avv. subisce danni pari alla DEF che ha perso (massimo 8) e −1 ATK finché resta in campo".
 * <p>
 * Soglia M-06 sulla propria DEF (≤ 50% della DEF all'entrata, bonus compresi), controllata dopo ogni fase
 * ({@code onDefenseCheck}) e solo se è ancora viva. La DEF persa conta solo le ferite vere dall'entrata, non la parte
 * assorbita dagli scudi. Una volta per entrata in campo (D-33): se in quel momento non può colpire l'avversaria (campo
 * vuoto, avversaria morta, Fascicolo Sigillato 0) l'effetto è consumato lo stesso (deciso il 18 novembre).
 */
public final class UnseenMatriarchEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "UNSEEN_MATRIARCH";

	private static final int MAX_DAMAGE = 8;
	private static final int ENEMY_ATTACK = -1;

	private boolean used;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onDefenseCheck(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		if (used || self.defense() * 2 > self.initialDefense()) {
			return;
		}
		used = true;
		int damage = Math.min(MAX_DAMAGE, self.woundsTaken());
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			if (damage > 0) {
				parts.add(EffectActions.damage(ctx, enemy, damage));
			}
			parts.add(EffectActions.attack(enemy, self, ENEMY_ATTACK, ModifierDuration.whileInPlay(),
					"while in play"));
		});
		EffectActions.log(ctx, parts);
	}
}
