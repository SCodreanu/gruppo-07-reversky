package it.downfall.reversky.api.support;

import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;

/**
 * Crea carte di prova valide sul database dei test (docs/test, "Dati di test").
 * <p>
 * Finché non esiste POST /admin/cards (API-14) le carte si creano solo da qui.
 * <p>
 * Non è un {@code @TestComponent} di proposito: aggiungerlo a {@code @Import} cambierebbe la
 * configurazione del test, e Spring avvierebbe un secondo contesto con un secondo container.
 * Si crea nel test con {@code new TestCards(catalogCardRepository)}.
 * <p>
 * Le carte non si cancellano mai, quindi restano tra un test e l'altro: i nomi vanno resi
 * univoci con {@link #uniqueName(String)}.
 */
public class TestCards {

	public static final String EFFECT_CODE = "TEST_EFFECT";
	public static final String IMAGE_PATH = "/images/cards/test.webp";
	public static final int CREATURE_ATK = 5;
	public static final int CREATURE_DEF = 6;

	private final CatalogCardRepository catalogCardRepository;

	public TestCards(CatalogCardRepository catalogCardRepository) {
		this.catalogCardRepository = catalogCardRepository;
	}

	/**
	 * Carta originale valida del tipo indicato: statistiche solo per le creature, sottotipo solo
	 * per Spell e Hidden, effetto per tutti tranne le creature.
	 */
	public CatalogCard create(String name, CardType type) {
		Integer atk = type.hasStats() ? CREATURE_ATK : null;
		Integer def = type.hasStats() ? CREATURE_DEF : null;
		String effectCode = type.requiresEffect() ? EFFECT_CODE : null;

		return catalogCardRepository.save(new CatalogCard(null, name, type, defaultSubtype(type), effectCode,
				IMAGE_PATH, "Test effect", "Test flavor", atk, def, null));
	}

	/**
	 * Creatura originale con nome univoco.
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
	 * Variante di {@code original} con il nome indicato e i suoi campi di gioco.
	 */
	public CatalogCard createVariantOf(CatalogCard original, String name) {
		return catalogCardRepository.save(new CatalogCard(original, name, original.getCardType(), original.getCardSubtype(),
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
	 * Nome che non si ripete tra un test e l'altro, es. "Creature 1a2b3c4d".
	 */
	public static String uniqueName(String prefix) {
		return prefix + " " + TestUsers.uniqueSuffix();
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
