package it.downfall.reversky.api.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import it.downfall.reversky.api.TestcontainersConfiguration;
import it.downfall.reversky.api.common.mail.EmailSender;
import it.downfall.reversky.api.support.TestUsers;

/**
 * {@link ImageStorage} con transazioni vere (API-14): il file system non partecipa alle transazioni, quindi un file
 * nuovo si toglie se la transazione fallisce e uno vecchio si toglie solo dopo il commit.
 */
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class ImageStorageIT {

	private static final byte[] PNG = Arrays.copyOf(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 32);

	@Autowired
	private ImageStorage imageStorage;

	@Autowired
	private TransactionTemplate transactionTemplate;

	// Stessa configurazione degli altri IT di servizio, così Spring riusa il contesto
	@MockitoBean
	private EmailSender emailSender;

	@Test
	@DisplayName("A saved image stays after the commit and is deleted when the transaction rolls back")
	void saveInTransaction_keptOnCommit_deletedOnRollback() {
		// Act
		String committed = transactionTemplate.execute(status -> imageStorage.saveInTransaction("cards", PNG));
		String rolledBack = transactionTemplate.execute(status -> {
			String path = imageStorage.saveInTransaction("cards", PNG);
			assertThat(file(path)).exists();
			status.setRollbackOnly();
			return path;
		});

		// Assert
		assertThat(file(committed)).exists().hasBinaryContent(PNG);
		assertThat(file(rolledBack)).doesNotExist();
	}

	@Test
	@DisplayName("An old image is deleted only if the transaction commits")
	void deleteAfterCommit_onlyOnCommit() {
		// Arrange
		String kept = transactionTemplate.execute(status -> imageStorage.saveInTransaction("cards", PNG));
		String deleted = transactionTemplate.execute(status -> imageStorage.saveInTransaction("cards", PNG));

		// Act
		transactionTemplate.executeWithoutResult(status -> {
			imageStorage.deleteAfterCommit(kept);
			status.setRollbackOnly();
		});
		transactionTemplate.executeWithoutResult(status -> imageStorage.deleteAfterCommit(deleted));

		// Assert
		assertThat(file(kept)).exists();
		assertThat(file(deleted)).doesNotExist();
	}

	@Test
	@DisplayName("A missing file or a path outside the folder is not an error, and nothing outside is touched")
	void deleteAfterCommit_missingOrOutside_isIgnored() throws Exception {
		// Arrange: un file fuori dalla cartella delle immagini, con lo stesso nome che userebbe un "../"
		Path outside = imageStorage.getRoot().resolveSibling("outside-" + System.nanoTime() + ".png");
		Files.write(outside, PNG);

		try {
			// Act
			transactionTemplate.executeWithoutResult(status -> {
				imageStorage.deleteAfterCommit("cards/placeholder-without-file.webp");
				imageStorage.deleteAfterCommit("../" + outside.getFileName());
			});

			// Assert
			assertThat(outside).exists();
		} finally {
			Files.deleteIfExists(outside);
		}
	}

	@Test
	@DisplayName("Outside a transaction saving or deleting is a programming error")
	void withoutTransaction_isRejected() {
		// Act + Assert
		assertThatThrownBy(() -> imageStorage.saveInTransaction("cards", PNG)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> imageStorage.deleteAfterCommit("cards/x.png")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("The public URL is /images/ plus the stored path, null for no path")
	void publicUrl_prefixesPath() {
		// Act + Assert
		assertThat(ImageStorage.publicUrl("cards/a.png")).isEqualTo("/images/cards/a.png");
		assertThat(ImageStorage.publicUrl(null)).isNull();
	}

	private Path file(String path) {
		return imageStorage.getRoot().resolve(path);
	}
}
