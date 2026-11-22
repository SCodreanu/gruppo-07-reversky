package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;

/**
 * The Undying Blossom, Mythic (Theory of the Dream), <i>Maledizione cremisi</i>: "una volta, quando subirebbe un danno
 * letale, resta con 4 DEF e perde i suoi bonus temporanei; l'avv. subisce 3 danni".
 * <p>
 * Vale contro ogni danno letale, al controllo delle morti dopo le prevenzioni e prima del Badge di Uscita Negata
 * ({@code onLethalDamage}, D-36, ENG-37). Bonus temporanei = bonus ATK e scudi con una durata a round o fino a fine
 * scontro (deciso il 18 novembre): si tolgono prima, così resta davvero con 4 DEF. In una morte doppia lei resta e
 * l'altra muore. Stato: se l'ha già usato in questa entrata.
 */
public final class UndyingBlossomEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "UNDYING_BLOSSOM";

	private static final int SURVIVAL_DEFENSE = 4;
	private static final int DAMAGE = 3;

	private boolean used;

	/** Crea l'effetto. */
	public UndyingBlossomEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public boolean onLethalDamage(CreatureContext ctx) {
		if (used) {
			return false;
		}
		used = true;
		CreatureInPlay self = ctx.self();
		boolean lostBonuses = self.removeTemporaryBonuses();
		self.surviveWith(SURVIVAL_DEFENSE);
		List<String> parts = new ArrayList<>();
		parts.add("survives with " + SURVIVAL_DEFENSE + " DEF" + (lostBonuses
				? " and loses its temporary bonuses (ATK " + MatchLog.number(self.attack()) + ")" : ""));
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, DAMAGE)));
		EffectActions.log(ctx, parts);
		return true;
	}
}
