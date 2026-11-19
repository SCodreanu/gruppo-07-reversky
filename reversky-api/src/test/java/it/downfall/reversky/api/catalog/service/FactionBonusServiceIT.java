package it.downfall.reversky.api.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.catalog.dto.FactionBonusRequest;
import it.downfall.reversky.api.catalog.dto.FactionBonusResponse;
import it.downfall.reversky.api.catalog.dto.FactionBonusSummaryResponse;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.exception.FactionBonusDuplicateException;
import it.downfall.reversky.api.catalog.exception.FactionBonusFactionNotFoundException;
import it.downfall.reversky.api.catalog.exception.FactionBonusRulesViolatedException;
import it.downfall.reversky.api.catalog.exception.FactionBonusSelfMatchupException;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestFactionBonuses;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link FactionBonusService} su un PostgreSQL vero (docs/backoffice, "Dati di riferimento").
 * <p>
 * La matrice è una sola per tutto il database: ogni test la ricorda prima e la rimette dopo
 * (vedi {@link TestFactionBonuses}) e usa solo fazioni create da sé.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class FactionBonusServiceIT {

	@Autowired
	private FactionBonusService factionBonusService;

	@Autowired
	private FactionService factionService;

	// Non serve al test: con la stessa configurazione degli altri IT, Spring riusa il contesto
	// già avviato (e il suo container) invece di crearne uno nuovo.
	@MockitoBean
	private EmailSender emailSender;

	private TestFactionBonuses testFactionBonuses;

	@BeforeEach
	void setUp() {
		testFactionBonuses = new TestFactionBonuses(factionBonusService, factionService);
		testFactionBonuses.rememberMatrix();
	}

	@AfterEach
	void tearDown() {
		testFactionBonuses.restoreMatrix();
	}

	// --- sostituzione valida ---

	@Test
	@DisplayName("A valid matrix is saved and returned with the faction names, sorted by origin and target")
	void replaceAll_validRows_savesAndReturnsSortedWithNames() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		FactionResponse c = testFactionBonuses.createFaction();

		// Act: righe inviate in disordine
		List<FactionBonusResponse> response = factionBonusService.replaceAll(List.of(
			new FactionBonusRequest(b.id(), a.id(), -1, 0),
			new FactionBonusRequest(a.id(), c.id(), 1, 2),
			new FactionBonusRequest(a.id(), b.id(), 2, 1)));

		// Assert: le fazioni create dopo hanno id più grandi, quindi l'ordine atteso è A→B, A→C, B→A
		assertThat(response).containsExactly(
			new FactionBonusResponse(a.id(), a.name(), b.id(), b.name(), 2, 1),
			new FactionBonusResponse(a.id(), a.name(), c.id(), c.name(), 1, 2),
			new FactionBonusResponse(b.id(), b.name(), a.id(), a.name(), -1, 0));
		assertThat(factionBonusService.findAll()).isEqualTo(response);
	}

	@Test
	@DisplayName("A negative ATK bonus (malus) is allowed")
	void replaceAll_negativeAtk_isSaved() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();

		// Act
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), -1, 0)));

		// Assert
		assertThat(factionBonusService.findAllSummaries())
			.containsExactly(new FactionBonusSummaryResponse(a.id(), b.id(), -1, 0));
	}

	@Test
	@DisplayName("Both directions of the same pair are two different rows")
	void replaceAll_bothDirections_savesTwoRows() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();

		// Act
		factionBonusService.replaceAll(List.of(
			new FactionBonusRequest(a.id(), b.id(), 2, 0),
			new FactionBonusRequest(b.id(), a.id(), -1, 1)));

		// Assert
		assertThat(factionBonusService.findAllSummaries()).hasSize(2);
	}

	@Test
	@DisplayName("The new matrix replaces the old one: pairs missing from the request are deleted")
	void replaceAll_existingMatrix_replacesIt() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		FactionResponse c = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(
			new FactionBonusRequest(a.id(), b.id(), 2, 1),
			new FactionBonusRequest(a.id(), c.id(), 1, 0)));

		// Act: A → B cambia valori, A → C sparisce
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), 3, 2)));

		// Assert
		assertThat(factionBonusService.findAllSummaries())
			.containsExactly(new FactionBonusSummaryResponse(a.id(), b.id(), 3, 2));
	}

	@Test
	@DisplayName("An empty list clears the matrix")
	void replaceAll_emptyList_clearsMatrix() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), 2, 1)));

		// Act
		List<FactionBonusResponse> response = factionBonusService.replaceAll(List.of());

		// Assert
		assertThat(response).isEmpty();
		assertThat(factionBonusService.findAll()).isEmpty();
	}

	@Test
	@DisplayName("A null list clears the matrix like an empty one")
	void replaceAll_nullList_clearsMatrix() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), 2, 1)));

		// Act
		factionBonusService.replaceAll(null);

		// Assert
		assertThat(factionBonusService.findAll()).isEmpty();
	}

	// --- righe non valide: la matrice resta com'era ---

	@Test
	@DisplayName("A faction matched against itself is rejected and the matrix does not change")
	void replaceAll_selfMatchup_throwsAndKeepsMatrix() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		List<FactionBonusRequest> before = givenMatrix(a, b);

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(List.of(
				new FactionBonusRequest(b.id(), a.id(), 1, 0),
				new FactionBonusRequest(a.id(), a.id(), 1, 0))))
			.isInstanceOf(FactionBonusSelfMatchupException.class);
		assertThat(testFactionBonuses.currentMatrix()).isEqualTo(before);
	}

	@Test
	@DisplayName("A negative DEF bonus is rejected and the matrix does not change")
	void replaceAll_negativeDef_throwsAndKeepsMatrix() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		List<FactionBonusRequest> before = givenMatrix(a, b);

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(List.of(new FactionBonusRequest(b.id(), a.id(), 1, -1))))
			.isInstanceOf(FactionBonusRulesViolatedException.class);
		assertThat(testFactionBonuses.currentMatrix()).isEqualTo(before);
	}

	@Test
	@DisplayName("An unknown origin faction is rejected and the matrix does not change")
	void replaceAll_unknownOrigin_throwsAndKeepsMatrix() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		List<FactionBonusRequest> before = givenMatrix(a, b);

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(List.of(new FactionBonusRequest(Integer.MAX_VALUE, a.id(), 1, 0))))
			.isInstanceOf(FactionBonusFactionNotFoundException.class);
		assertThat(testFactionBonuses.currentMatrix()).isEqualTo(before);
	}

	@Test
	@DisplayName("An unknown target faction is rejected")
	void replaceAll_unknownTarget_throwsFactionNotFound() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), Integer.MAX_VALUE, 1, 0))))
			.isInstanceOf(FactionBonusFactionNotFoundException.class);
	}

	@Test
	@DisplayName("The same pair twice in the request is rejected and the matrix does not change")
	void replaceAll_duplicatePair_throwsAndKeepsMatrix() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		List<FactionBonusRequest> before = givenMatrix(a, b);

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(List.of(
				new FactionBonusRequest(b.id(), a.id(), 1, 0),
				new FactionBonusRequest(b.id(), a.id(), 2, 1))))
			.isInstanceOf(FactionBonusDuplicateException.class);
		assertThat(testFactionBonuses.currentMatrix()).isEqualTo(before);
	}

	@Test
	@DisplayName("A null row is rejected without a NullPointerException")
	void replaceAll_nullRow_throwsRulesViolated() {
		// Arrange
		List<FactionBonusRequest> rows = new ArrayList<>();
		rows.add(null);

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(rows))
			.isInstanceOf(FactionBonusRulesViolatedException.class);
	}

	@Test
	@DisplayName("A row with a missing field is rejected without a NullPointerException")
	void replaceAll_missingField_throwsRulesViolated() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();

		// Act + Assert
		assertThatThrownBy(() -> factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), null, 0))))
			.isInstanceOf(FactionBonusRulesViolatedException.class);
	}

	// --- concorrenza ---

	@Test
	@DisplayName("Two replacements at the same time: the result is exactly one of the two matrices, never a mix")
	void replaceAll_twoConcurrent_lastOneWinsWithoutMixing() throws Exception {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		FactionResponse c = testFactionBonuses.createFaction();
		List<FactionBonusRequest> first = List.of(
			new FactionBonusRequest(a.id(), b.id(), 1, 0),
			new FactionBonusRequest(a.id(), c.id(), 1, 0));
		List<FactionBonusRequest> second = List.of(
			new FactionBonusRequest(b.id(), c.id(), 2, 1),
			new FactionBonusRequest(c.id(), a.id(), 2, 1));
		CountDownLatch start = new CountDownLatch(1);

		// Act
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> f1 = executor.submit(() -> {
				start.await();
				return factionBonusService.replaceAll(first);
			});
			Future<?> f2 = executor.submit(() -> {
				start.await();
				return factionBonusService.replaceAll(second);
			});
			start.countDown();
			// Nessuna delle due deve fallire: la seconda aspetta la prima
			f1.get(10, TimeUnit.SECONDS);
			f2.get(10, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		// Assert
		assertThat(testFactionBonuses.currentMatrix()).isIn(sorted(first), sorted(second));
	}

	// --- letture ---

	@Test
	@DisplayName("The summaries for GET /rules have the same rows as the admin list, without names")
	void findAllSummaries_matchesAdminList() {
		// Arrange
		FactionResponse a = testFactionBonuses.createFaction();
		FactionResponse b = testFactionBonuses.createFaction();
		factionBonusService.replaceAll(List.of(
			new FactionBonusRequest(a.id(), b.id(), 2, 1),
			new FactionBonusRequest(b.id(), a.id(), -1, 0)));

		// Act
		List<FactionBonusSummaryResponse> summaries = factionBonusService.findAllSummaries();

		// Assert
		assertThat(summaries).containsExactly(
			new FactionBonusSummaryResponse(a.id(), b.id(), 2, 1),
			new FactionBonusSummaryResponse(b.id(), a.id(), -1, 0));
	}

	// --- supporto ---

	/** Imposta una matrice di una riga (A → B) e la restituisce come la legge {@link TestFactionBonuses#currentMatrix()}. */
	private List<FactionBonusRequest> givenMatrix(FactionResponse a, FactionResponse b) {
		factionBonusService.replaceAll(List.of(new FactionBonusRequest(a.id(), b.id(), 2, 1)));
		return testFactionBonuses.currentMatrix();
	}

	/** Le righe nell'ordine di findAllSummaries: origine, poi bersaglio. */
	private static List<FactionBonusRequest> sorted(List<FactionBonusRequest> rows) {
		return rows.stream()
			.sorted((x, y) -> x.originFactionId().equals(y.originFactionId())
				? Integer.compare(x.targetFactionId(), y.targetFactionId())
				: Integer.compare(x.originFactionId(), y.originFactionId()))
			.toList();
	}
}
