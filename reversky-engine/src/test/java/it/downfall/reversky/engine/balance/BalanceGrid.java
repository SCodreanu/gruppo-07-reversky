package it.downfall.reversky.engine.balance;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import it.downfall.reversky.engine.core.rules.FactionBonus;
import it.downfall.reversky.engine.core.rules.RuleConfig;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Le griglie di configurazioni che {@code gradlew balanceTest} può simulare, ognuna con il suo report.
 * <p>
 * Si sceglie con {@code -PbalanceGrid=<nome>} (proprietà di sistema {@code balance.grid}); senza, la griglia base.
 * Tutte le griglie usano gli stessi mazzi e gli stessi seed, quindi i loro report si confrontano riga per riga.
 */
public enum BalanceGrid {

	/** La griglia 2 × 2 di {@code ENG-28}: 30 e 25 PV × bonus D-04 e bonus di prova (deciso il 20 novembre). */
	BASE("base", "Report di bilanciamento", "report-bilanciamento.md", "report-carte.csv", "",
			BalanceConfig::grid),

	/**
	 * Le forme del bonus di fazione da provare per D-04 (scelte il 20 novembre): +1 ATK in ogni round a chi batte
	 * l'avversaria, +1 DEF una volta per scontro a chi è battuto, contro chi lo batte; poi la stessa forma con +2 DEF
	 * a Wound contro Archive e a Invitation contro Dream; ognuna con 30 e 25 PV (D-01).
	 */
	FORME("forme", "Report delle forme del bonus di fazione", "report-forme-bonus.md", "report-forme-bonus-carte.csv",
			"Forme del bonus di fazione da provare per D-04, con gli stessi mazzi e seed di "
					+ "`report-bilanciamento.md`: cambia solo il bonus. +1 ATK contro +1 DEF: chi batte l'avversaria "
					+ "ha +1 ATK in ogni round dello scontro, chi è battuto ha +1 DEF una volta per scontro contro "
					+ "chi lo batte. La variante per coppia dà +2 DEF invece di +1 a Wound contro Archive e a "
					+ "Invitation contro Dream.",
			BalanceGrid::shapes),

	/**
	 * La griglia di {@code ENG-42}: a 30 PV (D-01) il bonus D-04, quello di prova e +1 ATK contro +1 DEF, per
	 * ribilanciare ATK e DEF delle creature mentre si sceglie il bonus (deciso il 20 novembre).
	 */
	PV30("pv30", "Report di bilanciamento a 30 PV", "report-30pv.md", "report-30pv-carte.csv",
			"Griglia di lavoro di ENG-42, con gli stessi mazzi e seed di `report-bilanciamento.md`: 30 PV con il "
					+ "bonus D-04 (+2/-1 ATK), quello di prova (+1 DEF) e +1 ATK contro +1 DEF, per ribilanciare "
					+ "ATK e DEF delle creature mentre si sceglie il bonus di fazione.",
			BalanceGrid::thirtyLifePoints);

	private final String gridName;
	private final String title;
	private final String reportFile;
	private final String csvFile;
	private final String note;
	private final Supplier<List<BalanceConfig>> configs;

	BalanceGrid(String gridName, String title, String reportFile, String csvFile, String note,
			Supplier<List<BalanceConfig>> configs) {
		this.gridName = gridName;
		this.title = title;
		this.reportFile = reportFile;
		this.csvFile = csvFile;
		this.note = note;
		this.configs = configs;
	}

	/**
	 * Griglia con questo nome.
	 *
	 * @param name {@code base}, {@code forme} o {@code pv30}
	 * @return la griglia
	 * @throws IllegalArgumentException se il nome non è di una griglia
	 */
	public static BalanceGrid byName(String name) {
		for (BalanceGrid grid : values()) {
			if (grid.gridName.equals(name)) {
				return grid;
			}
		}
		throw new IllegalArgumentException("Griglia di bilanciamento sconosciuta: " + name);
	}

	/**
	 * Configurazioni della griglia, nell'ordine del report.
	 *
	 * @return lista immutabile
	 */
	public List<BalanceConfig> configs() {
		return configs.get();
	}

	/**
	 * Titolo del report.
	 *
	 * @return titolo senza il {@code #}
	 */
	public String title() {
		return title;
	}

	/**
	 * Nome del file Markdown del report.
	 *
	 * @return nome nella cartella del report
	 */
	public String reportFile() {
		return reportFile;
	}

	/**
	 * Nome del file CSV per carta.
	 *
	 * @return nome nella cartella del report
	 */
	public String csvFile() {
		return csvFile;
	}

	/**
	 * Nota che il report mette in testa a "Come è fatto".
	 *
	 * @return frase in italiano; vuota per la griglia base
	 */
	public String note() {
		return note;
	}

	private static List<BalanceConfig> shapes() {
		List<FactionBonus> atkOneDefOne = atkAgainstDef(Set.of());
		// Archive contro Wound e Dream contro Invitation restavano al 68-76% con +1 DEF (scelto il 20 novembre)
		List<FactionBonus> perPair = atkAgainstDef(Set.of(TestFactions.ARCHIVE, TestFactions.DREAM));
		String perPairLabel = "+1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream";
		return List.of(shape("pv30-atk1-def1", "30 PV, +1 ATK contro +1 DEF", 30, atkOneDefOne),
				shape("pv25-atk1-def1", "25 PV, +1 ATK contro +1 DEF", 25, atkOneDefOne),
				shape("pv30-atk1-def12", "30 PV, " + perPairLabel, 30, perPair),
				shape("pv25-atk1-def12", "25 PV, " + perPairLabel, 25, perPair));
	}

	private static List<BalanceConfig> thirtyLifePoints() {
		List<BalanceConfig> base = BalanceConfig.grid();
		return List.of(base.get(0), base.get(1), shapes().getFirst());
	}

	/**
	 * +1 ATK a ogni fazione contro quella che batte; chi è battuto ha +1 DEF contro chi lo batte, +2 se chi lo batte
	 * è tra {@code twoDefAgainst}.
	 */
	private static List<FactionBonus> atkAgainstDef(Set<Integer> twoDefAgainst) {
		return BalanceConfig.FACTIONS.stream()
				.flatMap(faction -> List.of(new FactionBonus(faction, BalanceConfig.beats(faction), 1, 0),
						new FactionBonus(BalanceConfig.beats(faction), faction, 0,
								twoDefAgainst.contains(faction) ? 2 : 1))
						.stream())
				.toList();
	}

	private static BalanceConfig shape(String id, String label, int lifePoints, List<FactionBonus> bonuses) {
		return new BalanceConfig(id, label,
				new RuleConfig("bilanciamento-" + id, lifePoints, BalanceConfig.MAX_ROUNDS), bonuses);
	}
}
