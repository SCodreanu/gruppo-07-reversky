package it.downfall.reversky.engine.core.effect.impl;

/**
 * Echo Rift (Crepa Eco), Terreno della Theory of the Wound: bonus base +1 ATK. Effetto secondario (ENG-27): la prima
 * volta che ogni creatura viene ferita in uno scontro, +1 ATK per il resto di quello scontro.
 */
public final class EchoRiftEffect extends BaseTerrainEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "ECHO_RIFT";

	/** Crea l'effetto. */
	public EchoRiftEffect() {
		super(1, 0);
	}

	@Override
	public String code() {
		return CODE;
	}
}
