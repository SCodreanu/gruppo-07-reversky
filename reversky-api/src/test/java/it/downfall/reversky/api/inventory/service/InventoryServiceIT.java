package it.downfall.reversky.api.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.inventory.entity.CreditReason;
import it.downfall.reversky.api.inventory.entity.CreditTransaction;
import it.downfall.reversky.api.inventory.exception.InsufficientCreditsException;
import it.downfall.reversky.api.inventory.exception.InventoryNotFoundException;
import it.downfall.reversky.api.inventory.repository.CreditTransactionRepository;
import it.downfall.reversky.api.ruleset.service.RuleSetService;
import it.downfall.reversky.api.support.TestUsers;
import it.downfall.reversky.api.user.entity.User;

/**
 * {@link InventoryService} su un PostgreSQL vero (docs/crediti).
 * <p>
 * La classe non è {@code @Transactional}: ogni chiamata fa il commit come in produzione, altrimenti il test delle
 * spese contemporanee non avrebbe senso. Ogni test crea un proprio utente, che parte dai crediti di benvenuto.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class InventoryServiceIT {

	private static final int MISSING_USER_ID = Integer.MAX_VALUE;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private CreditTransactionRepository creditTransactionRepository;

	@Autowired
	private RuleSetService ruleSetService;

	@Autowired
	private TestUsers testUsers;

	// Non serve al test: con la stessa configurazione degli altri IT, Spring riusa il contesto già avviato
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("A new inventory starts with the signup bonus of the active rule set and its SIGNUP row")
	void createInventory_newUser_givesSignupBonusAndWritesSignupRow() {
		// Arrange
		int signupBonus = ruleSetService.getActiveRuleSet().getSignupBonus();

		// Act: TestUsers.create() chiama createInventory come la registrazione
		User user = testUsers.create();

		// Assert
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(signupBonus);
		assertThat(transactionsOf(user))
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.containsExactly(tuple(signupBonus, CreditReason.SIGNUP));
	}

	@Test
	@DisplayName("Adding credits raises the balance and writes a positive row")
	void addCredits_positiveAmount_raisesBalanceAndWritesRow() {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act
		int after = inventoryService.addCredits(user.getId(), 30, CreditReason.BATTLE_WIN);

		// Assert
		assertThat(after).isEqualTo(before + 30);
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(before + 30);
		assertThat(transactionsOf(user)).last()
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.containsExactly(30, CreditReason.BATTLE_WIN);
	}

	@Test
	@DisplayName("Spending credits lowers the balance and writes a negative row")
	void spendCredits_enoughCredits_lowersBalanceAndWritesNegativeRow() {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act
		int after = inventoryService.spendCredits(user.getId(), 200, CreditReason.PACK_PURCHASE);

		// Assert
		assertThat(after).isEqualTo(before - 200);
		assertThat(transactionsOf(user)).last()
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.containsExactly(-200, CreditReason.PACK_PURCHASE);
	}

	@Test
	@DisplayName("Spending exactly the whole balance leaves zero credits")
	void spendCredits_wholeBalance_leavesZero() {
		// Arrange
		User user = testUsers.create();
		int balance = inventoryService.getCredits(user.getId());

		// Act
		int after = inventoryService.spendCredits(user.getId(), balance, CreditReason.PACK_PURCHASE);

		// Assert
		assertThat(after).isZero();
	}

	@Test
	@DisplayName("A free purchase still writes a PACK_PURCHASE row with amount 0")
	void spendCredits_zero_writesZeroRow() {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act
		inventoryService.spendCredits(user.getId(), 0, CreditReason.PACK_PURCHASE);

		// Assert
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(before);
		assertThat(transactionsOf(user)).last()
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.containsExactly(0, CreditReason.PACK_PURCHASE);
	}

	@Test
	@DisplayName("Spending one credit more than the balance is rejected and changes nothing")
	void spendCredits_oneMoreThanBalance_throwsAndChangesNothing() {
		// Arrange
		User user = testUsers.create();
		int balance = inventoryService.getCredits(user.getId());
		int rowsBefore = transactionsOf(user).size();

		// Act + Assert
		assertThatThrownBy(() -> inventoryService.spendCredits(user.getId(), balance + 1, CreditReason.PACK_PURCHASE))
			.isInstanceOf(InsufficientCreditsException.class)
			.hasMessageContaining((balance + 1) + " required, " + balance + " available");
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(balance);
		assertThat(transactionsOf(user)).hasSize(rowsBefore);
	}

	@Test
	@DisplayName("A negative amount is a programming error, for both add and spend")
	void addAndSpend_negativeAmount_throwIllegalArgument() {
		// Arrange
		User user = testUsers.create();

		// Act + Assert
		assertThatThrownBy(() -> inventoryService.addCredits(user.getId(), -1, CreditReason.BATTLE_WIN))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> inventoryService.spendCredits(user.getId(), -1, CreditReason.PACK_PURCHASE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("A user without an inventory gets INVENTORY_NOT_FOUND everywhere")
	void allMethods_missingInventory_throwInventoryNotFound() {
		// Act + Assert
		assertThatThrownBy(() -> inventoryService.getCredits(MISSING_USER_ID)).isInstanceOf(InventoryNotFoundException.class);
		assertThatThrownBy(() -> inventoryService.getInventory(MISSING_USER_ID)).isInstanceOf(InventoryNotFoundException.class);
		assertThatThrownBy(() -> inventoryService.addCredits(MISSING_USER_ID, 1, CreditReason.BATTLE_WIN))
			.isInstanceOf(InventoryNotFoundException.class);
		assertThatThrownBy(() -> inventoryService.spendCredits(MISSING_USER_ID, 1, CreditReason.PACK_PURCHASE))
			.isInstanceOf(InventoryNotFoundException.class);
		assertThatThrownBy(() -> inventoryService.lockInventories(List.of(MISSING_USER_ID)))
			.isInstanceOf(InventoryNotFoundException.class);
	}

	@Test
	@DisplayName("Locking several inventories, with duplicates and in any order, succeeds")
	void lockInventories_unorderedWithDuplicates_succeeds() {
		// Arrange
		User first = testUsers.create();
		User second = testUsers.create();

		// Act + Assert: nessuna eccezione, e il saldo non cambia
		int before = inventoryService.getCredits(first.getId());
		inventoryService.lockInventories(List.of(second.getId(), first.getId(), second.getId()));
		assertThat(inventoryService.getCredits(first.getId())).isEqualTo(before);
	}

	@Test
	@DisplayName("An admin correction adds or removes credits with ADMIN_ADJUSTMENT rows")
	void adjustCredits_positiveThenNegative_writesAdminRows() {
		// Arrange
		User user = testUsers.create();
		int before = inventoryService.getCredits(user.getId());

		// Act
		inventoryService.adjustCredits(user.getId(), 100, "Compensation");
		int after = inventoryService.adjustCredits(user.getId(), -40, "Correction").credits();

		// Assert
		assertThat(after).isEqualTo(before + 60);
		assertThat(transactionsOf(user))
			.extracting(CreditTransaction::getAmount, CreditTransaction::getReason)
			.endsWith(tuple(100, CreditReason.ADMIN_ADJUSTMENT), tuple(-40, CreditReason.ADMIN_ADJUSTMENT));
	}

	@Test
	@DisplayName("An admin correction cannot bring the balance below zero")
	void adjustCredits_belowZero_throwsAndChangesNothing() {
		// Arrange
		User user = testUsers.create();
		int balance = inventoryService.getCredits(user.getId());

		// Act + Assert
		assertThatThrownBy(() -> inventoryService.adjustCredits(user.getId(), -(balance + 1), "Too much"))
			.isInstanceOf(InsufficientCreditsException.class);
		assertThat(inventoryService.getCredits(user.getId())).isEqualTo(balance);
	}

	@Test
	@DisplayName("Concurrent spends never overdraw: only as many succeed as the balance allows")
	void spendCredits_concurrentRequests_neverOverdraw() throws Exception {
		// Arrange: con il saldo di benvenuto, al più balance / price spese possono riuscire
		User user = testUsers.create();
		int balance = inventoryService.getCredits(user.getId());
		int price = balance / 4;
		int threads = 8;
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<Boolean>> results = new ArrayList<>();

		// Act
		try {
			for (int i = 0; i < threads; i++) {
				results.add(executor.submit(() -> {
					start.await();
					try {
						inventoryService.spendCredits(user.getId(), price, CreditReason.PACK_PURCHASE);
						return true;
					} catch (InsufficientCreditsException e) {
						return false;
					}
				}));
			}
			start.countDown();

			int succeeded = 0;
			for (Future<Boolean> result : results) {
				if (result.get()) {
					succeeded++;
				}
			}

			// Assert
			assertThat(succeeded).isEqualTo(balance / price);
			assertThat(inventoryService.getCredits(user.getId())).isEqualTo(balance - succeeded * price);
			assertThat(transactionsOf(user)).hasSize(1 + succeeded);
		} finally {
			executor.shutdownNow();
		}
	}

	private List<CreditTransaction> transactionsOf(User user) {
		return creditTransactionRepository.findAllByUserIdOrderByIdAsc(user.getId());
	}
}
