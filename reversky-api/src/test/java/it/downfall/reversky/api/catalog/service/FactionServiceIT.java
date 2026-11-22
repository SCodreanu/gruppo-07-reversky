package it.downfall.reversky.api.catalog.service;

import static it.downfall.reversky.api.support.TestUsers.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.exception.FactionNameAlreadyUsedException;
import it.downfall.reversky.api.catalog.exception.FactionNotFoundException;
import it.downfall.reversky.api.catalog.repository.FactionRepository;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link FactionService} su un PostgreSQL vero (docs/backoffice, "Dati di riferimento").
 * <p>
 * Le fazioni non si cancellano, quindi restano nel database tra un test e l'altro: ogni test
 * usa nomi casuali e controlla solo le proprie fazioni.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class FactionServiceIT {

	@Autowired
	private FactionService factionService;

	@Autowired
	private FactionRepository factionRepository;

	// Non serve al test: con la stessa configurazione di AuthServiceIT, Spring riusa
	// il contesto già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("A valid request creates the faction and returns it with its id")
	void create_validRequest_returnsSavedFaction() {
		// Arrange
		String name = uniqueName();

		// Act
		FactionResponse response = factionService.create(new FactionRequest(name, "factions/test.svg", "A test faction"));

		// Assert
		assertThat(response.id()).isNotNull();
		assertThat(response.name()).isEqualTo(name);
		assertThat(response.emblemPath()).isEqualTo("factions/test.svg");
		assertThat(response.description()).isEqualTo("A test faction");
		assertThat(factionRepository.findById(response.id())).isPresent();
	}

	@Test
	@DisplayName("The description is optional")
	void create_withoutDescription_savesNullDescription() {
		// Act
		FactionResponse response = factionService.create(new FactionRequest(uniqueName(), "factions/test.svg", null));

		// Assert
		assertThat(response.description()).isNull();
	}

	@Test
	@DisplayName("A name already used by another faction is rejected")
	void create_nameAlreadyUsed_throwsNameAlreadyUsed() {
		// Arrange
		String name = uniqueName();
		factionService.create(request(name));

		// Act + Assert
		assertThatThrownBy(() -> factionService.create(request(name)))
			.isInstanceOf(FactionNameAlreadyUsedException.class);
	}

	@Test
	@DisplayName("The list contains every faction, sorted by id")
	void findAll_afterCreatingTwo_returnsThemInIdOrder() {
		// Arrange
		FactionResponse first = factionService.create(request(uniqueName()));
		FactionResponse second = factionService.create(request(uniqueName()));

		// Act
		List<FactionResponse> factions = factionService.findAll();

		// Assert
		assertThat(factions).isSortedAccordingTo(Comparator.comparing(FactionResponse::id));
		assertThat(factions.indexOf(first)).isLessThan(factions.indexOf(second));
	}

	@Test
	@DisplayName("An update changes name, emblem and description, and is saved")
	void update_existingFaction_changesAllFields() {
		// Arrange
		FactionResponse created = factionService.create(request(uniqueName()));
		String newName = uniqueName();

		// Act
		FactionResponse response = factionService.update(created.id(), new FactionRequest(newName, "factions/new.svg", "New text"));

		// Assert
		assertThat(response.name()).isEqualTo(newName);
		Faction saved = factionRepository.findById(created.id()).orElseThrow();
		assertThat(saved.getName()).isEqualTo(newName);
		assertThat(saved.getEmblemPath()).isEqualTo("factions/new.svg");
		assertThat(saved.getDescription()).isEqualTo("New text");
	}

	@Test
	@DisplayName("An update that keeps the same name is not a conflict with itself")
	void update_sameName_keepsTheName() {
		// Arrange
		String name = uniqueName();
		FactionResponse created = factionService.create(request(name));

		// Act
		FactionResponse response = factionService.update(created.id(), new FactionRequest(name, "factions/other.svg", "Only the text changes"));

		// Assert
		assertThat(response.name()).isEqualTo(name);
		assertThat(response.description()).isEqualTo("Only the text changes");
	}

	@Test
	@DisplayName("An update cannot take the name of another faction")
	void update_nameOfAnotherFaction_throwsNameAlreadyUsed() {
		// Arrange
		String takenName = uniqueName();
		factionService.create(request(takenName));
		FactionResponse other = factionService.create(request(uniqueName()));

		// Act + Assert
		assertThatThrownBy(() -> factionService.update(other.id(), request(takenName)))
			.isInstanceOf(FactionNameAlreadyUsedException.class);
	}

	@Test
	@DisplayName("Updating a faction that does not exist is rejected")
	void update_unknownId_throwsNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> factionService.update(Integer.MAX_VALUE, request(uniqueName())))
			.isInstanceOf(FactionNotFoundException.class);
	}

	@Test
	@DisplayName("A missing faction is reported before a taken name")
	void update_unknownIdAndTakenName_throwsNotFound() {
		// Arrange
		String takenName = uniqueName();
		factionService.create(request(takenName));

		// Act + Assert
		assertThatThrownBy(() -> factionService.update(Integer.MAX_VALUE, request(takenName)))
			.isInstanceOf(FactionNotFoundException.class);
	}

	private static String uniqueName() {
		return "Faction " + uniqueSuffix();
	}

	private static FactionRequest request(String name) {
		return new FactionRequest(name, "factions/test.svg", "A test faction");
	}
}
