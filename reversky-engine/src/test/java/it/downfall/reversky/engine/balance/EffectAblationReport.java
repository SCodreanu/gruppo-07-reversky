package it.downfall.reversky.engine.balance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import it.downfall.reversky.engine.balance.EffectAblation.Measure;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;

/**
 * Report del valore degli effetti ({@code ENG-42}, fase 2): quanto vale ogni effetto in punti di V e se è troppo
 * forte o troppo debole (scelto il 21 novembre).
 * <p>
 * Taratura: +1 DEF su una creatura senza effetto vale 1 punto di V; la media dei guadagni per copia delle creature
 * senza effetto dà i punti di partita per V. Il valore di un effetto in V è il suo guadagno per copia diviso per
 * questa media. Una creatura si confronta con il prezzo del suo effetto ({@code ps_effetto}); Spell, Hidden, Terreni
 * e Oggetti con la media in V del loro tipo. Giudizio: troppo forte o troppo debole se lo scarto supera
 * {@value #GAP_V} punti di V e anche il {@value #GAP_SHARE} del riferimento.
 */
public final class EffectAblationReport {

	/** Scarto minimo in punti di V per un giudizio. */
	public static final double GAP_V = 2.0;

	/** Scarto minimo come frazione del riferimento per un giudizio. */
	public static final double GAP_SHARE = 0.5;

	/** Intestazione del CSV per carta. */
	public static final String CSV_HEADER = "id,nome,tipo,fazione,rarita,partite,punti_per_copia,errore_punti,"
			+ "valore_v,errore_v,ps_effetto,riferimento_v,scarto_v,giudizio";

	/** Nome del file Markdown del report. */
	public static final String REPORT_FILE = "report-effetti.md";

	/** Nome del file CSV per carta. */
	public static final String CSV_FILE = "report-effetti-carte.csv";

	private static final List<CardType> TYPE_ORDER = List.of(CardType.CREATURE, CardType.SPELL, CardType.HIDDEN,
			CardType.TERRAIN, CardType.ITEM);

	private final BalanceConfig config;
	private final int matchesPerPairing;
	private final long baseSeed;
	private final int baselineMatches;
	private final List<Calibration> calibration = new ArrayList<>();
	private final List<Effect> effects = new ArrayList<>();

	/**
	 * Crea il report vuoto.
	 *
	 * @param config            configurazione delle partite
	 * @param matchesPerPairing partite per accoppiamento
	 * @param baseSeed          seed della prima partita
	 * @param baselineMatches   partite di base giocate
	 */
	public EffectAblationReport(BalanceConfig config, int matchesPerPairing, long baseSeed, int baselineMatches) {
		this.config = config;
		this.matchesPerPairing = matchesPerPairing;
		this.baseSeed = baseSeed;
		this.baselineMatches = baselineMatches;
	}

	/**
	 * Aggiunge il guadagno con +1 DEF di una creatura senza effetto.
	 *
	 * @param card    creatura senza effetto
	 * @param measure guadagno per copia della variante con +1 DEF
	 */
	public void addCalibration(CardDefinition card, Measure measure) {
		calibration.add(new Calibration(card, measure));
	}

	/**
	 * Aggiunge il valore dell'effetto di una carta.
	 *
	 * @param card    carta con effetto
	 * @param price   prezzo dell'effetto in V ({@code ps_effetto})
	 * @param measure valore per copia dell'effetto: guadagno della carta senza effetto, cambiato di segno
	 */
	public void addEffect(CardDefinition card, double price, Measure measure) {
		effects.add(new Effect(card, price, measure));
	}

	/**
	 * Punti di partita per punto di V: media dei guadagni con +1 DEF.
	 *
	 * @return frazione di partita per copia e per punto di V
	 * @throws IllegalStateException senza taratura
	 */
	public double pointsPerV() {
		if (calibration.isEmpty()) {
			throw new IllegalStateException("Manca la taratura: nessuna creatura senza effetto misurata");
		}
		return calibration.stream().mapToDouble(c -> c.measure().perCopy()).average().orElseThrow();
	}

	/**
	 * Righe del report, per tipo e dallo scarto più alto al più basso.
	 *
	 * @return lista immutabile
	 * @throws IllegalStateException senza taratura
	 */
	public List<Row> rows() {
		double pointsPerV = pointsPerV();
		Map<CardType, Double> typeAverage = effects.stream()
				.collect(Collectors.groupingBy(e -> e.card().type(),
						Collectors.averagingDouble(e -> e.measure().perCopy() / pointsPerV)));
		return effects.stream()
				.map(e -> row(e, pointsPerV, e.card().type() == CardType.CREATURE ? e.price()
						: typeAverage.get(e.card().type())))
				.sorted(Comparator.comparingInt((Row r) -> TYPE_ORDER.indexOf(r.card().type()))
						.thenComparing(Comparator.comparingDouble(Row::gapV).reversed()))
				.toList();
	}

