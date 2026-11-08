package it.downfall.reversky.api.support;

import java.util.concurrent.ThreadLocalRandom;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.catalog.repository.RarityRepository;

/**
 * Crea carte e rarità di prova valide sul database dei test (docs/test, "Dati di test").
 * <p>
 * Finché non esiste POST /admin/cards (API-14) le carte si creano solo da qui.
 * <p>
 * Non è un {@code @TestComponent} di proposito: aggiungerlo a {@code @Import} cambierebbe la
 * configurazione del test, e Spring avvierebbe un secondo contesto con un secondo container.
 * Si crea nel test con {@code new TestCards(catalogCardRepository, rarityRepository)}.
 * <p>
 * Le carte non si cancellano mai, quindi restano tra un test e l'altro: i nomi vanno resi
 * univoci con {@link #uniqueName(String)}.
 * <p>
 * Rarità: {@link #common()}, {@link #uncommon()} e {@link #rare()} cercano la rarità con il
 * sorting_order fisso (1, 2, 3) e la creano senza massimi se non c'è; quando esisterà il seed di
 * #85 troveranno quella. I test non devono quindi contare sui loro massimi: per provarli si crea
 * una rarità propria con {@link #createRarity(Integer, Integer)}, che usa un ordine casuale ≥ 1000.
 */
public class TestCards {

	public static final String EFFECT_CODE = "TEST_EFFECT";
	public static final String IMAGE_PATH = "/images/cards/test.webp";
	public static final int CREATURE_ATK = 5;
	public static final int CREATURE_DEF = 6;

	private final CatalogCardRepository catalogCardRepository;
	private final RarityRepository rarityRepository;

	public TestCards(CatalogCardRepository catalogCardRepository, RarityRepository rarityRepository) {
		this.catalogCardRepository = catalogCardRepository;
		this.rarityRepository = rarityRepository;
	}

	/**
	 * Carta originale valida del tipo indicato, con la rarità predefinita del tipo
	 * (Rare per i Terreni, Common per gli altri).
	 */
	public CatalogCard create(String name, CardType type) {
		return create(name, type, defaultRarity(type));
	}

	/**
	 * Carta originale del tipo e della rarità indicati: statistiche solo per le creature, sottotipo
	 * solo per Spell e Hidden, effetto per tutti tranne le creature. Non controlla che la rarità
	 * sia ammessa per il tipo (il database non lo impone), così si possono preparare anche casi sbagliati.
	 */
	public CatalogCard create(String name, CardType type, Rarity rarity) {
		Integer atk = type.hasStats() ? CREATURE_ATK : null;
		Integer def = type.hasStats() ? CREATURE_DEF : null;
		String effectCode = type.requiresEffect() ? EFFECT_CODE : null;

		return catalogCardRepository.save(new CatalogCard(null, rarity, name, type, defaultSubtype(type), effectCode,
				IMAGE_PATH, "Test effect", "Test flavor", atk, def, null));
	}

	/**
	 * Creatura originale Common con nome univoco.
	 */
	public CatalogCard createCreature() {
		return create(uniqueName("Creature"), CardType.CREATURE);
	}

	/**
	 * Variante di {@code original} con lo stesso nome e i suoi campi di gioco.
	 */
	public CatalogCard createVariantOf(CatalogCard original) {
		return createVariantOf(original, original.getName());
	}

	/**
	 * Variante di {@code original} con il nome indicato e i suoi campi di gioco, rarità compresa.
	 */
	public CatalogCard createVariantOf(CatalogCard original, String name) {
		return catalogCardRepository.save(new CatalogCard(original, original.getRarity(), name, original.getCardType(), original.getCardSubtype(),
				original.getEffectCode(), "/images/cards/test-event.webp", original.getEffectDescription(), "Event flavor",
				original.getAtk(), original.getDef(), original.getCopyLimit()));
	}

	/**
	 * Disattiva la carta e la salva.
	 */
	public CatalogCard deactivate(CatalogCard card) {
		card.deactivate();
		return catalogCardRepository.save(card);
	}

	/**
	 * Rarità con sorting_order {@link Rarity#COMMON_ORDER}, creata se manca.
	 */
	public Rarity common() {
		return findOrCreateRarity("Common", Rarity.COMMON_ORDER);
	}

	/**
	 * Rarità con sorting_order {@link Rarity#UNCOMMON_ORDER}, creata se manca.
	 */
	public Rarity uncommon() {
		return findOrCreateRarity("Uncommon", Rarity.UNCOMMON_ORDER);
	}

	/**
	 * Rarità con sorting_order {@link Rarity#RARE_ORDER}, creata se manca.
	 */
	public Rarity rare() {
		return findOrCreateRarity("Rare", Rarity.RARE_ORDER);
	}

	/**
	 * Rarità nuova con nome univoco, i massimi indicati (null = nessun limite) e un sorting_order
	 * casuale ≥ 1000, quindi diversa da Common, Uncommon e Rare: la ammettono solo le creature.
	 */
	public Rarity createRarity(Integer maxAtk, Integer maxDef) {
		int sortingOrder = ThreadLocalRandom.current().nextInt(1_000, 1_000_000_000);
		return rarityRepository.save(new Rarity(uniqueName("Rarity"), 3, null, maxAtk, maxDef, sortingOrder));
	}

	/**
	 * Nome che non si ripete tra un test e l'altro, es. "Creature 1a2b3c4d".
	 */
	public static String uniqueName(String prefix) {
		return prefix + " " + TestUsers.uniqueSuffix();
	}

	// Cerca per sorting_order e mai per nome, come fa CardType; i massimi restano null
	private Rarity findOrCreateRarity(String name, int sortingOrder) {
		return rarityRepository.findAll().stream()
			.filter(rarity -> rarity.getSortingOrder() == sortingOrder)
			.findFirst()
			.orElseGet(() -> rarityRepository.save(new Rarity(name, 3, null, null, null, sortingOrder)));
	}

	// Una rarità ammessa dal tipo
	private Rarity defaultRarity(CardType type) {
		return (type == CardType.TERRAIN) ? rare() : common();
	}

	// Un sottotipo valido per i tipi che lo richiedono
	private static CardSubtype defaultSubtype(CardType type) {
		return switch (type) {
			case SPELL -> CardSubtype.BURN;
			case HIDDEN -> CardSubtype.SURVIVAL;
			case CREATURE, TERRAIN -> null;
		};
	}
}
