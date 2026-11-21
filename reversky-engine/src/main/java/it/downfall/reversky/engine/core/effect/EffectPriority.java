package it.downfall.reversky.engine.core.effect;

/**
 * Priorità di un effetto di creatura quando più effetti agiscono nello stesso momento (D-30, docs/effetti "Più effetti
 * nello stesso momento"): prima per priorità, nell'ordine di questo enum, poi la creatura dello sfidante.
 */
public enum EffectPriority {

	/** Effetti che annullano altri effetti (Il Predicatore sotto la Pioggia, SCP-2521, Baku): agiscono per primi. */
	NULLIFY,

	/** Tutti gli altri effetti. */
	NORMAL
}