	/**
	 * Il report in Markdown, in italiano.
	 *
	 * @return testo del file {@value #REPORT_FILE}
	 */
	public String markdown() {
		List<Row> rows = rows();
		StringBuilder md = new StringBuilder();
		md.append("# Report del valore degli effetti\n\n");
		md.append("Generato da `EffectAblationSimulationTest` con `gradlew effectTest` (`ENG-42`, fase 2): non si "
				+ "modifica a mano, si rigenera. Gli stessi seed danno lo stesso report.\n\n");
		md.append("## Come è fatto\n\n");
		md.append("- Configurazione: ").append(config.label()).append(", ").append(number(baselineMatches))
				.append(" partite di base (16 accoppiamenti × ").append(number(matchesPerPairing))
				.append("), seed ").append(baseSeed).append(" + accoppiamento × ").append(number(matchesPerPairing))
				.append(" + partita; mazzi casuali come in `report-bilanciamento.md`.\n");
		md.append("- Partite appaiate: per ogni carta si rigiocano, con gli stessi seed e mazzi, le partite in cui i "
				+ "due mazzi ne hanno un numero diverso di copie, con la carta senza effetto. Il valore per copia è "
				+ "la pendenza del guadagno sulla differenza di copie, cambiata di segno.\n");
		md.append("- Senza effetto: una creatura perde il codice; Spell, Hidden, Terreni e Oggetti hanno un effetto "
				+ "che non fa niente, quindi il valore è quello di tutta la carta (un Terreno perde anche il bonus "
				+ "base).\n");
		md.append("- Giudizio: le creature contro il prezzo del loro effetto (`ps_effetto`), le altre carte contro "
				+ "la media in V del loro tipo; troppo forte o troppo debole se lo scarto supera ")
				.append(decimal(GAP_V)).append(" punti di V e il ").append(percent(GAP_SHARE))
				.append(" del riferimento.\n");
		md.append("- Dati di ogni carta: `").append(CSV_FILE).append("`.\n\n");
		appendCalibration(md);
		for (CardType type : TYPE_ORDER) {
			List<Row> ofType = rows.stream().filter(r -> r.card().type() == type).toList();
			if (!ofType.isEmpty()) {
				appendTable(md, type, ofType);
			}
		}
		appendWarnings(md, rows);
		return md.toString();
	}

	/**
	 * Il CSV per carta, numeri con il punto.
	 *
	 * @return testo del file {@value #CSV_FILE}
	 */
	public String csv() {
		StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
		for (Row row : rows()) {
			CardDefinition card = row.card();
			csv.append(card.cardId()).append(',').append(quoted(card.name())).append(',').append(card.type())
					.append(',').append(faction(card)).append(',').append(card.rarity()).append(',')
					.append(row.measure().matches()).append(',').append(fixed(row.measure().perCopy(), 4))
					.append(',').append(fixed(row.measure().standardError(), 4)).append(',')
					.append(fixed(row.valueV(), 2)).append(',').append(fixed(row.errorV(), 2)).append(',')
					.append(fixed(row.price(), 2)).append(',').append(fixed(row.reference(), 2)).append(',')
					.append(fixed(row.gapV(), 2)).append(',').append(row.verdict().label()).append('\n');
		}
		return csv.toString();
	}

	private void appendCalibration(StringBuilder md) {
		double pointsPerV = pointsPerV();
		double spread = calibration.stream().mapToDouble(c -> c.measure().perCopy()).max().orElseThrow()
				- calibration.stream().mapToDouble(c -> c.measure().perCopy()).min().orElseThrow();
		md.append("## Taratura\n\n");
		md.append("+1 DEF su ").append(calibration.size()).append(" creature senza effetto: 1 punto di V vale ")
				.append(points(pointsPerV)).append(" per copia (da una creatura all'altra varia di ")
				.append(points(spread)).append(").\n\n");
	}

