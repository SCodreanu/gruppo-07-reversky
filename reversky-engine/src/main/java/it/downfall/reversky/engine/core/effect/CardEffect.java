package it.downfall.reversky.engine.core.effect;

/**
 * Effetto di una carta che non è una creatura: Spell, Hidden, Terreno od Oggetto (docs/effetti).
 * <p>
 * C'è un'interfaccia per ogni tipo di carta, con solo i momenti che servono a quel tipo: l'interfaccia chiusa
 * ({@code sealed}) fa sì che il registro sappia sempre a quale tipo di carta appartiene un codice. Le abilità delle
 * creature ({@code CreatureEffect}) arrivano con ENG-23.
 */
public sealed interface CardEffect permits SpellEffect, HiddenEffect, TerrainEffect, ItemEffect {

	/**
	 * Codice univoco, uguale a {@code effectCode} della carta ({@code catalog_cards.effect_code}).
	 *
	 * @return il codice in {@code UPPER_SNAKE_CASE}
	 */
	String code();
}
