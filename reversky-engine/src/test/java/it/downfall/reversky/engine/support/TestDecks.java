package it.downfall.reversky.engine.support;

import static it.downfall.reversky.engine.support.TestFactions.ARCHIVE;
import static it.downfall.reversky.engine.support.TestFactions.DREAM;
import static it.downfall.reversky.engine.support.TestFactions.INVITATION;
import static it.downfall.reversky.engine.support.TestFactions.WOUND;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;

/**
 * Mazzi di prova: mazzi piccoli in un ordine preciso per i test di regola, e un mazzo completo da 30 carte per
 * fazione con le carte vere del catalogo, per i test di partita completa.
 * <p>
 * Il mazzo completo segue la composizione dell'API (reversky-api/docs/mazzi): 20 creature, 6 Spell, 2 Hidden,
 * 1 Terreno, 1 Oggetto, una copia per carta, nei limiti di rarità (massimo 2 Legendary, 2 Mythic, 5 Rare
 * contando Terreno e Oggetto, una sola Hidden {@code DAMAGE}). Le carte sono prese da
 * {@code docs/bilanciamento/catalogo-finale.csv} nell'ordine del file: per fazione 7 Common, le prime 6 Uncommon,
 * le prime 3 Rare, le prime 2 Mythic e le 2 Legendary, nessuna Mutant; per tutti le prime 2 Spell di ogni
 * sottotipo, Last Sanctuary e Hostile Presence; il Terreno e l'Oggetto della fazione.
 * <p>
 * Id = numero di riga nel CSV. Nomi in inglese quando il catalogo degli effetti ne dà uno; gli altri restano
 * quelli del CSV, in attesa dei nomi inglesi dei DSG. Statistiche e codici effetto sono quelli del catalogo.
 */
public final class TestDecks {

