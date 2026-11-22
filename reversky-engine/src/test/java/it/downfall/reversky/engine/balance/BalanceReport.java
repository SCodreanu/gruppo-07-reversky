package it.downfall.reversky.engine.balance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import it.downfall.reversky.engine.balance.BalanceStats.Score;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.EndReason;

/**
 * Report di bilanciamento: un Markdown di sintesi per i DSG e un CSV con una riga per carta e configurazione.
 * <p>
 * Avvisi decisi il 20 novembre: scontro di vantaggio (una fazione contro quella che batte) fuori da 55–65%, scontro
 * neutro fuori da 45–55%, fazione contro le altre fuori da 45–55%, partite a {@code ROUND_LIMIT} sopra il 5%, primo
 * giocatore fuori da 45–55%; carta giocata a più di 10 punti dalla media del suo gruppo (rarità per le creature, tipo
 * per le altre carte, media pesata sulle partite giocate), solo con almeno 500 partite giocata, altrimenti "campione
 * basso"; scala delle rarità delle creature non crescente, sul punteggio nel mazzo. Le bande comprendono gli estremi.
 * <p>
 * Il punteggio da giocata è spostato verso il basso, perché chi perde gioca più carte: va bene per confrontare carte
 * dello stesso gruppo, non per confrontare i gruppi. Per questo la scala delle rarità usa il punteggio nel mazzo
 * (deciso il 20 novembre).
 */
public final class BalanceReport {

	/** Partite giocata sotto le quali una carta è "campione basso" e non si segnala. */
	public static final int MIN_PLAYS = 500;

	private static final double ADVANTAGE_MIN = 0.55;
	private static final double ADVANTAGE_MAX = 0.65;
	private static final double EVEN_MIN = 0.45;
	private static final double EVEN_MAX = 0.55;
	private static final double ROUND_LIMIT_MAX = 0.05;
	private static final double CARD_BAND = 0.10;
	/** Tolleranza per i confronti con gli estremi delle bande, che sono inclusi. */
	private static final double EPSILON = 1e-9;
	private static final List<String> CREATURE_RARITIES = List.of("Common", "Uncommon", "Rare", "Mythic",
			"Legendary");
	private static final List<String> GROUPS = List.of("Common", "Uncommon", "Rare", "Mythic", "Legendary", "Mutant",
			"Spell", "Hidden", "Terreno", "Oggetto");
	private static final String CSV_HEADER = "configurazione,id,nome,tipo,fazione,rarita,gruppo,partite_nel_mazzo,"
			+ "punteggio_nel_mazzo_pct,partite_giocata,punteggio_giocata_pct,media_gruppo_pct,scarto_pp,"
			+ "attivazioni_per_partita_giocata,controllo";

	private final List<CardDefinition> catalog;
	private final int matchesPerPairing;
	private final long baseSeed;
	private final BalanceGrid grid;
	private final Map<BalanceConfig, BalanceStats> results = new LinkedHashMap<>();

	/** Esito del controllo di una carta. */
	public enum CardCheck {
		/** Nella banda del suo gruppo. */
		OK,
		/** Più di 10 punti sopra la media del gruppo. */
		ABOVE,
		/** Più di 10 punti sotto la media del gruppo. */
		BELOW,
		/** Meno di {@link #MIN_PLAYS} partite giocata: il punteggio non è affidabile. */
		LOW_SAMPLE
	}

	/** Su quali partite si calcola il punteggio di una carta o di un gruppo. */
	enum Basis {
		/** Partite in cui la carta era nel mazzo del lato. */
		IN_DECK,
		/** Partite in cui il lato ha giocato la carta. */
		PLAYED
	}

	/**
	 * Crea un report vuoto.
	 *
	 * @param catalog           carte del catalogo, nell'ordine del CSV
	 * @param matchesPerPairing partite per accoppiamento, per la descrizione
	 * @param baseSeed          seed base, per la descrizione
	 */
	public BalanceReport(List<CardDefinition> catalog, int matchesPerPairing, long baseSeed) {
		this(catalog, matchesPerPairing, baseSeed, BalanceGrid.BASE);
	}

