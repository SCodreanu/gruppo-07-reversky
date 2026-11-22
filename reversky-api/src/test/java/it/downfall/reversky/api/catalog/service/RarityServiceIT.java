package it.downfall.reversky.api.catalog.service;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.RarityCreateRequest;
import it.downfall.reversky.api.catalog.dto.RarityResponse;
import it.downfall.reversky.api.catalog.dto.RarityUpdateRequest;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.exception.RarityNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.RarityNotFoundException;
import it.downfall.reversky.api.catalog.exception.RaritySortingOrderAlreadyUsedException;
import it.downfall.reversky.api.catalog.repository.RarityRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link RarityService} su un PostgreSQL vero (docs/backoffice, "Dati di riferimento").
 * <p>
 * Le rarità non si cancellano, quindi restano nel database tra un test e l'altro: ogni test
 * usa nomi e sortingOrder casuali e controlla solo le proprie rarità.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class RarityServiceIT {

	@Autowired
	private RarityService rarityService;

	@Autowired
	private RarityRepository rarityRepository;

	// Non serve al test: con la stessa configurazione di AuthServiceIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	// --- create ---

	@Test
	@DisplayName("A valid request creates the rarity and returns it with its id")
	void create_validRequest_returnsSavedRarity() {
		// Arrange
		String name = uniqueName();
		int sortingOrder = uniqueSortingOrder();

		// Act
		RarityResponse response = rarityService.create(new RarityCreateRequest(name, 2, 5, sortingOrder));

		// Assert
		assertThat(response.id()).isNotNull();
		assertThat(response.name()).isEqualTo(name);
		assertThat(response.deckLimit()).isEqualTo(2);
		assertThat(response.maxPerDeck()).isEqualTo(5);
		assertThat(response.sortingOrder()).isEqualTo(sortingOrder);
		assertThat(rarityRepository.findById(response.id())).isPresent();
	}

	@Test
	@DisplayName("An empty max per deck is saved as null: no limit")
	void create_withoutMaxPerDeck_savesNull() {
		// Act
		RarityResponse response = rarityService.create(new RarityCreateRequest(uniqueName(), 1, null, uniqueSortingOrder()));

		// Assert
		Rarity saved = rarityRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getMaxPerDeck()).isNull();
	}

	@Test
	@DisplayName("A name already used by another rarity is rejected")
	void create_nameAlreadyUsed_throwsNameAlreadyUsed() {
		// Arrange
		String name = uniqueName();
		rarityService.create(createRequest(name, uniqueSortingOrder()));

		// Act + Assert
		assertThatThrownBy(() -> rarityService.create(createRequest(name, uniqueSortingOrder())))
			.isInstanceOf(RarityNameAlreadyUsedException.class);
	}

	@Test
	@DisplayName("A sorting order already used by another rarity is rejected")
	void create_sortingOrderAlreadyUsed_throwsSortingOrderAlreadyUsed() {
		// Arrange
		int sortingOrder = uniqueSortingOrder();
		rarityService.create(createRequest(uniqueName(), sortingOrder));

		// Act + Assert
		assertThatThrownBy(() -> rarityService.create(createRequest(uniqueName(), sortingOrder)))
			.isInstanceOf(RaritySortingOrderAlreadyUsedException.class);
	}

	@Test
	@DisplayName("When both name and sorting order are taken, the name is reported")
	void create_nameAndSortingOrderTaken_throwsNameAlreadyUsed() {
		// Arrange
		RarityResponse existing = rarityService.create(createRequest(uniqueName(), uniqueSortingOrder()));

		// Act + Assert
		assertThatThrownBy(() -> rarityService.create(createRequest(existing.name(), existing.sortingOrder())))
			.isInstanceOf(RarityNameAlreadyUsedException.class);
	}

	// --- findAll ---

	@Test
	@DisplayName("The list is sorted by sorting order, not by creation order")
	void findAll_createdInReverseOrder_returnsThemBySortingOrder() {
		// Arrange: la rarità più rara viene creata per prima, quindi ha l'id più basso
		int lowerOrder = uniqueSortingOrder();
		RarityResponse rarer = rarityService.create(createRequest(uniqueName(), lowerOrder + 1));
		RarityResponse commoner = rarityService.create(createRequest(uniqueName(), lowerOrder));

		// Act
		List<RarityResponse> rarities = rarityService.findAll();

		// Assert
		assertThat(rarities).isSortedAccordingTo(Comparator.comparing(RarityResponse::sortingOrder));
		assertThat(rarities.indexOf(commoner)).isLessThan(rarities.indexOf(rarer));
	}

	// --- update ---

	@Test
	@DisplayName("An update changes name and limits, and is saved")
	void update_existingRarity_changesFields() {
		// Arrange
		RarityResponse created = rarityService.create(createRequest(uniqueName(), uniqueSortingOrder()));
		String newName = uniqueName();

		// Act
		RarityResponse response = rarityService.update(created.id(), new RarityUpdateRequest(newName, 1, 4));

		// Assert
		assertThat(response.name()).isEqualTo(newName);
		Rarity saved = rarityRepository.findById(created.id()).orElseThrow();
		assertThat(saved.getName()).isEqualTo(newName);
		assertThat(saved.getDeckLimit()).isEqualTo(1);
		assertThat(saved.getMaxPerDeck()).isEqualTo(4);
	}

	@Test
	@DisplayName("An update never changes the sorting order")
	void update_existingRarity_keepsSortingOrder() {
		// Arrange
		int sortingOrder = uniqueSortingOrder();
		RarityResponse created = rarityService.create(createRequest(uniqueName(), sortingOrder));

		// Act
		RarityResponse response = rarityService.update(created.id(), updateRequest(uniqueName()));

		// Assert
		assertThat(response.sortingOrder()).isEqualTo(sortingOrder);
		assertThat(rarityRepository.findById(created.id()).orElseThrow().getSortingOrder()).isEqualTo(sortingOrder);
	}

	@Test
	@DisplayName("An update that keeps the same name is not a conflict with itself")
	void update_sameName_keepsTheName() {
		// Arrange
		String name = uniqueName();
		RarityResponse created = rarityService.create(createRequest(name, uniqueSortingOrder()));

		// Act
		RarityResponse response = rarityService.update(created.id(), new RarityUpdateRequest(name, 3, null));

		// Assert
		assertThat(response.name()).isEqualTo(name);
		assertThat(response.deckLimit()).isEqualTo(3);
	}

	@Test
	@DisplayName("An update cannot take the name of another rarity")
	void update_nameOfAnotherRarity_throwsNameAlreadyUsed() {
		// Arrange
		String takenName = uniqueName();
		rarityService.create(createRequest(takenName, uniqueSortingOrder()));
		RarityResponse other = rarityService.create(createRequest(uniqueName(), uniqueSortingOrder()));

		// Act + Assert
		assertThatThrownBy(() -> rarityService.update(other.id(), updateRequest(takenName)))
			.isInstanceOf(RarityNameAlreadyUsedException.class);
	}

	@Test
	@DisplayName("Updating a rarity that does not exist is rejected")
	void update_unknownId_throwsNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> rarityService.update(Integer.MAX_VALUE, updateRequest(uniqueName())))
			.isInstanceOf(RarityNotFoundException.class);
	}

	@Test
	@DisplayName("A missing rarity is reported before a taken name")
	void update_unknownIdAndTakenName_throwsNotFound() {
		// Arrange
		String takenName = uniqueName();
		rarityService.create(createRequest(takenName, uniqueSortingOrder()));

		// Act + Assert
		assertThatThrownBy(() -> rarityService.update(Integer.MAX_VALUE, updateRequest(takenName)))
			.isInstanceOf(RarityNotFoundException.class);
	}

	// --- supporto ---

	private static String uniqueName() {
		return "Rarity " + uniqueSuffix();
	}

	// Valori alti e casuali: non si scontrano con le rarità vere (1-6) né, in pratica, tra loro.
	// Il +1 di findAll resta comunque sotto Integer.MAX_VALUE.
	private static int uniqueSortingOrder() {
		return ThreadLocalRandom.current().nextInt(1_000, 1_000_000_000);
	}

	private static RarityCreateRequest createRequest(String name, int sortingOrder) {
		return new RarityCreateRequest(name, 2, null, sortingOrder);
	}

	private static RarityUpdateRequest updateRequest(String name) {
		return new RarityUpdateRequest(name, 2, null);
	}
}
