package it.downfall.reversky.engine.balance;

import java.util.List;

import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Una configurazione di regole del bilanciamento: punti vita, limite di round e bonus di fazione.
 * <p>
 * La griglia di {@code ENG-28} (deciso il 20 novembre) incrocia i punti vita di D-01 (30 e 25) con i due bonus di
 * D-04 (+2/−1 ATK, e il bonus di prova del 13 novembre: +1 DEF contro la fazione che si batte, nessun bonus ATK),
 * sempre con il limite di 75 round di D-02.
 *
 * @param id             identificativo breve, usato nel CSV
 * @param label          descrizione per il report
 * @param rules          punti vita e limite di round
 * @param factionBonuses righe di bonus di fazione
 */
public record BalanceConfig(String id, String label, RuleConfig rules, List<FactionBonus> factionBonuses) {

	/** Fazioni nell'ordine del report: Wound, Invitation, Archive, Dream (id di {@link TestFactions}). */
	public static final List<Integer> FACTIONS = List.of(TestFactions.WOUND, TestFactions.INVITATION,
			TestFactions.ARCHIVE, TestFactions.DREAM);

	/** Limite di round totali (D-02). */
	public static final int MAX_ROUNDS = 75;

	/**
	 * Copia la lista dei bonus.
	 */
	public BalanceConfig {
		factionBonuses = List.copyOf(factionBonuses);
	}

	/**
	 * Le quattro configurazioni della griglia, nell'ordine del report.
	 *
	 * @return 30 PV con D-04, 30 PV con il bonus di prova, 25 PV con D-04, 25 PV con il bonus di prova
	 */
	public static List<BalanceConfig> grid() {
		return List.of(
				of("pv30-d04", "30 PV, bonus D-04 (+2/−1 ATK)", 30, TestFactions.standardBonuses()),
				of("pv30-prova", "30 PV, bonus di prova (+1 DEF)", 30, trialBonuses()),
				of("pv25-d04", "25 PV, bonus D-04 (+2/−1 ATK)", 25, TestFactions.standardBonuses()),
				of("pv25-prova", "25 PV, bonus di prova (+1 DEF)", 25, trialBonuses()));
	}

	/**
	 * Fazione battuta da una fazione (docs/fazioni): Wound batte Dream, Invitation batte Archive, Archive batte Wound,
	 * Dream batte Invitation.
	 *
	 * @param factionId id della fazione
	 * @return id della fazione che batte
	 */
	public static int beats(int factionId) {
		return switch (factionId) {
			case TestFactions.WOUND -> TestFactions.DREAM;
			case TestFactions.INVITATION -> TestFactions.ARCHIVE;
			case TestFactions.ARCHIVE -> TestFactions.WOUND;
			case TestFactions.DREAM -> TestFactions.INVITATION;
			default -> throw new IllegalArgumentException("Fazione sconosciuta: " + factionId);
		};
	}

	/**
	 * Nome breve della fazione, come nel catalogo.
	 *
	 * @param factionId id della fazione
	 * @return Wound, Invitation, Archive o Dream
	 */
	public static String factionName(int factionId) {
		return switch (factionId) {
			case TestFactions.WOUND -> "Wound";
			case TestFactions.INVITATION -> "Invitation";
			case TestFactions.ARCHIVE -> "Archive";
			case TestFactions.DREAM -> "Dream";
			default -> throw new IllegalArgumentException("Fazione sconosciuta: " + factionId);
		};
	}

	/** Bonus di prova del 13 novembre (D-04): +1 DEF contro la fazione che si batte, nessun bonus ATK. */
	private static List<FactionBonus> trialBonuses() {
		return FACTIONS.stream().map(faction -> new FactionBonus(faction, beats(faction), 0, 1)).toList();
	}

	private static BalanceConfig of(String id, String label, int lifePoints, List<FactionBonus> bonuses) {
		return new BalanceConfig(id, label, new RuleConfig("bilanciamento-" + id, lifePoints, MAX_ROUNDS), bonuses);
	}
}