	/**
	 * Crea un report vuoto per una griglia: titolo, nota e nome del CSV vengono dalla griglia.
	 *
	 * @param catalog           carte del catalogo, nell'ordine del CSV
	 * @param matchesPerPairing partite per accoppiamento, per la descrizione
	 * @param baseSeed          seed base, per la descrizione
	 * @param grid              griglia simulata
	 */
	public BalanceReport(List<CardDefinition> catalog, int matchesPerPairing, long baseSeed, BalanceGrid grid) {
		this.catalog = List.copyOf(catalog);
		this.matchesPerPairing = matchesPerPairing;
		this.baseSeed = baseSeed;
		this.grid = grid;
	}

	/**
	 * Aggiunge i risultati di una configurazione; l'ordine di aggiunta è l'ordine del report.
	 *
	 * @param config configurazione
	 * @param stats  statistiche delle sue partite
	 */
	public void add(BalanceConfig config, BalanceStats stats) {
		results.put(config, stats);
	}

	/**
	 * Avvisi su fazioni, scontri diretti, {@code ROUND_LIMIT} e primo giocatore.
	 *
	 * @param stats statistiche di una configurazione
	 * @return avvisi in italiano, nell'ordine del report; vuota se è tutto nelle bande
	 */
	public static List<String> warnings(BalanceStats stats) {
		List<String> warnings = new ArrayList<>(matchupWarnings(stats));
		for (int faction : stats.factionIds()) {
			Score score = stats.faction(faction);
			if (score.matches() > 0 && outside(score.rate(), EVEN_MIN, EVEN_MAX)) {
				warnings.add("Fazione " + BalanceConfig.factionName(faction) + ": " + pct(score.rate())
						+ " contro le altre (atteso 45-55%)");
			}
		}
		double roundLimit = share(stats.endReasonCount(EndReason.ROUND_LIMIT), stats.matches());
		if (roundLimit > ROUND_LIMIT_MAX + EPSILON) {
			warnings.add("Partite a ROUND_LIMIT: " + pct(roundLimit) + " (massimo 5%)");
		}
		double first = stats.firstPlayer().rate();
		if (stats.matches() > 0 && outside(first, EVEN_MIN, EVEN_MAX)) {
			warnings.add("Vantaggio del primo giocatore: lo sfidante fa " + pct(first) + " (atteso 45-55%)");
		}
		return warnings;
	}

	/**
	 * Controlla il punteggio da giocata di una carta contro la media del suo gruppo.
	 *
	 * @param played    punteggio delle partite in cui la carta è stata giocata
	 * @param groupRate media del gruppo, da 0 a 1
	 * @return l'esito del controllo
	 */
	public static CardCheck cardCheck(Score played, double groupRate) {
		if (played.matches() < MIN_PLAYS) {
			return CardCheck.LOW_SAMPLE;
		}
		double gap = played.rate() - groupRate;
		if (gap > CARD_BAND + EPSILON) {
			return CardCheck.ABOVE;
		}
		if (gap < -CARD_BAND - EPSILON) {
			return CardCheck.BELOW;
		}
		return CardCheck.OK;
	}

	/**
	 * Controlla che la media delle creature cresca con la rarità: Common, Uncommon, Rare, Mythic, Legendary.
	 *
	 * @param averages media per rarità; le rarità che mancano si saltano
	 * @return un avviso per ogni rarità che non supera la precedente
	 */
	public static List<String> rarityScaleWarnings(Map<String, Double> averages) {
		List<String> warnings = new ArrayList<>();
		String previous = null;
		for (String rarity : CREATURE_RARITIES) {
			if (!averages.containsKey(rarity)) {
				continue;
			}
			if (previous != null && averages.get(rarity) <= averages.get(previous) + EPSILON) {
				warnings.add(rarity + " (" + pct(averages.get(rarity)) + ") non supera " + previous + " ("
						+ pct(averages.get(previous)) + ")");
			}
			previous = rarity;
		}
		return warnings;
	}

	/**
	 * Il report in Markdown.
	 *
	 * @return testo con fine riga {@code \n}
	 */
	public String markdown() {
		StringBuilder md = new StringBuilder();
		appendIntroduction(md);
		appendSummary(md);
		results.forEach((config, stats) -> appendConfig(md, config, stats));
		return md.toString();
	}

