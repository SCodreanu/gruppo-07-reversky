package it.downfall.reversky.api.catalog.repository;

import static it.downfall.reversky.api.support.TestCards.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link CatalogCardRepository} e {@link CatalogCardSpecifications} su un PostgreSQL vero.
 * <p>
 * Verifica tre cose: che l'entità corrisponda alla tabella di V5 e V6 (tipi, enum e rarità letti e scritti),
 * che i vincoli di V5 e V6 rifiutino le carte non valide anche se il service sbagliasse, e che metodi
 * derivati e filtri restituiscano le righe giuste.
 * <p>
 * Le carte non si cancellano, quindi ogni test usa nomi univoci e guarda solo le proprie carte.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class CatalogCardRepositoryIT {

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	// Non serve al test: con la stessa configurazione degli altri IT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Autowired
	private RarityRepository rarityRepository;

	private TestCards testCards;

	// Rarità delle carte costruite a mano in questo test: le creature la ammettono sempre
	private Rarity common;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository, rarityRepository);
		common = testCards.common();
	}

	// --- mappatura ---

	@Test
	@DisplayName("A creature is saved and read back with all its fields")
	void save_creature_readsBackAllFields() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Wendigo"), CardType.CREATURE, null, "WENDIGO",
				"/images/cards/wendigo.webp", "Loses 1 DEF", "Hunger and frost", 18, 17, 1);

		// Act
		Integer id = catalogCardRepository.save(card).getId();
		CatalogCard reloaded = catalogCardRepository.findById(id).orElseThrow();

		// Assert
		assertThat(reloaded.getName()).isEqualTo(card.getName());
		assertThat(reloaded.getCardType()).isEqualTo(CardType.CREATURE);
		assertThat(reloaded.getCardSubtype()).isNull();
		assertThat(reloaded.getEffectCode()).isEqualTo("WENDIGO");
		assertThat(reloaded.getImagePath()).isEqualTo("/images/cards/wendigo.webp");
		assertThat(reloaded.getEffectDescription()).isEqualTo("Loses 1 DEF");
		assertThat(reloaded.getFlavorText()).isEqualTo("Hunger and frost");
		assertThat(reloaded.getAtk()).isEqualTo(18);
		assertThat(reloaded.getDef()).isEqualTo(17);
		assertThat(reloaded.getCopyLimit()).isEqualTo(1);
		assertThat(reloaded.isActive()).isTrue();
		assertThat(reloaded.getVariantOf()).isNull();
		// getId() su un proxy non lo carica, quindi funziona anche fuori transazione
		assertThat(reloaded.getRarity().getId()).isEqualTo(common.getId());
	}

	@Test
	@DisplayName("The rarity of a card is loaded lazily")
	void findById_rarity_isLazyProxy() {
		// Arrange
		CatalogCard card = testCards.createCreature();

		transactionTemplate.executeWithoutResult(status -> {
			// Act
			CatalogCard reloaded = catalogCardRepository.findById(card.getId()).orElseThrow();

			// Assert: la rarità si carica solo quando serve un suo campo diverso dall'id
			assertThat(Hibernate.isInitialized(reloaded.getRarity())).isFalse();
			assertThat(reloaded.getRarity().getSortingOrder()).isEqualTo(Rarity.COMMON_ORDER);
		});
	}

	@Test
	@DisplayName("Every card type and subtype is written and read as a PostgreSQL enum")
	void save_everyType_readsBackEnums() {
		// Arrange + Act
		CatalogCard spell = testCards.create(uniqueName("Spell"), CardType.SPELL);
		CatalogCard hidden = testCards.create(uniqueName("Hidden"), CardType.HIDDEN);
		CatalogCard terrain = testCards.create(uniqueName("Terrain"), CardType.TERRAIN);

		// Assert
		assertThat(catalogCardRepository.findById(spell.getId()).orElseThrow().getCardSubtype()).isEqualTo(CardSubtype.BURN);
		assertThat(catalogCardRepository.findById(hidden.getId()).orElseThrow().getCardSubtype()).isEqualTo(CardSubtype.SURVIVAL);
		assertThat(catalogCardRepository.findById(terrain.getId()).orElseThrow().getCardType()).isEqualTo(CardType.TERRAIN);
	}

	@Test
	@DisplayName("A variant is saved with the id of its original")
	void save_variant_keepsOriginalId() {
		// Arrange
		CatalogCard original = testCards.createCreature();

		// Act
		CatalogCard variant = testCards.createVariantOf(original);
		CatalogCard reloaded = catalogCardRepository.findById(variant.getId()).orElseThrow();

		// Assert: getId() su un proxy non lo carica, quindi funziona anche fuori transazione
		assertThat(reloaded.getVariantOf().getId()).isEqualTo(original.getId());
		assertThat(reloaded.isVariant()).isTrue();
	}

	// --- vincoli di V5 ---

	@Test
	@DisplayName("The database rejects a creature with DEF 0")
	void save_creatureWithZeroDef_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.CREATURE, null, null,
				TestCards.IMAGE_PATH, null, null, 3, 0, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_stats_match_type");
	}

	@Test
	@DisplayName("The database rejects a creature without stats")
	void save_creatureWithoutStats_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.CREATURE, null, null,
				TestCards.IMAGE_PATH, null, null, null, null, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_stats_match_type");
	}

	@Test
	@DisplayName("The database rejects a spell with stats")
	void save_spellWithStats_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.SPELL, CardSubtype.BURN, TestCards.EFFECT_CODE,
				TestCards.IMAGE_PATH, null, null, 1, 1, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_stats_match_type");
	}

	@Test
	@DisplayName("The database rejects a spell without a subtype")
	void save_spellWithoutSubtype_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.SPELL, null, TestCards.EFFECT_CODE,
				TestCards.IMAGE_PATH, null, null, null, null, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_subtype_matches_type");
	}

	@Test
	@DisplayName("The database rejects a hidden card with a spell subtype")
	void save_hiddenWithSpellSubtype_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.HIDDEN, CardSubtype.BURN, TestCards.EFFECT_CODE,
				TestCards.IMAGE_PATH, null, null, null, null, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_subtype_matches_type");
	}

	@Test
	@DisplayName("The database rejects a creature with a subtype")
	void save_creatureWithSubtype_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.CREATURE, CardSubtype.DAMAGE, null,
				TestCards.IMAGE_PATH, null, null, 1, 1, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_subtype_matches_type");
	}

	@Test
	@DisplayName("The database rejects a terrain without an effect")
	void save_terrainWithoutEffect_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.TERRAIN, null, null,
				TestCards.IMAGE_PATH, null, null, null, null, null);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_effect_required");
	}

	@Test
	@DisplayName("The database rejects a copy limit of 0")
	void save_zeroCopyLimit_isRejected() {
		// Arrange
		CatalogCard card = new CatalogCard(null, common, uniqueName("Bad"), CardType.CREATURE, null, null,
				TestCards.IMAGE_PATH, null, null, 1, 1, 0);

		// Act + Assert
		assertRejectedBy(card, "chk_catalog_cards_copy_limit");
	}

	@Test
	@DisplayName("The database rejects a card that is a variant of itself")
	void update_variantOfItself_isRejected() {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert: con l'entità non si arriva a questo stato prima che la riga abbia un id, quindi SQL diretto
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE catalog_cards SET variant_of_id = id WHERE id = ?", card.getId()))
			.isInstanceOf(DataIntegrityViolationException.class)
			.rootCause()
			.hasMessageContaining("chk_catalog_cards_not_own_variant");
	}

	// --- vincoli di V6 ---

	@Test
	@DisplayName("The database rejects a card without a rarity")
	void update_nullRarity_isRejected() {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert: SQL diretto, l'entità ha optional = false e non arriverebbe al database
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE catalog_cards SET rarity_id = NULL WHERE id = ?", card.getId()))
			.isInstanceOf(DataIntegrityViolationException.class)
			.rootCause()
			.hasMessageContaining("rarity_id");
	}

	@Test
	@DisplayName("The database rejects a rarity that does not exist")
	void update_unknownRarity_isRejected() {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert: il nome della chiave esterna è quello generato da PostgreSQL (V6 non lo sceglie)
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE catalog_cards SET rarity_id = -1 WHERE id = ?", card.getId()))
			.isInstanceOf(DataIntegrityViolationException.class)
			.rootCause()
			.hasMessageContaining("catalog_cards_rarity_id_fkey");
	}

	@Test
	@DisplayName("Two originals with the same name are rejected")
	void save_twoOriginalsWithSameName_isRejected() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard duplicate = new CatalogCard(null, common, original.getName(), CardType.CREATURE, null, null,
				TestCards.IMAGE_PATH, null, null, 1, 1, null);

		// Act + Assert
		assertRejectedBy(duplicate, "uq_catalog_cards_base_name");
	}

	@Test
	@DisplayName("A variant can reuse the name of its original")
	void save_variantWithOriginalName_isAccepted() {
		// Arrange
		CatalogCard original = testCards.createCreature();

		// Act
		CatalogCard variant = testCards.createVariantOf(original);

		// Assert
		assertThat(variant.getId()).isNotNull();
		assertThat(variant.getName()).isEqualTo(original.getName());
	}

	// --- metodi derivati ---

	@Test
	@DisplayName("A name used by another original is reported as taken")
	void existsByNameAndVariantOfIsNullAndIdNot_otherOriginal_returnsTrue() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard other = testCards.createCreature();

		// Act + Assert
		assertThat(catalogCardRepository.existsByNameAndVariantOfIsNullAndIdNot(original.getName(), other.getId())).isTrue();
	}

	@Test
	@DisplayName("The name of the excluded card itself is not reported as taken")
	void existsByNameAndVariantOfIsNullAndIdNot_sameCard_returnsFalse() {
		// Arrange
		CatalogCard original = testCards.createCreature();

		// Act + Assert
		assertThat(catalogCardRepository.existsByNameAndVariantOfIsNullAndIdNot(original.getName(), original.getId())).isFalse();
	}

	@Test
	@DisplayName("A name used only by a variant is not reported as taken")
	void existsByNameAndVariantOfIsNullAndIdNot_onlyVariant_returnsFalse() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original, uniqueName("Event"));
		CatalogCard other = testCards.createCreature();

		// Act + Assert
		assertThat(catalogCardRepository.existsByNameAndVariantOfIsNullAndIdNot(variant.getName(), other.getId())).isFalse();
	}

	@Test
	@DisplayName("All the variants of an original are found, and nothing else")
	void findAllByVariantOf_original_returnsOnlyItsVariants() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard firstVariant = testCards.createVariantOf(original);
		CatalogCard secondVariant = testCards.createVariantOf(original, uniqueName("Event"));
		testCards.createVariantOf(testCards.createCreature());

		// Act
		List<CatalogCard> variants = catalogCardRepository.findAllByVariantOf(original);

		// Assert
		assertThat(variants).extracting(CatalogCard::getId).containsExactlyInAnyOrder(firstVariant.getId(), secondVariant.getId());
	}

	@Test
	@DisplayName("An original knows whether it has variants")
	void existsByVariantOf_withAndWithoutVariants() {
		// Arrange
		CatalogCard withVariant = testCards.createCreature();
		testCards.createVariantOf(withVariant);
		CatalogCard withoutVariant = testCards.createCreature();

		// Act + Assert
		assertThat(catalogCardRepository.existsByVariantOf(withVariant)).isTrue();
		assertThat(catalogCardRepository.existsByVariantOf(withoutVariant)).isFalse();
	}

	// --- filtri (Specification) ---

	@Test
	@DisplayName("The type filter keeps only cards of that type")
	void hasType_spell_returnsOnlySpells() {
		// Arrange: il token nel nome limita la ricerca alle carte di questo test
		String token = TestUsers.uniqueSuffix();
		CatalogCard spell = testCards.create("Spell " + token, CardType.SPELL);
		testCards.create("Creature " + token, CardType.CREATURE);

		// Act
		List<CatalogCard> result = catalogCardRepository.findAll(Specification.allOf(
				CatalogCardSpecifications.nameContains(token),
				CatalogCardSpecifications.hasType(CardType.SPELL)));

		// Assert
		assertThat(result).extracting(CatalogCard::getId).containsExactly(spell.getId());
	}

	@Test
	@DisplayName("The name filter ignores case and surrounding spaces")
	void nameContains_differentCaseAndSpaces_findsCard() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		CatalogCard card = testCards.create("Wendigo " + token, CardType.CREATURE);

		// Act
		List<CatalogCard> result = catalogCardRepository.findAll(
				CatalogCardSpecifications.nameContains("  WENDIGO " + token.toUpperCase() + "  "));

		// Assert
		assertThat(result).extracting(CatalogCard::getId).containsExactly(card.getId());
	}

	@Test
	@DisplayName("The rarity filter keeps only cards of that rarity")
	void hasRarity_rarityId_returnsOnlyThatRarity() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		Rarity other = testCards.createRarity(null, null);
		CatalogCard commonCard = testCards.create("Common " + token, CardType.CREATURE, common);
		testCards.create("Other " + token, CardType.CREATURE, other);

		// Act
		List<CatalogCard> result = catalogCardRepository.findAll(Specification.allOf(
				CatalogCardSpecifications.nameContains(token),
				CatalogCardSpecifications.hasRarity(common.getId())));

		// Assert
		assertThat(result).extracting(CatalogCard::getId).containsExactly(commonCard.getId());
	}

	@Test
	@DisplayName("The rarity filter with an unknown id finds nothing")
	void hasRarity_unknownId_returnsEmpty() {
		// Arrange
		testCards.createCreature();

		// Act
		List<CatalogCard> result = catalogCardRepository.findAll(CatalogCardSpecifications.hasRarity(-1));

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("Empty filters do not filter anything")
	void emptyFilters_returnEveryCard() {
		// Arrange
		testCards.createCreature();
		Specification<CatalogCard> noFilters = Specification.allOf(
				CatalogCardSpecifications.hasType(null),
				CatalogCardSpecifications.nameContains("   "),
				CatalogCardSpecifications.hasRarity(null),
				CatalogCardSpecifications.isActive(null));

		// Act + Assert
		assertThat(catalogCardRepository.count(noFilters)).isEqualTo(catalogCardRepository.count());
	}

	@Test
	@DisplayName("The active filter separates active and deactivated cards")
	void isActive_trueAndFalse_splitCards() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		CatalogCard active = testCards.create("Active " + token, CardType.CREATURE);
		CatalogCard inactive = testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act
		List<CatalogCard> onlyActive = catalogCardRepository.findAll(Specification.allOf(
				CatalogCardSpecifications.nameContains(token), CatalogCardSpecifications.isActive(true)));
		List<CatalogCard> onlyInactive = catalogCardRepository.findAll(Specification.allOf(
				CatalogCardSpecifications.nameContains(token), CatalogCardSpecifications.isActive(false)));

		// Assert
		assertThat(onlyActive).extracting(CatalogCard::getId).containsExactly(active.getId());
		assertThat(onlyInactive).extracting(CatalogCard::getId).containsExactly(inactive.getId());
	}

	@Test
	@DisplayName("Filters work together with pagination and sorting")
	void filtersWithPageable_returnPageAndTotals() {
		// Arrange: 3 creature e 1 spell con lo stesso token
		String token = TestUsers.uniqueSuffix();
		CatalogCard first = testCards.create("A " + token, CardType.CREATURE);
		CatalogCard second = testCards.create("B " + token, CardType.CREATURE);
		testCards.create("C " + token, CardType.CREATURE);
		testCards.create("D " + token, CardType.SPELL);

		// Act
		Page<CatalogCard> page = catalogCardRepository.findAll(Specification.allOf(
				CatalogCardSpecifications.nameContains(token), CatalogCardSpecifications.hasType(CardType.CREATURE)),
				PageRequest.of(0, 2, Sort.by("name", "id")));

		// Assert
		assertThat(page.getContent()).extracting(CatalogCard::getId).containsExactly(first.getId(), second.getId());
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	// --- copyRulesFrom con un proxy ---

	@Test
	@DisplayName("Copying the rules from a lazy proxy of the original copies the real values")
	void copyRulesFrom_lazyProxy_copiesRealValues() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);

		transactionTemplate.executeWithoutResult(status -> {
			CatalogCard loadedVariant = catalogCardRepository.findById(variant.getId()).orElseThrow();
			CatalogCard proxy = loadedVariant.getVariantOf();

			// Il test ha senso solo se l'originale è davvero un proxy non ancora caricato
			assertThat(Hibernate.isInitialized(proxy)).isFalse();

			// Valori diversi da quelli dell'originale, per vedere che vengono sovrascritti
			loadedVariant.updateRules(testCards.rare(), CardType.TERRAIN, null, "OTHER", null, null, null, 3);

			// Act
			loadedVariant.copyRulesFrom(proxy);

			// Assert: la rarità non è null anche se proxy non era caricato (copyRulesFrom usa i getter)
			assertThat(loadedVariant.getRarity()).isNotNull();
			assertThat(loadedVariant.getRarity().getId()).isEqualTo(common.getId());
			assertThat(loadedVariant.getCardType()).isEqualTo(CardType.CREATURE);
			assertThat(loadedVariant.getEffectCode()).isNull();
			assertThat(loadedVariant.getAtk()).isEqualTo(TestCards.CREATURE_ATK);
			assertThat(loadedVariant.getDef()).isEqualTo(TestCards.CREATURE_DEF);
			assertThat(loadedVariant.getCopyLimit()).isNull();

			// Niente da salvare: il test controlla solo l'entità in memoria
			status.setRollbackOnly();
		});
	}

	// Il database rifiuta la carta con il vincolo indicato
	private void assertRejectedBy(CatalogCard card, String constraintName) {
		assertThatThrownBy(() -> catalogCardRepository.saveAndFlush(card))
			.isInstanceOf(DataIntegrityViolationException.class)
			.rootCause()
			.hasMessageContaining(constraintName);
	}
}
