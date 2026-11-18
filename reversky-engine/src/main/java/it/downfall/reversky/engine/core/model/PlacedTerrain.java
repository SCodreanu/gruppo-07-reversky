package it.downfall.reversky.engine.core.model;

import java.util.Objects;

import it.downfall.reversky.engine.core.effect.TerrainEffect;

/**
 * Il Terreno in gioco, chi l'ha pescato e il suo effetto. In gioco c'è un solo Terreno alla volta, di chiunque sia
 * (D-19); il proprietario serve a The Null Witness, che lo rimuove solo se l'ha pescato l'avversario (D-38).
 * <p>
 * L'effetto è una nuova istanza per ogni messa in gioco (deciso il 18 novembre, ENG-27): tiene lo stato degli effetti
 * secondari (es. "la prima ferita di ogni scontro" di Echo Rift), che riparte quando un altro Terreno lo sostituisce.
 *
 * @param card   carta del Terreno
 * @param owner  lato di chi l'ha pescato
 * @param effect effetto del Terreno, creato dal registro per questa messa in gioco
 */
public record PlacedTerrain(CardDefinition card, Side owner, TerrainEffect effect) {

	/**
	 * Controlla i campi.
	 *
	 * @throws NullPointerException se manca un campo
	 */
	public PlacedTerrain {
		Objects.requireNonNull(card, "card");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(effect, "effect");
	}

	/**
	 * Indica se il Terreno riguarda una creatura: è della sua fazione, per una delle sue fazioni (D-17, D-22). Il
	 * bonus base e gli effetti secondari valgono per queste creature, di entrambi i giocatori.
	 *
	 * @param creature creatura in campo
	 * @return {@code true} se una delle fazioni della creatura è quella del Terreno
	 */
	public boolean affects(CreatureInPlay creature) {
		return creature.factions().contains(card.factionId());
	}
}
