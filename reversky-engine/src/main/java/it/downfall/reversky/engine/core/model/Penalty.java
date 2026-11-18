package it.downfall.reversky.engine.core.model;

/**
 * Penalità ricevuta da una creatura (docs/effetti/catalogo-effetti, "penalità"; ENG-37, deciso il 18 novembre): DEF
 * persa per un effetto, una Spell, una Hidden, un danno in attesa o un Terreno dell'avversario, oppure un malus ATK
 * di una carta avversaria. Gli attacchi, i sacrifici, i sigilli e i congelamenti non sono penalità. Resta in coda
 * nella creatura finché il motore la controlla, dopo la fase (Remnants of Reality).
 *
 * @param defense     DEF persa davvero, o 0 per un malus ATK
 * @param attackMalus il malus ATK, o {@code null} per una penalità DEF
 */
public record Penalty(int defense, StatModifier attackMalus) {

	/**
	 * Controlla i valori.
	 *
	 * @throws IllegalArgumentException se non è esattamente una delle due penalità
	 */
	public Penalty {
		if ((defense > 0) == (attackMalus != null) || defense < 0) {
			throw new IllegalArgumentException("Una penalità è DEF persa oppure un malus ATK");
		}
	}

	/**
	 * Indica se è un malus ATK.
	 *
	 * @return {@code true} per un malus ATK, {@code false} per DEF persa
	 */
	public boolean isAttack() {
		return attackMalus != null;
	}
}
