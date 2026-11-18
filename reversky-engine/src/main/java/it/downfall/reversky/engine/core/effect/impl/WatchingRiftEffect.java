package it.downfall.reversky.engine.core.effect.impl;

import java.util.IdentityHashMap;
import java.util.Map;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlacedTerrain;

/**
 * Watching Rift (Crepa Occhi), Terreno della Theory of the Archive: bonus base +1 ATK e +1 DEF. Effetto secondario
 * (ENG-27): la prima Spell {@code DAMAGE} subita da ogni creatura della fazione in uno scontro fa 1 punto in meno in
 * totale, prima dalla DEF, poi dall'ATK.
 * <p>
 * Una Spell pescata fra due scontri conta per lo scontro dopo (deciso il 18 novembre,
 * {@link MatchState#combatWindow()}); una Spell annullata dal Fascicolo Sigillato 0 non arriva qui e non consuma
 * l'effetto.
 */
public final class WatchingRiftEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "WATCHING_RIFT";

	private static final int REDUCTION = 1;

	/** Per ogni creatura, l'ultimo scontro in cui una Spell è già stata ridotta; si legge solo per chiave. */
	private final Map<CreatureInPlay, Integer> reducedIn = new IdentityHashMap<>();

	/** Crea l'effetto. */
	public WatchingRiftEffect() {
		super(1, 1);
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int reduceDamageSpell(MatchState state, PlacedTerrain terrain, CreatureInPlay target, String spell) {
		int window = state.combatWindow();
		Integer last = reducedIn.put(target, window);
		if (last != null && last == window) {
			return 0;
		}
		state.log().effectTriggered(target.side(), target.name(),
				"takes " + REDUCTION + " point less from " + spell + " (" + terrain.card().name() + ")");
		return REDUCTION;
	}
}
