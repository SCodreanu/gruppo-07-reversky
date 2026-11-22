package it.downfall.reversky.engine.core.effect.impl;

import java.util.IdentityHashMap;
import java.util.Map;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.StatModifier;
import it.downfall.reversky.engine.core.model.Wound;

/**
 * Echo Rift (Crepa Eco), Terreno della Theory of the Wound: bonus base +1 ATK. Effetto secondario, <i>Eco delle
 * ferite</i> (ENG-27): la prima volta che ogni creatura della fazione viene ferita in uno scontro, +1 ATK per il resto
 * di quello scontro.
 * <p>
 * Ferita = perdita reale di DEF sopra 0, da un attacco o da un effetto. Una ferita fra due scontri conta per lo scontro
 * dopo (deciso il 18 novembre, {@link MatchState#combatWindow()}). Il +1 ha origine {@code TERRAIN}, quindi sparisce
 * anche se il Terreno esce.
 */
public final class EchoRiftEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "ECHO_RIFT";

	private static final int WOUND_ATTACK = 1;

	/** Per ogni creatura, l'ultimo scontro in cui ha già avuto il +1; si legge solo per chiave. */
	private final Map<CreatureInPlay, Integer> rewardedIn = new IdentityHashMap<>();

	/** Crea l'effetto. */
	public EchoRiftEffect() {
		super(1, 0);
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onWounded(MatchState state, PlacedTerrain terrain, CreatureInPlay creature, Wound wound) {
		int window = state.combatWindow();
		Integer last = rewardedIn.put(creature, window);
		if (last != null && last == window) {
			return;
		}
		creature.addModifier(StatModifier.attack(ModifierSource.TERRAIN, terrain.owner(), WOUND_ATTACK,
				ModifierDuration.untilCombatEnd()));
		state.log().effectTriggered(creature.side(), creature.name(), MatchLog.signed(WOUND_ATTACK)
				+ " ATK this combat from " + terrain.card().name()
				+ " (ATK " + MatchLog.number(creature.attack()) + ")");
	}
}