	/**
	 * Il CSV per carta: una riga per carta del catalogo e configurazione, con l'intestazione.
	 *
	 * @return testo con fine riga {@code \n}, separatore virgola, decimali con il punto
	 */
	public String csv() {
		StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
		results.forEach((config, stats) -> {
			Map<String, Double> groups = groupRates(stats, catalog, Basis.PLAYED);
			for (CardDefinition card : catalog) {
				Score deck = stats.inDeck(card.cardId());
				Score played = stats.played(card.cardId());
				double group = groups.getOrDefault(group(card), 0.0);
				csv.append(String.join(",", config.id(), String.valueOf(card.cardId()), csvText(card.name()),
						card.type().name(), factions(card), card.rarity(), group(card),
						String.valueOf(deck.matches()), decimal(deck.rate() * 100),
						String.valueOf(played.matches()), decimal(played.rate() * 100), decimal(group * 100),
						decimal((played.rate() - group) * 100),
						decimal(played.matches() == 0 ? 0 : (double) stats.triggers(card.cardId()) / played.matches()),
						cardCheck(played, group).name())).append('\n');
			}
		});
		return csv.toString();
	}

	private static List<String> matchupWarnings(BalanceStats stats) {
		List<String> warnings = new ArrayList<>();
		List<Integer> factions = stats.factionIds();
		for (int a : factions) {
			for (int b : factions) {
				Score score = stats.matchup(a, b);
				if (a == b || score.matches() == 0) {
					continue;
				}
				String name = BalanceConfig.factionName(a) + " contro " + BalanceConfig.factionName(b);
				if (BalanceConfig.beats(a) == b && outside(score.rate(), ADVANTAGE_MIN, ADVANTAGE_MAX)) {
					warnings.add("Scontro di vantaggio " + name + ": " + pct(score.rate()) + " (atteso 55-65%)");
				}
				boolean neutral = BalanceConfig.beats(a) != b && BalanceConfig.beats(b) != a;
				if (neutral && factions.indexOf(a) < factions.indexOf(b)
						&& outside(score.rate(), EVEN_MIN, EVEN_MAX)) {
					warnings.add("Scontro neutro " + name + ": " + pct(score.rate()) + " (atteso 45-55%)");
				}
			}
		}
		return warnings;
	}

	private void appendIntroduction(StringBuilder md) {
		int perConfig = matchesPerPairing * BalancePairing.all().size();
		md.append("# ").append(grid.title()).append("\n\n")
				.append("Generato da `BalanceSimulationTest` con `gradlew balanceTest` (`ENG-28`): non si modifica a ")
				.append("mano, si rigenera. Gli stessi seed danno lo stesso report.\n\n")
				.append("## Come è fatto\n\n");
		if (!grid.note().isEmpty()) {
			md.append("- ").append(grid.note()).append('\n');
		}
		md.append("- Catalogo: `catalogo-finale.csv`, ").append(catalog.size()).append(" carte, con il registro ")
				.append("di tutti gli effetti dell'engine.\n")
				.append("- Mazzi: di una sola fazione, nuovi a ogni partita. 20 creature della fazione (le Mutant ")
				.append("valgono per entrambe le loro fazioni), il Terreno e l'Oggetto della fazione, 6 Spell, ")
				.append("2 Hidden. Ogni carta si sceglie a caso tra quelle ancora ammesse dai limiti dell'API: copie ")
				.append("Common 3, Uncommon 2 (anche Spell e Hidden), Rare 2, Mythic, Legendary e Mutant 1; massimo ")
				.append("2 Legendary, 2 Mythic, 1 Mutant, 5 Rare contando Terreno e Oggetto; al massimo una Hidden ")
				.append("`DAMAGE`.\n")
				.append("- Partite: 16 accoppiamenti ordinati di fazioni (speculari compresi) × ")
				.append(number(matchesPerPairing)).append(" = ").append(number(perConfig))
				.append(" per configurazione. La partita i dell'accoppiamento p ha seed ").append(baseSeed)
				.append(" + p × ").append(number(matchesPerPairing)).append(" + i, uguale in tutte le ")
				.append("configurazioni: cambiano solo le regole.\n")
				.append("- Punteggio: vittoria 1, pareggio ½, sconfitta 0. Le fazioni e gli scontri diretti non ")
				.append("contano le partite speculari; il primo giocatore (lo sfidante, che pesca per primo), la ")
				.append("durata e le carte le contano tutte.\n")
				.append("- Carte: \"nel mazzo\" se è nel mazzo di quel lato, \"giocata\" se è entrata in gioco ")
				.append("(creatura in campo, Spell risolta, Hidden attivata, Terreno o Oggetto messo). Il gruppo è la ")
				.append("rarità per le creature e il tipo per le altre carte; la sua media è pesata sulle partite.\n")
				.append("- Il punteggio da giocata è sotto il 50% in quasi tutti i gruppi: chi perde gioca più carte, ")
				.append("perché perde più creature. Il confronto dentro un gruppo resta valido (la banda di 10 punti ")
				.append("usa il punteggio da giocata), il livello assoluto no: la scala delle rarità usa il punteggio ")
				.append("nel mazzo, che non ha questo spostamento (deciso il 20 novembre).\n")
				.append("- Avvisi: scontro di vantaggio fuori da 55-65%, scontro neutro e fazione fuori da 45-55%, ")
				.append("ROUND_LIMIT sopra il 5%, primo giocatore fuori da 45-55%, carta a più di 10 punti dalla ")
				.append("media del gruppo (con almeno ").append(MIN_PLAYS).append(" partite giocata), scala delle ")
				.append("rarità delle creature non crescente.\n")
				.append("- Dati di ogni carta: `").append(grid.csvFile()).append("`.\n\n");
	}

