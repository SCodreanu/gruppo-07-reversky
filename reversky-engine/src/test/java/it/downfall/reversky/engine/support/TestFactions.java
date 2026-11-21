package it.downfall.reversky.engine.support;

import java.util.List;

import it.downfall.reversky.engine.core.model.Faction;
import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.FactionMatrix;

/**
 * Le quattro fazioni e i bonus di D-04 con gli id degli esempi (docs/fazioni). Nel core le fazioni sono dati:
 * queste costanti esistono solo nei test.
 */
public final class TestFactions {

	/** Theory of the Wound: batte Dream. */
	public static final int WOUND = 1;

	/** Theory of the Invitation: batte Archive. */
	public static final int INVITATION = 2;

	/** Theory of the Archive: batte Wound. */
	public static final int ARCHIVE = 3;

	/** Theory of the Dream: batte Invitation. */
	public static final int DREAM = 4;

	private TestFactions() {
	}

	/**
	 * Le quattro fazioni, nell'ordine degli id.
	 *
	 * @return lista immutabile delle fazioni
	 */
	public static List<Faction> all() {
		return List.of(
				new Faction(WOUND, "Theory of the Wound"),
				new Faction(INVITATION, "Theory of the Invitation"),
				new Faction(ARCHIVE, "Theory of the Archive"),
				new Faction(DREAM, "Theory of the Dream"));
	}

	/**
	 * Righe di bonus di D-04: +2 ATK contro la fazione che si batte, −1 ATK contro quella che ti batte; le coppie
	 * neutre (Wound–Invitation, Archive–Dream) non hanno righe.
	 *
	 * @return lista immutabile delle 8 righe, nell'ordine della tabella di docs/fazioni
	 */
	public static List<FactionBonus> standardBonuses() {
		return List.of(
				new FactionBonus(WOUND, DREAM, 2, 0),
				new FactionBonus(INVITATION, ARCHIVE, 2, 0),
				new FactionBonus(ARCHIVE, WOUND, 2, 0),
				new FactionBonus(DREAM, INVITATION, 2, 0),
				new FactionBonus(DREAM, WOUND, -1, 0),
				new FactionBonus(ARCHIVE, INVITATION, -1, 0),
				new FactionBonus(WOUND, ARCHIVE, -1, 0),
				new FactionBonus(INVITATION, DREAM, -1, 0));
	}

	/**
	 * Matrice con i bonus di D-04.
	 *
	 * @return matrice costruita da {@link #standardBonuses()}
	 */
	public static FactionMatrix standardMatrix() {
		return new FactionMatrix(standardBonuses());
	}

	/**
	 * Matrice senza righe: ogni coppia vale 0. Per i test di regola in cui i bonus non devono entrare.
	 *
	 * @return matrice vuota
	 */
	public static FactionMatrix noBonuses() {
		return new FactionMatrix(List.of());
	}
}