	private static void appendTable(StringBuilder md, CardType type, List<Row> rows) {
		md.append("## ").append(typeName(type)).append("\n\n");
		md.append("| Carta | Fazione | Rarità | Partite | Valore per copia | Valore in V | PS | Riferimento | "
				+ "Scarto | Giudizio |\n");
		md.append("|---|---|---|---|---|---|---|---|---|---|\n");
		for (Row row : rows) {
			CardDefinition card = row.card();
			md.append("| ").append(card.name()).append(" | ").append(faction(card)).append(" | ")
					.append(card.rarity()).append(" | ").append(number(row.measure().matches())).append(" | ")
					.append(points(row.measure().perCopy())).append(" | ").append(decimal(row.valueV()))
					.append(" ± ").append(decimal(row.errorV())).append(" | ").append(decimal(row.price()))
					.append(" | ").append(decimal(row.reference())).append(" | ").append(signed(row.gapV()))
					.append(" | ").append(row.verdict().label()).append(" |\n");
		}
		md.append('\n');
	}

	private static void appendWarnings(StringBuilder md, List<Row> rows) {
		md.append("## Avvisi\n\n");
		List<Row> flagged = rows.stream().filter(r -> r.verdict() != Verdict.OK).toList();
		if (flagged.isEmpty()) {
			md.append("Nessuno.\n");
			return;
		}
		for (Row row : flagged) {
			md.append("- ").append(row.card().name()).append(" (").append(typeName(row.card().type()))
					.append(", ").append(row.card().rarity()).append("): ").append(row.verdict().label())
					.append(", ").append(decimal(row.valueV())).append(" V contro ").append(decimal(row.reference()))
					.append(" (").append(signed(row.gapV())).append(").\n");
		}
	}

	private static Row row(Effect effect, double pointsPerV, double reference) {
		Measure measure = effect.measure();
		double valueV = measure.perCopy() / pointsPerV;
		double gap = valueV - reference;
		boolean flagged = Math.abs(gap) > GAP_V && Math.abs(gap) > GAP_SHARE * Math.abs(reference);
		Verdict verdict = !flagged ? Verdict.OK : gap > 0 ? Verdict.STRONG : Verdict.WEAK;
		return new Row(effect.card(), effect.price(), measure, valueV, measure.standardError() / pointsPerV,
				reference, gap, verdict);
	}

	private static String typeName(CardType type) {
		return switch (type) {
			case CREATURE -> "Creature";
			case SPELL -> "Spell";
			case HIDDEN -> "Hidden";
			case TERRAIN -> "Terreni";
			case ITEM -> "Oggetti";
		};
	}

	private static String faction(CardDefinition card) {
		if (card.factionId() == null) {
			return "";
		}
		String name = BalanceConfig.factionName(card.factionId());
		Integer second = card.secondaryFactionId();
		return second == null ? name : name + "/" + BalanceConfig.factionName(second);
	}

	private static String quoted(String value) {
		return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
	}

	private static String points(double perCopy) {
		return String.format(Locale.ITALY, "%.2f punti", perCopy * 100);
	}

	private static String decimal(double value) {
		return String.format(Locale.ITALY, "%.1f", value);
	}

	private static String signed(double value) {
		return String.format(Locale.ITALY, "%+.1f", value);
	}

	private static String percent(double share) {
		return String.format(Locale.ITALY, "%.0f%%", share * 100);
	}

	private static String number(int value) {
		return String.format(Locale.ITALY, "%,d", value);
	}

	private static String fixed(double value, int decimals) {
		return String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	/** Giudizio su un effetto. */
	public enum Verdict {

		/** Dentro la soglia. */
		OK("ok"),

		/** Vale più del riferimento oltre la soglia. */
		STRONG("troppo forte"),

		/** Vale meno del riferimento oltre la soglia. */
		WEAK("troppo debole");

		private final String label;

		Verdict(String label) {
			this.label = label;
		}

		/**
		 * Nome nel report.
		 *
		 * @return in italiano
		 */
		public String label() {
			return label;
		}
	}

	/**
	 * Una riga del report.
	 *
	 * @param card      carta
	 * @param price     prezzo dell'effetto in V ({@code ps_effetto})
	 * @param measure   valore per copia dell'effetto
	 * @param valueV    valore in punti di V
	 * @param errorV    errore standard in punti di V
	 * @param reference riferimento in V: il prezzo per le creature, la media del tipo per le altre carte
	 * @param gapV      valore meno riferimento
	 * @param verdict   giudizio
	 */
	public record Row(CardDefinition card, double price, Measure measure, double valueV, double errorV,
			double reference, double gapV, Verdict verdict) {
	}

	private record Calibration(CardDefinition card, Measure measure) {
	}

	private record Effect(CardDefinition card, double price, Measure measure) {
	}
}
