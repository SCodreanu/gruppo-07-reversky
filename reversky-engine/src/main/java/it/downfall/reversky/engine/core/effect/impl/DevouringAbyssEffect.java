package it.downfall.reversky.engine.core.effect.impl;

/**
 * Devouring Abyss (Crepa Cavità), Terreno della Theory of the Dream: bonus base +1 ATK. Effetto secondario (ENG-27):
 * all'inizio di ogni scontro 1 danno alla creatura avversaria.
 */
public final class DevouringAbyssEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "DEVOURING_ABYSS";

	/** Crea l'effetto. */
	public DevouringAbyssEffect() {
		super(1, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
