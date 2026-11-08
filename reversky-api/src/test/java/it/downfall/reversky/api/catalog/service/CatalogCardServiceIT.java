package it.downfall.reversky.api.catalog.service;

import static it.downfall.reversky.api.support.TestCards.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.CatalogCardResponse;
import it.downfall.reversky.api.catalog.dto.CatalogCardUpdateRequest;
import it.downfall.reversky.api.catalog.entity.CardSubtype;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.exception.CatalogCardNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.CatalogCardNotFoundException;
import it.downfall.reversky.api.catalog.exception.CatalogCardRulesViolatedException;
import it.downfall.reversky.api.catalog.exception.InvalidCardVariantException;
import it.downfall.reversky.api.catalog.repository.CatalogCardRepository;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestCards;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link CatalogCardService} su un PostgreSQL vero (docs/backoffice, "Carte").
 * <p>
 * Copre le regole del task: carte mai cancellate, statistiche, sottotipo ed effetto per tipo,
 * nome univoco tra le originali, varianti che puntano solo a originali e che ricevono le modifiche
 * dell'originale. Dopo ogni modifica si rilegge la carta dal database, per non fidarsi della risposta.
 * <p>
 * Le carte non si cancellano, quindi ogni test usa nomi univoci e guarda solo le proprie carte.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class CatalogCardServiceIT {

	// Id che non esiste: le carte di prova non arrivano mai così in alto
	private static final int MISSING_ID = Integer.MAX_VALUE;

	@Autowired
	private CatalogCardService catalogCardService;

	@Autowired
	private CatalogCardRepository catalogCardRepository;

	// Non serve al test: con la stessa configurazione degli altri IT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestCards testCards;

	@BeforeEach
	void setUp() {
		testCards = new TestCards(catalogCardRepository);
	}

	// --- findById ---

	@Test
	@DisplayName("A deactivated card can still be read by id")
	void findById_deactivatedCard_returnsIt() {
		// Arrange
		CatalogCard card = testCards.deactivate(testCards.createCreature());

		// Act
		CatalogCardResponse response = catalogCardService.findById(card.getId());

		// Assert
		assertThat(response.id()).isEqualTo(card.getId());
		assertThat(response.active()).isFalse();
	}

	@Test
	@DisplayName("A variant is returned with the id of its original")
	void findById_variant_returnsVariantOfId() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);

		// Act
		CatalogCardResponse response = catalogCardService.findById(variant.getId());

		// Assert
		assertThat(response.variantOfId()).isEqualTo(original.getId());
	}

	@Test
	@DisplayName("A missing card throws CatalogCardNotFoundException")
	void findById_missingCard_throwsNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.findById(MISSING_ID))
			.isInstanceOf(CatalogCardNotFoundException.class);
	}

	// --- findActive e findAll ---

	@Test
	@DisplayName("The public list hides deactivated cards")
	void findActive_withDeactivatedCard_returnsOnlyActive() {
		// Arrange: il token nel nome limita la ricerca alle carte di questo test
		String token = TestUsers.uniqueSuffix();
		CatalogCard active = testCards.create("Active " + token, CardType.CREATURE);
		testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act
		PageResponse<CatalogCardResponse> page = catalogCardService.findActive(null, token, PageRequest.of(0, 20));

		// Assert
		assertThat(page.content()).extracting(CatalogCardResponse::id).containsExactly(active.getId());
		assertThat(page.totalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("The public list filters by type and keeps variants")
	void findActive_typeFilter_returnsOriginalsAndVariantsOfThatType() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		CatalogCard spell = testCards.create("Spell " + token, CardType.SPELL);
		CatalogCard variant = testCards.createVariantOf(spell, "Spell event " + token);
		testCards.create("Creature " + token, CardType.CREATURE);

		// Act
		PageResponse<CatalogCardResponse> page = catalogCardService.findActive(CardType.SPELL, token, PageRequest.of(0, 20));

		// Assert
		assertThat(page.content()).extracting(CatalogCardResponse::id).containsExactlyInAnyOrder(spell.getId(), variant.getId());
	}

	@Test
	@DisplayName("The sort sent by the client is ignored: cards are always sorted by name")
	void findActive_unknownSortProperty_isIgnored() {
		// Arrange: con il sort del client, una proprietà inesistente farebbe fallire la query
		String token = TestUsers.uniqueSuffix();
		CatalogCard second = testCards.create("B " + token, CardType.CREATURE);
		CatalogCard first = testCards.create("A " + token, CardType.CREATURE);

		// Act
		PageResponse<CatalogCardResponse> page = catalogCardService.findActive(null, token,
				PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "doesNotExist")));

		// Assert
		assertThat(page.content()).extracting(CatalogCardResponse::id).containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("The public list keeps page number, size and totals")
	void findActive_secondPage_returnsPageAndTotals() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		testCards.create("A " + token, CardType.CREATURE);
		testCards.create("B " + token, CardType.CREATURE);
		CatalogCard third = testCards.create("C " + token, CardType.CREATURE);

		// Act
		PageResponse<CatalogCardResponse> page = catalogCardService.findActive(null, token, PageRequest.of(1, 2));

		// Assert
		assertThat(page.content()).extracting(CatalogCardResponse::id).containsExactly(third.getId());
		assertThat(page.page()).isEqualTo(1);
		assertThat(page.size()).isEqualTo(2);
		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("The admin list without the active filter returns active and deactivated cards")
	void findAll_withoutActiveFilter_returnsBoth() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		CatalogCard active = testCards.create("Active " + token, CardType.CREATURE);
		CatalogCard inactive = testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act
		PageResponse<CatalogCardResponse> page = catalogCardService.findAll(null, token, null, PageRequest.of(0, 20));

		// Assert
		assertThat(page.content()).extracting(CatalogCardResponse::id).containsExactly(active.getId(), inactive.getId());
	}

	@Test
	@DisplayName("The admin list can show only deactivated cards")
	void findAll_activeFalse_returnsOnlyDeactivated() {
		// Arrange
		String token = TestUsers.uniqueSuffix();
		testCards.create("Active " + token, CardType.CREATURE);
		CatalogCard inactive = testCards.deactivate(testCards.create("Inactive " + token, CardType.CREATURE));

		// Act
		PageResponse<CatalogCardResponse> page = catalogCardService.findAll(null, token, false, PageRequest.of(0, 20));

		// Assert
		assertThat(page.content()).extracting(CatalogCardResponse::id).containsExactly(inactive.getId());
	}

	// --- update di un'originale ---

	@Test
	@DisplayName("A valid update of an original changes every field")
	void update_original_changesEveryField() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		String newName = uniqueName("Renamed");
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(newName, CardType.SPELL, CardSubtype.SHIELD,
				"SHIELD_TWO", null, null, null, 2, "Shields for 2", "New flavor");

		// Act
		CatalogCardResponse response = catalogCardService.update(card.getId(), request);

		// Assert
		CatalogCard reloaded = reload(card);
		assertThat(response.name()).isEqualTo(newName);
		assertThat(reloaded.getName()).isEqualTo(newName);
		assertThat(reloaded.getCardType()).isEqualTo(CardType.SPELL);
		assertThat(reloaded.getCardSubtype()).isEqualTo(CardSubtype.SHIELD);
		assertThat(reloaded.getEffectCode()).isEqualTo("SHIELD_TWO");
		assertThat(reloaded.getEffectDescription()).isEqualTo("Shields for 2");
		assertThat(reloaded.getAtk()).isNull();
		assertThat(reloaded.getDef()).isNull();
		assertThat(reloaded.getCopyLimit()).isEqualTo(2);
		assertThat(reloaded.getFlavorText()).isEqualTo("New flavor");
	}

	@Test
	@DisplayName("Updating an original updates the game fields of all its variants")
	void update_originalWithVariants_copiesRulesToVariants() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard firstVariant = testCards.createVariantOf(original, uniqueName("Event"));
		CatalogCard secondVariant = testCards.createVariantOf(original);
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(original.getName(), CardType.CREATURE, null,
				"NEW_EFFECT", null, 11, 12, 1, "New effect text", "Original flavor");

		// Act
		catalogCardService.update(original.getId(), request);

		// Assert
		for (CatalogCard variant : new CatalogCard[] {firstVariant, secondVariant}) {
			CatalogCard reloaded = reload(variant);
			assertThat(reloaded.getAtk()).isEqualTo(11);
			assertThat(reloaded.getDef()).isEqualTo(12);
			assertThat(reloaded.getEffectCode()).isEqualTo("NEW_EFFECT");
			assertThat(reloaded.getEffectDescription()).isEqualTo("New effect text");
			assertThat(reloaded.getCopyLimit()).isEqualTo(1);
		}
	}

	@Test
	@DisplayName("Updating an original does not change name, image and flavor text of its variants")
	void update_originalWithVariants_keepsVariantDetails() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		String variantName = uniqueName("Event");
		CatalogCard variant = testCards.createVariantOf(original, variantName);

		// Act
		catalogCardService.update(original.getId(), creatureRequest(uniqueName("Renamed"), null));

		// Assert
		CatalogCard reloaded = reload(variant);
		assertThat(reloaded.getName()).isEqualTo(variantName);
		assertThat(reloaded.getImagePath()).isEqualTo(variant.getImagePath());
		assertThat(reloaded.getFlavorText()).isEqualTo(variant.getFlavorText());
	}

	@Test
	@DisplayName("An original can keep its own name")
	void update_originalSameName_isAccepted() {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act
		CatalogCardResponse response = catalogCardService.update(card.getId(), creatureRequest(card.getName(), null));

		// Assert
		assertThat(response.name()).isEqualTo(card.getName());
	}

	@Test
	@DisplayName("An original cannot take the name of another original")
	void update_originalNameOfAnotherOriginal_throwsNameAlreadyUsed() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCard other = testCards.createCreature();

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), creatureRequest(other.getName(), null)))
			.isInstanceOf(CatalogCardNameAlreadyUsedException.class);
		assertThat(reload(card).getName()).isEqualTo(card.getName());
	}

	@Test
	@DisplayName("A missing card throws CatalogCardNotFoundException on update")
	void update_missingCard_throwsNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(MISSING_ID, creatureRequest(uniqueName("Any"), null)))
			.isInstanceOf(CatalogCardNotFoundException.class);
	}

	// --- regole per tipo ---

	@Test
	@DisplayName("A creature without DEF is rejected and nothing changes")
	void update_creatureWithoutDef_throwsRulesViolated() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(card.getName(), CardType.CREATURE, null,
				null, null, 7, null, null, null, null);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), request))
			.isInstanceOf(CatalogCardRulesViolatedException.class)
			.hasMessageContaining("ATK and DEF");
		assertThat(reload(card).getAtk()).isEqualTo(TestCards.CREATURE_ATK);
	}

	@Test
	@DisplayName("A spell with stats is rejected")
	void update_spellWithStats_throwsRulesViolated() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(card.getName(), CardType.SPELL, CardSubtype.BURN,
				TestCards.EFFECT_CODE, null, 1, 1, null, null, null);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), request))
			.isInstanceOf(CatalogCardRulesViolatedException.class)
			.hasMessageContaining("must not have ATK or DEF");
	}

	@Test
	@DisplayName("A spell without a subtype is rejected")
	void update_spellWithoutSubtype_throwsRulesViolated() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(card.getName(), CardType.SPELL, null,
				TestCards.EFFECT_CODE, null, null, null, null, null, null);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), request))
			.isInstanceOf(CatalogCardRulesViolatedException.class)
			.hasMessageContaining("must have a subtype");
	}

	@Test
	@DisplayName("A hidden card with a spell subtype is rejected")
	void update_hiddenWithSpellSubtype_throwsRulesViolated() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(card.getName(), CardType.HIDDEN, CardSubtype.BURN,
				TestCards.EFFECT_CODE, null, null, null, null, null, null);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), request))
			.isInstanceOf(CatalogCardRulesViolatedException.class)
			.hasMessageContaining("BURN is not valid");
	}

	@Test
	@DisplayName("A creature with a subtype is rejected")
	void update_creatureWithSubtype_throwsRulesViolated() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(card.getName(), CardType.CREATURE, CardSubtype.DAMAGE,
				null, null, 1, 1, null, null, null);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), request))
			.isInstanceOf(CatalogCardRulesViolatedException.class)
			.hasMessageContaining("DAMAGE is not valid");
	}

	@Test
	@DisplayName("A terrain without an effect is rejected")
	void update_terrainWithoutEffect_throwsRulesViolated() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(card.getName(), CardType.TERRAIN, null,
				null, null, null, null, null, null, null);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), request))
			.isInstanceOf(CatalogCardRulesViolatedException.class)
			.hasMessageContaining("must have an effect code");
	}

	@Test
	@DisplayName("A creature without an effect is accepted")
	void update_creatureWithoutEffect_isAccepted() {
		// Arrange
		CatalogCard card = testCards.create(uniqueName("Spell"), CardType.SPELL);

		// Act
		CatalogCardResponse response = catalogCardService.update(card.getId(), creatureRequest(card.getName(), null));

		// Assert
		assertThat(response.type()).isEqualTo(CardType.CREATURE);
		assertThat(response.effectCode()).isNull();
		assertThat(response.subtype()).isNull();
	}

	// --- update di una variante ---

	@Test
	@DisplayName("Game fields sent for a variant are ignored and copied from the original")
	void update_variant_ignoresRequestRulesAndCopiesOriginal() {
		// Arrange
		CatalogCard original = testCards.create(uniqueName("Spell"), CardType.SPELL);
		CatalogCard variant = testCards.createVariantOf(original);
		String newName = uniqueName("Event");
		CatalogCardUpdateRequest request = new CatalogCardUpdateRequest(newName, CardType.CREATURE, null,
				null, original.getId(), 9, 9, 3, "Ignored text", "Event flavor 2");

		// Act
		catalogCardService.update(variant.getId(), request);

		// Assert
		CatalogCard reloaded = reload(variant);
		assertThat(reloaded.getCardType()).isEqualTo(CardType.SPELL);
		assertThat(reloaded.getCardSubtype()).isEqualTo(original.getCardSubtype());
		assertThat(reloaded.getEffectCode()).isEqualTo(original.getEffectCode());
		assertThat(reloaded.getEffectDescription()).isEqualTo(original.getEffectDescription());
		assertThat(reloaded.getAtk()).isNull();
		assertThat(reloaded.getCopyLimit()).isNull();
		assertThat(reloaded.getName()).isEqualTo(newName);
		assertThat(reloaded.getFlavorText()).isEqualTo("Event flavor 2");
	}

	@Test
	@DisplayName("A variant can keep the name of its original")
	void update_variantWithOriginalName_isAccepted() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original, uniqueName("Event"));

		// Act
		CatalogCardResponse response = catalogCardService.update(variant.getId(), creatureRequest(original.getName(), original.getId()));

		// Assert
		assertThat(response.name()).isEqualTo(original.getName());
	}

	@Test
	@DisplayName("A variant cannot take the name of a different original")
	void update_variantWithNameOfOtherOriginal_throwsNameAlreadyUsed() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);
		CatalogCard other = testCards.createCreature();

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(variant.getId(), creatureRequest(other.getName(), original.getId())))
			.isInstanceOf(CatalogCardNameAlreadyUsedException.class);
	}

	// --- variantOfId ---

	@Test
	@DisplayName("An original without variants can become a variant and takes the original's game fields")
	void update_originalBecomesVariant_copiesRules() {
		// Arrange
		CatalogCard original = testCards.create(uniqueName("Terrain"), CardType.TERRAIN);
		CatalogCard card = testCards.createCreature();

		// Act
		CatalogCardResponse response = catalogCardService.update(card.getId(), creatureRequest(uniqueName("Event"), original.getId()));

		// Assert
		CatalogCard reloaded = reload(card);
		assertThat(response.variantOfId()).isEqualTo(original.getId());
		assertThat(reloaded.getVariantOf().getId()).isEqualTo(original.getId());
		assertThat(reloaded.getCardType()).isEqualTo(CardType.TERRAIN);
		assertThat(reloaded.getAtk()).isNull();
	}

	@Test
	@DisplayName("A variant can become an original again, with the game fields of the request")
	void update_variantBecomesOriginal_usesRequestRules() {
		// Arrange
		CatalogCard original = testCards.create(uniqueName("Spell"), CardType.SPELL);
		CatalogCard variant = testCards.createVariantOf(original, uniqueName("Event"));

		// Act
		CatalogCardResponse response = catalogCardService.update(variant.getId(), creatureRequest(variant.getName(), null));

		// Assert
		CatalogCard reloaded = reload(variant);
		assertThat(response.variantOfId()).isNull();
		assertThat(reloaded.isVariant()).isFalse();
		assertThat(reloaded.getCardType()).isEqualTo(CardType.CREATURE);
	}

	@Test
	@DisplayName("A variant that becomes an original with a taken name gets a name conflict, not a database error")
	void update_variantBecomesOriginalWithTakenName_throwsNameAlreadyUsed() {
		// Arrange: la variante ha lo stesso nome dell'originale. Se l'entità cambiasse prima del
		// controllo, l'auto-flush manderebbe l'UPDATE e l'indice univoco darebbe un errore del database.
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(variant.getId(), creatureRequest(original.getName(), null)))
			.isInstanceOf(CatalogCardNameAlreadyUsedException.class);
		assertThat(reload(variant).isVariant()).isTrue();
	}

	@Test
	@DisplayName("A card cannot be a variant of itself")
	void update_variantOfItself_throwsInvalidVariant() {
		// Arrange: id sopra 127, perché fino a 127 Java riusa gli stessi oggetti Integer e un
		// confronto con == sembrerebbe funzionare anche quando è sbagliato
		CatalogCard card = testCards.createCreature();
		while (card.getId() <= 127) {
			card = testCards.createCreature();
		}
		Integer cardId = card.getId();
		String name = card.getName();

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(cardId, creatureRequest(name, cardId)))
			.isInstanceOf(InvalidCardVariantException.class)
			.hasMessageContaining("variant of itself");
	}

	@Test
	@DisplayName("A variant of a missing card is rejected")
	void update_variantOfMissingCard_throwsInvalidVariant() {
		// Arrange
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), creatureRequest(card.getName(), MISSING_ID)))
			.isInstanceOf(InvalidCardVariantException.class)
			.hasMessageContaining("does not exist");
	}

	@Test
	@DisplayName("A variant of another variant is rejected")
	void update_variantOfVariant_throwsInvalidVariant() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);
		CatalogCard card = testCards.createCreature();

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), creatureRequest(card.getName(), variant.getId())))
			.isInstanceOf(InvalidCardVariantException.class)
			.hasMessageContaining("is a variant");
		assertThat(reload(card).isVariant()).isFalse();
	}

	@Test
	@DisplayName("An original with variants cannot become a variant")
	void update_originalWithVariantsBecomesVariant_throwsInvalidVariant() {
		// Arrange
		CatalogCard card = testCards.createCreature();
		testCards.createVariantOf(card);
		CatalogCard otherOriginal = testCards.createCreature();

		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.update(card.getId(), creatureRequest(card.getName(), otherOriginal.getId())))
			.isInstanceOf(InvalidCardVariantException.class)
			.hasMessageContaining("has variants");
	}

	// --- activate e deactivate ---

	@Test
	@DisplayName("Deactivating a card saves it as inactive and leaves its variants active")
	void deactivate_original_onlyThatCardIsInactive() {
		// Arrange
		CatalogCard original = testCards.createCreature();
		CatalogCard variant = testCards.createVariantOf(original);

		// Act
		CatalogCardResponse response = catalogCardService.deactivate(original.getId());

		// Assert
		assertThat(response.active()).isFalse();
		assertThat(reload(original).isActive()).isFalse();
		assertThat(reload(variant).isActive()).isTrue();
	}

	@Test
	@DisplayName("Deactivating twice is accepted and changes nothing")
	void deactivate_alreadyInactive_staysInactive() {
		// Arrange
		CatalogCard card = testCards.deactivate(testCards.createCreature());

		// Act
		CatalogCardResponse response = catalogCardService.deactivate(card.getId());

		// Assert
		assertThat(response.active()).isFalse();
	}

	@Test
	@DisplayName("Activating a deactivated card saves it as active")
	void activate_deactivatedCard_isActive() {
		// Arrange
		CatalogCard card = testCards.deactivate(testCards.createCreature());

		// Act
		CatalogCardResponse response = catalogCardService.activate(card.getId());

		// Assert
		assertThat(response.active()).isTrue();
		assertThat(reload(card).isActive()).isTrue();
	}

	@Test
	@DisplayName("Activating or deactivating a missing card throws CatalogCardNotFoundException")
	void activateAndDeactivate_missingCard_throwNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> catalogCardService.activate(MISSING_ID)).isInstanceOf(CatalogCardNotFoundException.class);
		assertThatThrownBy(() -> catalogCardService.deactivate(MISSING_ID)).isInstanceOf(CatalogCardNotFoundException.class);
	}

	// Creatura valida (ATK 7, DEF 8); variantOfId null = originale
	private CatalogCardUpdateRequest creatureRequest(String name, Integer variantOfId) {
		return new CatalogCardUpdateRequest(name, CardType.CREATURE, null, null, variantOfId, 7, 8, null, null, "Flavor");
	}

	// Rilegge la carta dal database, fuori dalla transazione del service
	private CatalogCard reload(CatalogCard card) {
		return catalogCardRepository.findById(card.getId()).orElseThrow();
	}
}
