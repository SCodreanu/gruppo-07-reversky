package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * Kitsune, Legendary (Theory of the Dream), <i>Mille forme</i>: "finché sono entrambe in campo copia l'effetto
 * dell'avv. (l'avv. tiene il suo); se copia qualcosa, +3 DEF; sigilla inoltre fino a 3 punti di bonus ATK dell'avv.
 * finché resta in campo".
 * <p>
 * A ogni inizio di scontro, quindi all'entrata e contro ogni nuova avversaria (ENG-38, deciso il 19 novembre): la
 * copia è una nuova istanza dal registro, parte come appena entrata ({@code onEnterPlay} e {@code onCombatStart}
 * subito) e si toglie a fine scontro; quello che ha già dato a Kitsune resta. +3 DEF permanente una volta per copia.
 * Il sigillo dura fino a fine scontro e non tocca il bonus di fazione (D-35). Non copia un'altra Kitsune; Il Custode
 * delle Porte Incrociate non si copia e non si sigilla, perché è immune ({@link CreatureContext#targetEnemy()}).
 * Copia anche un effetto annullato (conta il codice della carta).
 */
public final class KitsuneEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "KITSUNE";

	private static final int DEFENSE_GAIN = 3;
	private static final int SEAL = 3;

	/** Crea l'effetto. */
	public KitsuneEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onCombatStart(CreatureContext ctx) {
		CreatureInPlay enemy = ctx.targetEnemy().orElse(null);
		if (enemy == null) {
			return;
		}
		CreatureInPlay self = ctx.self();
		List<String> parts = new ArrayList<>();
		Optional<CreatureEffect> copy = Optional.empty();
		if (EffectActions.hasEffect(enemy) && !CODE.equals(enemy.card().effectCode())) {
			copy = ctx.borrowEffect(enemy.card().effectCode(), ModifierDuration.untilCombatEnd());
		}
		copy.ifPresent(effect -> {
			parts.add("copies " + enemy.name() + "'s effect");
			parts.add(EffectActions.defense(self, DEFENSE_GAIN));
		});
		parts.add(EffectActions.seal(enemy, self, SEAL, ModifierDuration.untilCombatEnd()));
		EffectActions.log(ctx, parts);
		copy.ifPresent(effect -> {
			effect.onEnterPlay(ctx.nextActivation());
			effect.onCombatStart(ctx.nextActivation());
		});
	}
}