	private void appendSummary(StringBuilder md) {
		md.append("## Sintesi\n\n")
				.append("| Configurazione | Partite | Fazione migliore | Fazione peggiore | ROUND_LIMIT ")
				.append("| Durata media | Primo giocatore | Avvisi |\n")
				.append("|---|---|---|---|---|---|---|---|\n");
		results.forEach((config, stats) -> {
			List<Integer> byScore = stats.factionIds().stream()
					.sorted((a, b) -> Double.compare(stats.faction(b).rate(), stats.faction(a).rate()))
					.toList();
			int best = byScore.getFirst();
			int worst = byScore.getLast();
			md.append("| ").append(config.label()).append(" | ").append(number(stats.matches())).append(" | ")
					.append(BalanceConfig.factionName(best)).append(' ').append(pct(stats.faction(best).rate()))
					.append(" | ").append(BalanceConfig.factionName(worst)).append(' ')
					.append(pct(stats.faction(worst).rate())).append(" | ")
					.append(pct(share(stats.endReasonCount(EndReason.ROUND_LIMIT), stats.matches()))).append(" | ")
					.append(decimal1(stats.averageRounds())).append(" | ").append(pct(stats.firstPlayer().rate()))
					.append(" | ").append(allWarnings(stats).size()).append(" |\n");
		});
		md.append('\n');
	}

	private void appendConfig(StringBuilder md, BalanceConfig config, BalanceStats stats) {
		md.append("## ").append(config.label()).append("\n\n");
		appendFactions(md, stats);
		appendMatchups(md, stats);
		appendMatches(md, stats);
		appendGroups(md, stats);
		appendWarnings(md, stats);
		appendCardChecks(md, stats);
	}

	private static void appendFactions(StringBuilder md, BalanceStats stats) {
		md.append("### Fazioni\n\nContro le altre fazioni, senza le partite speculari.\n\n")
				.append("| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |\n")
				.append("|---|---|---|---|---|---|\n");
		for (int faction : stats.factionIds()) {
			Score score = stats.faction(faction);
			md.append("| ").append(BalanceConfig.factionName(faction)).append(" | ").append(number(score.matches()))
					.append(" | ").append(number(score.wins())).append(" | ").append(number(score.draws()))
					.append(" | ").append(number(score.losses())).append(" | ").append(pct(score.rate()))
					.append(" |\n");
		}
		md.append('\n');
	}

