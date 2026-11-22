package it.downfall.reversky.engine.core.effect;

import java.util.Optional;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Uscita del Terreno in gioco (docs/carte, "Terreno"; D-19): quando un Terreno è sostituito, rimosso (The Null
 * Witness) o inghiottito (The Hollow Wyrm), le creature perdono i suoi bonus, l'ATK subito e dello scudo solo la parte
 * non consumata, quindi l'uscita non uccide mai. Lo usano il {@code TerrainService} e gli effetti che tolgono il
 * Terreno (ENG-36).
 */
public final class Terrains {

	private Terrains() {
	}

	/**
	 * Toglie il Terreno in gioco per un effetto (ENG-36): evento {@code TERRAIN_REMOVED}, poi ogni creatura in campo
	 * perde il suo bonus, prima lo sfidante. Il Terreno va negli scarti.
	 *
	 * @param state     partita
	 * @param causeSide lato della carta che lo toglie
	 * @param cause     nome della carta che lo toglie
	 * @return il Terreno tolto, o vuoto se non ce n'era uno
	 */
	public static Optional<PlacedTerrain> remove(MatchState state, Side causeSide, String cause) {
		Optional<PlacedTerrain> removed = state.removeTerrain();
		removed.ifPresent(old -> {
			state.log().terrainRemoved(causeSide, old.card(), cause);
			loseBonuses(state, old);
		});
		return removed;
	}

	/**
	 * Le creature in campo perdono il bonus di un Terreno uscito, prima lo sfidante; chi lo perde lo dice nel log.
	 *
	 * @param state partita
	 * @param old   Terreno uscito
	 */
	public static void loseBonuses(MatchState state, PlacedTerrain old) {
		for (Side side : Side.values()) {
			state.player(side).creature().ifPresent(creature -> loseBonus(state, creature, old));
		}
	}

	private static void loseBonus(MatchState state, CreatureInPlay creature, PlacedTerrain old) {
		if (creature.loseTerrainBonus()) {
			state.log().effectTriggered(creature.side(), creature.name(), "loses the " + old.card().name()
					+ " bonus (ATK " + MatchLog.number(creature.attack()) + ", DEF "
					+ MatchLog.number(creature.defense()) + ")");
		}
	}
}
