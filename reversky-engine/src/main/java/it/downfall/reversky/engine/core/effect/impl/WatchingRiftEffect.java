package it.downfall.reversky.engine.core.effect.impl;

/**
 * Watching Rift (Crepa Occhi), Terreno della Theory of the Archive: bonus base +1 ATK e +1 DEF. Effetto secondario
 * (ENG-27): la prima Spell DAMAGE subita in uno scontro fa 1 punto in meno.
 */
public final class WatchingRiftEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "WATCHING_RIFT";

	/** Crea l'effetto. */
	public WatchingRiftEffect() {
		super(1, 1);
	}

	@Override
	public String code() {
		return CODE;
	}
}