	private static void appendMatchups(StringBuilder md, BalanceStats stats) {
		md.append("### Scontri diretti\n\nPunteggio della fazione della riga contro quella della colonna, nei due ")
				.append("posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, ")
				.append("45-55% negli scontri neutri.\n\n| |");
		for (int faction : stats.factionIds()) {
			md.append(' ').append(BalanceConfig.factionName(faction)).append(" |");
		}
		md.append("\n|---|").append("---|".repeat(stats.factionIds().size())).append('\n');
		for (int row : stats.factionIds()) {
			md.append("| ").append(BalanceConfig.factionName(row)).append(" (")
					.append(BalanceConfig.factionName(BalanceConfig.beats(row))).append(") |");
			for (int column : stats.factionIds()) {
				md.append(' ').append(row == column ? "—" : pct(stats.matchup(row, column).rate())).append(" |");
			}
			md.append('\n');
		}
		md.append('\n');
	}

	private static void appendMatches(StringBuilder md, BalanceStats stats) {
		int lifePoints = stats.endReasonCount(EndReason.LIFE_POINTS_ZERO)
				+ stats.endReasonCount(EndReason.BOTH_LIFE_POINTS_ZERO);
		md.append("### Partite\n\n")
				.append("- Primo giocatore (sfidante): ").append(pct(stats.firstPlayer().rate())).append(".\n")
				.append("- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): ")
				.append(pct(share(lifePoints, stats.matches()))).append(". D-01 cercava circa metà.\n")
				.append("- Durata in round: media ").append(decimal1(stats.averageRounds())).append(", minimo ")
				.append(stats.shortestMatch()).append(", mediana ").append(stats.roundsPercentile(50))
				.append(", 90° percentile ").append(stats.roundsPercentile(90)).append(", massimo ")
				.append(stats.longestMatch()).append(".\n\n")
				.append("| Motivo di fine | Partite | Quota |\n|---|---|---|\n");
		for (EndReason reason : EndReason.values()) {
			int count = stats.endReasonCount(reason);
			md.append("| `").append(reason.name()).append("` | ").append(number(count)).append(" | ")
					.append(pct(share(count, stats.matches()))).append(" |\n");
		}
		md.append('\n');
	}

	private void appendGroups(StringBuilder md, BalanceStats stats) {
		Map<String, Double> inDeck = groupRates(stats, catalog, Basis.IN_DECK);
		Map<String, Double> rates = groupRates(stats, catalog, Basis.PLAYED);
		md.append("### Rarità e tipi\n\nPunteggio del lato che ha una carta del gruppo nel mazzo e del lato che ")
				.append("l'ha giocata.\n\n")
				.append("| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |\n|---|---|---|---|---|\n");
		for (String group : GROUPS) {
			List<CardDefinition> cards = catalog.stream().filter(card -> group(card).equals(group)).toList();
			if (cards.isEmpty()) {
				continue;
			}
			int plays = cards.stream().mapToInt(card -> stats.played(card.cardId()).matches()).sum();
			md.append("| ").append(group).append(" | ").append(cards.size()).append(" | ").append(number(plays))
					.append(" | ").append(pct(inDeck.getOrDefault(group, 0.0))).append(" | ")
					.append(pct(rates.getOrDefault(group, 0.0))).append(" |\n");
		}
		md.append('\n');
	}

	private void appendWarnings(StringBuilder md, BalanceStats stats) {
		List<String> warnings = allWarnings(stats);
		md.append("### Avvisi\n\n");
		if (warnings.isEmpty()) {
			md.append("Nessuno.\n\n");
			return;
		}
		warnings.forEach(warning -> md.append("- ").append(warning).append(".\n"));
		md.append('\n');
	}

	private void appendCardChecks(StringBuilder md, BalanceStats stats) {
		Map<String, Double> rates = groupRates(stats, catalog, Basis.PLAYED);
		List<CardDefinition> outOfBand = new ArrayList<>();
		List<CardDefinition> lowSample = new ArrayList<>();
		for (CardDefinition card : catalog) {
			CardCheck check = cardCheck(stats.played(card.cardId()), rates.getOrDefault(group(card), 0.0));
			if (check == CardCheck.ABOVE || check == CardCheck.BELOW) {
				outOfBand.add(card);
			} else if (check == CardCheck.LOW_SAMPLE) {
				lowSample.add(card);
			}
		}
		md.append("### Carte fuori banda\n\nPiù di 10 punti dalla media del gruppo, con almeno ").append(MIN_PLAYS)
				.append(" partite giocata.\n\n");
		if (outOfBand.isEmpty()) {
			md.append("Nessuna.\n\n");
		} else {
			md.append("| Carta | Gruppo | Partite giocata | Punteggio | Media del gruppo | Scarto |\n")
					.append("|---|---|---|---|---|---|\n");
			for (CardDefinition card : outOfBand) {
				Score played = stats.played(card.cardId());
				double group = rates.get(group(card));
				md.append("| ").append(card.name()).append(" | ").append(group(card)).append(" | ")
						.append(number(played.matches())).append(" | ").append(pct(played.rate())).append(" | ")
						.append(pct(group)).append(" | ").append(points(played.rate() - group)).append(" |\n");
			}
			md.append('\n');
		}
		md.append("### Campione basso\n\nMeno di ").append(MIN_PLAYS).append(" partite giocata: non si valutano.\n\n");
		if (lowSample.isEmpty()) {
			md.append("Nessuna.\n\n");
		} else {
			md.append(lowSample.stream()
					.map(card -> card.name() + " (" + number(stats.played(card.cardId()).matches()) + ")")
					.collect(Collectors.joining(", "))).append(".\n\n");
		}
	}

