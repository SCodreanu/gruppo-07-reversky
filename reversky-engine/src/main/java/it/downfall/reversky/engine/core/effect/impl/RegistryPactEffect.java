package it.downfall.reversky.engine.core.effect.impl;

/**
 * Patto del Registro (Registry Pact), Spell {@code SHIELD}: "+2 DEF, +1 ATK".
 */
public final class RegistryPactEffect extends ShieldSpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "REGISTRY_PACT";

	/** Crea l'effetto. */
	public RegistryPactEffect() {
		super(1, 2);
	}

	@Override
	public String code() {
		return CODE;
	}
}
