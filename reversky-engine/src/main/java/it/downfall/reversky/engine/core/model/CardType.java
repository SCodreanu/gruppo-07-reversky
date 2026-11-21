package it.downfall.reversky.engine.core.model;

/**
 * Tipo di una carta, uguale al campo {@code type} del contratto JSON (docs/api-engine) e all'enum del database.
 */
public enum CardType {

	/** Creatura: l'unica carta che entra in campo e combatte. */
	CREATURE,

	/** Spell: si risolve quando viene pescata, senza fazione. */
	SPELL,

	/** Hidden: viene messa coperta e si attiva più tardi, senza fazione. */
	HIDDEN,

	/** Terreno: dà il bonus alle creature della sua fazione, di entrambi i giocatori. */
	TERRAIN,

	/** Oggetto: resta accanto al mazzo e scatta una volta sola (D-47). */
	ITEM
}