	/** Spell e Hidden uguali in tutti i mazzi completi. */
	private static final List<CardDefinition> SHARED_CARDS = List.of(
			TestCards.spell(93, "Bite of the Reverse", CardSubtype.DAMAGE, "BITE_OF_REVERSE"),
			TestCards.spell(94, "Skin Fracture", CardSubtype.DAMAGE, "SKIN_FRACTURE"),
			TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD, "VEIL_OF_ANCHORED"),
			TestCards.spell(97, "Registry Pact", CardSubtype.SHIELD, "REGISTRY_PACT"),
			TestCards.spell(102, "Call of the Rift", CardSubtype.BURN, "CRACK_CALL"),
			TestCards.spell(103, "Reverse Fusion", CardSubtype.BURN, "REVERSE_FUSION"),
			TestCards.hidden(105, "Last Sanctuary", CardSubtype.SURVIVAL, "LAST_SANCTUARY"),
			TestCards.hidden(109, "Hostile Presence", CardSubtype.DAMAGE, "HOSTILE_PRESENCE"));

	private TestDecks() {
	}

	/**
	 * Mazzo piccolo nell'ordine dato, senza mescolamento: si sa chi pesca cosa (docs/test).
	 *
	 * @param cards carte nell'ordine di pescata
	 * @return lista immutabile
	 */
	public static List<CardDefinition> of(CardDefinition... cards) {
		return List.of(cards);
	}

	/**
	 * Mazzo completo da 30 carte di una fazione: prima le 20 creature, poi Terreno e Oggetto, poi le 6 Spell e le
	 * 2 Hidden. È l'ordine prima del mescolamento.
	 *
	 * @param factionId una delle costanti di {@link TestFactions}
	 * @return lista immutabile di 30 carte
	 */
	public static List<CardDefinition> full(int factionId) {
		List<CardDefinition> deck = new ArrayList<>(factionCards(factionId));
		deck.addAll(SHARED_CARDS);
		return List.copyOf(deck);
	}

	private static List<CardDefinition> factionCards(int factionId) {
		return switch (factionId) {
			case WOUND -> wound();
			case INVITATION -> invitation();
			case ARCHIVE -> archive();
			case DREAM -> dream();
			default -> throw new IllegalArgumentException("Nessun mazzo di prova per la fazione " + factionId);
		};
	}

	private static List<CardDefinition> wound() {
		return List.of(
				creature(46, "Dullers", WOUND, "Common", 5, 9, null),
				creature(47, "Crawlers", WOUND, "Common", 5, 9, null),
				creature(48, "Faceling", WOUND, "Common", 6, 7, null),
				creature(49, "Cercatore di Moquette", WOUND, "Common", 5, 9, null),
				creature(50, "Yellow Hum", WOUND, "Common", 7, 6, null),
				creature(51, "L'Ascensore Senza Pulsanti", WOUND, "Common", 4, 12, null),
				creature(52, "L'Inquilino del Soffitto", WOUND, "Common", 4, 12, null),
				creature(53, "Howlers", WOUND, "Uncommon", 5, 7, "HOWLERS"),
				creature(54, "Wretches", WOUND, "Uncommon", 4, 9, "WRETCHES"),
				creature(55, "Faded Tag", WOUND, "Uncommon", 6, 6, "FADED_TAG"),
				creature(56, "Living Waiting Room", WOUND, "Uncommon", 3, 14, "LIVING_WAITING_ROOM"),
				creature(57, "The Gnawing Witness", WOUND, "Uncommon", 7, 5, "GNAWING_WITNESS"),
				creature(58, "L'Addetto alle Uscite", WOUND, "Uncommon", 5, 8, "EXIT_CLERK"),
				creature(59, "Skin-Stealers", WOUND, "Rare", 5, 6, "SKIN_STEALERS"),
				creature(60, "The Service Double", WOUND, "Rare", 4, 7, "SERVICE_DOUBLE"),
				creature(61, "The Building Director", WOUND, "Rare", 4, 9, "BUILDING_DIRECTOR"),
				creature(63, "Hounds", WOUND, "Mythic", 6, 6, "HOUNDS"),
				creature(64, "Deathmoths", WOUND, "Mythic", 4, 8, "DEATHMOTHS"),
				creature(66, "Smiler", WOUND, "Legendary", 3, 8, "SMILER"),
				creature(67, "The Hollow Wyrm", WOUND, "Legendary", 4, 5, "HOLLOW_WYRM"),
				TestCards.terrain(113, "Echo Rift", WOUND, "ECHO_RIFT"),
				TestCards.item(117, "Badge di Uscita Negata", WOUND, "DENIED_EXIT_BADGE"));
	}

	private static List<CardDefinition> invitation() {
		return List.of(
				creature(1, "Squonk", INVITATION, "Common", 4, 12, null),
				creature(2, "Will-o'-the-Wisp", INVITATION, "Common", 6, 7, null),
				creature(3, "Ash Raven", INVITATION, "Common", 7, 6, null),
				creature(4, "Cane Senz'Ombra", INVITATION, "Common", 5, 9, null),
				creature(5, "Spaventapasseri Zoppo", INVITATION, "Common", 4, 12, null),
				creature(6, "The Coal Crawler", INVITATION, "Common", 5, 9, null),
				creature(7, "Le Falene della Miniera", INVITATION, "Common", 4, 12, null),
				creature(8, "Wampus Cat", INVITATION, "Uncommon", 4, 11, "WAMPUS_CAT"),
				creature(9, "Hidebehind", INVITATION, "Uncommon", 6, 5, "HIDEBEHIND"),
				creature(10, "The Silent Shepherd", INVITATION, "Uncommon", 4, 11, "SILENT_SHEPHERD"),
				creature(11, "White Stag", INVITATION, "Uncommon", 5, 8, "WHITE_STAG"),
				creature(12, "The Nameless Caller", INVITATION, "Uncommon", 4, 10, "NAMELESS_CALLER"),
				creature(13, "Il Raccoglitore di Orme", INVITATION, "Uncommon", 4, 11, "FOOTPRINT_GATHERER"),
				creature(14, "Spearfinger", INVITATION, "Rare", 5, 7, "SPEARFINGER"),
				creature(15, "Bell Witch", INVITATION, "Rare", 4, 8, "BELL_WITCH"),
				creature(16, "The Rain Preacher", INVITATION, "Rare", 4, 10, "RAIN_PREACHER"),
				creature(18, "Mothman", INVITATION, "Mythic", 4, 7, "MOTHMAN"),
				creature(19, "Bigfoot", INVITATION, "Mythic", 4, 6, "BIGFOOT"),
				creature(21, "Wendigo", INVITATION, "Legendary", 4, 7, "WENDIGO"),
				creature(22, "La Regina dei Sentieri Perduti", INVITATION, "Legendary", 5, 10, "QUEEN_OF_LOST_PATHS"),
				TestCards.terrain(114, "Hollow Clearing", INVITATION, "HOLLOW_CLEARING"),
				TestCards.item(118, "Bussola dei Sentieri Falsi", INVITATION, "FALSE_PATHS_COMPASS"));
	}

	private static List<CardDefinition> archive() {
		return List.of(
				creature(23, "SCP-131", ARCHIVE, "Common", 4, 12, null),
				creature(24, "SCP-999", ARCHIVE, "Common", 4, 12, null),
				creature(25, "SCP-1762", ARCHIVE, "Common", 6, 7, null),
				creature(26, "Archive Puppet", ARCHIVE, "Common", 5, 9, null),
				creature(27, "Eternal Cup", ARCHIVE, "Common", 5, 9, null),
				creature(28, "Il Timbro di Nessuno", ARCHIVE, "Common", 7, 6, null),
				creature(29, "La Falena del Fascicolo Rosso", ARCHIVE, "Common", 4, 12, null),
				creature(30, "SCP-173", ARCHIVE, "Uncommon", 6, 6, "SCP_173"),
				creature(31, "SCP-096", ARCHIVE, "Uncommon", 4, 10, "SCP_096"),
				creature(32, "SCP-049", ARCHIVE, "Uncommon", 4, 10, "SCP_049"),
				creature(33, "Il Collezionista di Nomi", ARCHIVE, "Uncommon", 4, 10, "NAME_COLLECTOR"),
				creature(34, "SCP-053", ARCHIVE, "Uncommon", 3, 14, "SCP_053"),
				creature(35, "SCP-2006", ARCHIVE, "Uncommon", 4, 10, "SCP_2006"),
				creature(37, "SCP-106", ARCHIVE, "Rare", 4, 9, "SCP_106"),
				creature(38, "SCP-682", ARCHIVE, "Rare", 4, 9, "SCP_682"),
				creature(39, "SCP-2521", ARCHIVE, "Rare", 4, 9, "SCP_2521"),
				creature(41, "SCP-2317", ARCHIVE, "Mythic", 5, 5, "SCP_2317"),
				creature(42, "SCP-3000", ARCHIVE, "Mythic", 4, 7, "SCP_3000"),
				creature(44, "SCP-001", ARCHIVE, "Legendary", 3, 6, "SCP_001"),
				creature(45, "SCP-3999", ARCHIVE, "Legendary", 3, 11, "SCP_3999"),
				TestCards.terrain(115, "Watching Rift", ARCHIVE, "WATCHING_RIFT"),
				TestCards.item(119, "Fascicolo Sigillato 0", ARCHIVE, "SEALED_FILE_ZERO"));
	}

	private static List<CardDefinition> dream() {
		return List.of(
				creature(68, "Kasa-obake", DREAM, "Common", 5, 9, null),
				creature(69, "Chochin-obake", DREAM, "Common", 6, 7, null),
				creature(70, "Zashiki-warashi", DREAM, "Common", 4, 12, null),
				creature(71, "Kodama", DREAM, "Common", 4, 12, null),
				creature(72, "Temple Tanuki", DREAM, "Common", 5, 9, null),
				creature(73, "The Bone Lantern", DREAM, "Common", 7, 6, null),
				creature(74, "La Sposa delle Campane", DREAM, "Common", 4, 12, null),
				creature(75, "Kappa", DREAM, "Uncommon", 6, 6, "KAPPA"),
				creature(76, "Nurikabe", DREAM, "Uncommon", 4, 10, "NURIKABE"),
				creature(77, "Noppera-bō", DREAM, "Uncommon", 4, 10, "NOPPERA_BO"),
				creature(78, "Rokurokubi", DREAM, "Uncommon", 7, 5, "ROKUROKUBI"),
				creature(79, "The Threadbound Doll", DREAM, "Uncommon", 6, 6, "THREADBOUND_DOLL"),
				creature(80, "L'Ombra del Torii", DREAM, "Uncommon", 3, 15, "TORII_SHADOW"),
				creature(82, "Yuki-onna", DREAM, "Rare", 4, 8, "YUKI_ONNA"),
				creature(83, "Jorōgumo", DREAM, "Rare", 4, 7, "JOROGUMO"),
				creature(84, "Nure-onna", DREAM, "Rare", 7, 5, "NURE_ONNA"),
				creature(86, "Baku", DREAM, "Mythic", 4, 8, "BAKU"),
				creature(87, "The Unseen Matriarch", DREAM, "Mythic", 4, 8, "UNSEEN_MATRIARCH"),
				creature(89, "Shuten-dōji", DREAM, "Legendary", 3, 9, "SHUTEN_DOJI"),
				creature(90, "Kitsune", DREAM, "Legendary", 4, 6, "KITSUNE"),
				TestCards.terrain(116, "Devouring Abyss", DREAM, "DEVOURING_ABYSS"),
				TestCards.item(120, "Campanello del Torii", DREAM, "TORII_BELL"));
	}

	private static CardDefinition creature(int cardId, String name, int factionId, String rarity, int attack,
			int defense, String effectCode) {
		return TestCards.creature(cardId, name, factionId, null, rarity, attack, defense, effectCode);
	}
}
