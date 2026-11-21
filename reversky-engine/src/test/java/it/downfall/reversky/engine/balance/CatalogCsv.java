package it.downfall.reversky.engine.balance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Legge il catalogo finale di 120 carte ({@code docs/bilanciamento/catalogo-finale.csv}) e lo trasforma nelle
 * {@link CardDefinition} che l'API manderebbe all'engine.
 * <p>
 * Id = numero di riga del CSV (la prima carta è 1), come in {@code TestDecks}; nome com'è nel CSV. Spell e Hidden non
 * hanno rarità nel CSV: valgono Uncommon, come in {@code TestCards} (2 copie nel mazzo). Le fazioni hanno gli id di
 * {@link TestFactions}.
 */
public final class CatalogCsv {

	/** Percorso del catalogo finale, relativo alla cartella del modulo (quella in cui Gradle lancia i test). */
	public static final Path DEFAULT_PATH = Path.of("docs", "bilanciamento", "catalogo-finale.csv");

	/** Rarità di Spell e Hidden, che nel CSV non ne hanno una. */
	public static final String SHARED_CARD_RARITY = "Uncommon";

	private static final int NAME = 0;
	private static final int TYPE = 1;
	private static final int SUBTYPE = 2;
	private static final int FACTION = 3;
	private static final int SECONDARY_FACTION = 4;
	private static final int RARITY = 5;
	private static final int ATTACK = 6;
	private static final int DEFENSE = 7;
	private static final int EFFECT_CODE = 8;
	private static final int EFFECT_PRICE = 10;

	private CatalogCsv() {
	}

	/**
	 * Legge il prezzo degli effetti (colonna {@code ps_effetto}, in punti di V) dallo stesso file di {@link #load}.
	 *
	 * @param path percorso del file
	 * @return prezzo per id della carta (numero di riga), solo per le carte che ne hanno uno; mappa immutabile
	 * @throws IOException se il file non si legge
	 */
	public static Map<Integer, Double> effectPrices(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		Map<Integer, Double> prices = new LinkedHashMap<>();
		for (int row = 1; row < lines.size(); row++) {
			if (!lines.get(row).isBlank()) {
				String price = splitLine(lines.get(row)).get(EFFECT_PRICE);
				if (!price.isBlank()) {
					prices.put(row, Double.valueOf(price));
				}
			}
		}
		return Collections.unmodifiableMap(prices);
	}

	/**
	 * Legge il catalogo da un file CSV in UTF-8 con l'intestazione nella prima riga.
	 *
	 * @param path percorso del file
	 * @return le carte nell'ordine del file; lista immutabile
	 * @throws IOException se il file non si legge
	 */
	public static List<CardDefinition> load(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		List<CardDefinition> cards = new ArrayList<>();
		for (int row = 1; row < lines.size(); row++) {
			if (!lines.get(row).isBlank()) {
				cards.add(toCard(row, splitLine(lines.get(row))));
			}
		}
		return List.copyOf(cards);
	}

	/**
	 * Divide una riga CSV nei suoi campi: le virgole dentro le virgolette non dividono, due virgolette di seguito
	 * dentro un campo tra virgolette sono una virgoletta.
	 *
	 * @param line riga senza il fine riga
	 * @return i campi, anche vuoti
	 */
	public static List<String> splitLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (quoted && c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
				field.append('"');
				i++;
			} else if (c == '"') {
				quoted = !quoted;
			} else if (c == ',' && !quoted) {
				fields.add(field.toString());
				field.setLength(0);
			} else {
				field.append(c);
			}
		}
		fields.add(field.toString());
		return fields;
	}

	private static CardDefinition toCard(int cardId, List<String> fields) {
		CardType type = type(fields.get(TYPE));
		boolean shared = type == CardType.SPELL || type == CardType.HIDDEN;
		String rarity = shared ? SHARED_CARD_RARITY : fields.get(RARITY);
		boolean creature = type == CardType.CREATURE;
		return new CardDefinition(cardId, fields.get(NAME), type, rarity, TestCards.rarityOrder(rarity),
				faction(fields.get(FACTION)), faction(fields.get(SECONDARY_FACTION)),
				creature ? Integer.valueOf(fields.get(ATTACK)) : null,
				creature ? Integer.valueOf(fields.get(DEFENSE)) : null,
				shared ? CardSubtype.valueOf(fields.get(SUBTYPE)) : null, blankToNull(fields.get(EFFECT_CODE)));
	}

	private static CardType type(String value) {
		return switch (value) {
			case "Creatura" -> CardType.CREATURE;
			case "Spell" -> CardType.SPELL;
			case "Hidden" -> CardType.HIDDEN;
			case "Terreno" -> CardType.TERRAIN;
			case "Oggetto" -> CardType.ITEM;
			default -> throw new IllegalArgumentException("Tipo di carta sconosciuto nel catalogo: " + value);
		};
	}

	private static Integer faction(String value) {
		return switch (value) {
			case "" -> null;
			case "Wound" -> TestFactions.WOUND;
			case "Invitation" -> TestFactions.INVITATION;
			case "Archive" -> TestFactions.ARCHIVE;
			case "Dream" -> TestFactions.DREAM;
			default -> throw new IllegalArgumentException("Fazione sconosciuta nel catalogo: " + value);
		};
	}

	private static String blankToNull(String value) {
		return value.isBlank() ? null : value;
	}
}
