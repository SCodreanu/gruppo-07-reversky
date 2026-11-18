package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.Penalties;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlacedTerrain;

/**
 * Devouring Abyss (Crepa Cavità), Terreno della Theory of the Dream: bonus base +1 ATK. Effetto secondario (ENG-27):
 * all'inizio di ogni scontro ogni creatura della fazione infligge 1 danno alla creatura avversaria.
 * <p>
 * Alla fine del passo 0, prima lo sfidante, con il controllo delle morti dopo ogni colpo (deciso il 18 novembre). È
 * danno da effetto (D-08) e la ferita ha come causa la creatura che colpisce; il Fascicolo Sigillato 0 non lo annulla,
 * perché non annulla i Terreni.
 */
public final class DevouringAbyssEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "DEVOURING_ABYSS";

	private static final int DAMAGE = 1;

	/** Crea l'effetto. */
	public DevouringAbyssEffect() {
		super(1, 0);
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onCombatStart(MatchState state, PlacedTerrain terrain, CreatureInPlay creature) {
		CreatureInPlay enemy = state.opponentOf(creature.side()).creature().filter(e -> !e.isDead()).orElse(null);
		if (enemy == null) {
			return;
		}
		int dealt = Penalties.defense(state, enemy, DAMAGE, creature.side(), creature);
		state.log().effectTriggered(creature.side(), creature.name(), "deals " + dealt + " damage to " + enemy.name()
				+ " with " + terrain.card().name() + " (DEF " + MatchLog.number(enemy.defense()) + ")");
	}
}
