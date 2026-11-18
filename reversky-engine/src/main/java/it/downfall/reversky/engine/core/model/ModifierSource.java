package it.downfall.reversky.engine.core.model;

/**
 * Origine di un {@link StatModifier} (D-35): serve agli effetti che distinguono, sospendono o tolgono un
 * modificatore secondo da dove arriva (es. un Terreno che esce toglie solo i suoi).
 * <p>
 * Il bonus di fazione non è un modificatore: l'ATK si ricalcola a ogni round dalla {@code FactionMatrix} e la DEF si
 * somma una volta all'inizio dello scontro, quindi nessun effetto lo può togliere o sospendere.
 */
public enum ModifierSource {

	/** Bonus del Terreno in gioco (D-17, D-19). */
	TERRAIN,

	/** Effetto di un Oggetto (D-47). */
	ITEM,

	/** Spell pescata (D-10, D-12). */
	SPELL,

	/** Hidden attivata (D-15). */
	HIDDEN,

	/** Abilità di una creatura. */
	EFFECT
}
