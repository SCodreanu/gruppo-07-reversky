package it.downfall.reversky.engine.core.effect.impl;

import java.util.Optional;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectPriority;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * SCP-2521, Rare (Theory of the Archive), <i>Abilità rubata</i>: "all'entrata annulla l'effetto dell'avv. finché resta
 * in campo; se l'avv. è Rare o inferiore, SCP-2521 usa quell'effetto come proprio finché resta in campo".
 * <p>
 * Priorità {@code NULLIFY} (D-30). L'effetto rubato è una nuova istanza dal registro (ENG-38, deciso il 19 novembre):
 * parte come se SCP-2521 fosse appena entrata (il suo {@code onEnterPlay} scatta subito, il suo
 * {@code onCombatStart} nel passo dopo), "sé" e "alleate" si leggono dal lato di SCP-2521 e resta anche dopo la morte
 * della vittima. Un effetto rubato non ruba di nuovo (contro un altro SCP-2521 annulla soltanto). Contro Mythic,
 * Legendary e Mutant annulla soltanto. Rare o inferiore è {@code rarityOrder} ≤ 3, come per Bigfoot: va tenuto uguale
 * al seed delle rarità dell'API.
 */
public final class Scp2521Effect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_2521";

	/** {@code rarityOrder} di Rare. */
	private static final int RARE_ORDER = 3;

	/** Crea l'effetto. */
	public Scp2521Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public EffectPriority priority() {
		return EffectPriority.NULLIFY;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay enemy = ctx.targetEnemy().filter(EffectActions::hasEffect).orElse(null);
		if (enemy == null) {
			return;
		}
		boolean cancelled = enemy.cancelEffect(ModifierDuration.whileInPlay());
		Optional<CreatureEffect> stolen = enemy.card().rarityOrder() <= RARE_ORDER
				? ctx.borrowEffect(enemy.card().effectCode(), ModifierDuration.whileInPlay())
				: Optional.empty();
		if (!cancelled && stolen.isEmpty()) {
			return;
		}
		String steals = stolen.isPresent() ? " and steals it" : "";
		ctx.log().effectTriggered(ctx.self().side(), ctx.self().name(), EffectActions.cancel(enemy) + steals);
		stolen.ifPresent(effect -> effect.onEnterPlay(ctx.nextActivation()));
	}
}