	/** Avvisi della configurazione: fazioni, partite e scala delle rarità; le carte hanno le loro tabelle. */
	private List<String> allWarnings(BalanceStats stats) {
		List<String> warnings = new ArrayList<>(warnings(stats));
		// Deciso il 20 novembre: la scala usa il punteggio nel mazzo, perché quello da giocata è spostato in basso
		// (chi perde gioca più carte) e il suo livello assoluto non si confronta tra gruppi
		Map<String, Double> rates = groupRates(stats, catalog, Basis.IN_DECK);
		Map<String, Double> creatures = new LinkedHashMap<>();
		CREATURE_RARITIES.stream().filter(rates::containsKey).forEach(rarity -> creatures.put(rarity,
				rates.get(rarity)));
		rarityScaleWarnings(creatures).forEach(warning -> warnings.add("Scala delle rarità (nel mazzo): "
				+ warning));
		return warnings;
	}

	/**
	 * Media di ogni gruppo, pesata sulle partite; solo i gruppi con almeno una partita.
	 *
	 * @param stats   statistiche di una configurazione
	 * @param catalog carte del catalogo
	 * @param basis   punteggio nel mazzo o da giocata
	 * @return media per gruppo, nell'ordine in cui i gruppi compaiono nel catalogo
	 */
	static Map<String, Double> groupRates(BalanceStats stats, List<CardDefinition> catalog, Basis basis) {
		Map<String, long[]> sums = new LinkedHashMap<>();
		for (CardDefinition card : catalog) {
			Score score = basis == Basis.IN_DECK ? stats.inDeck(card.cardId()) : stats.played(card.cardId());
			long[] sum = sums.computeIfAbsent(group(card), group -> new long[2]);
			sum[0] += 2L * score.wins() + score.draws();
			sum[1] += 2L * score.matches();
		}
		Map<String, Double> rates = new LinkedHashMap<>();
		sums.forEach((group, sum) -> {
			if (sum[1] > 0) {
				rates.put(group, (double) sum[0] / sum[1]);
			}
		});
		return rates;
	}

	private static String group(CardDefinition card) {
		return switch (card.type()) {
			case CREATURE -> card.rarity();
			case SPELL -> "Spell";
			case HIDDEN -> "Hidden";
			case TERRAIN -> "Terreno";
			case ITEM -> "Oggetto";
		};
	}

	private static String factions(CardDefinition card) {
		return card.factions().stream().map(BalanceConfig::factionName).collect(Collectors.joining("/"));
	}

	private static boolean outside(double rate, double min, double max) {
		return rate < min - EPSILON || rate > max + EPSILON;
	}

	private static double share(int count, int total) {
		return total == 0 ? 0 : (double) count / total;
	}

	private static String pct(double rate) {
		return String.format(Locale.ITALY, "%.1f%%", rate * 100);
	}

	private static String points(double gap) {
		return String.format(Locale.ITALY, "%+.1f punti", gap * 100);
	}

	private static String decimal1(double value) {
		return String.format(Locale.ITALY, "%.1f", value);
	}

	private static String number(long value) {
		return String.format(Locale.ITALY, "%,d", value);
	}

	private static String decimal(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String csvText(String text) {
		return text.contains(",") || text.contains("\"") ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
	}
}
