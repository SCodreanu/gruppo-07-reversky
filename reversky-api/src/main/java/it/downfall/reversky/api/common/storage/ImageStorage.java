package it.downfall.reversky.api.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Immagini caricate dal backoffice, salvate in una cartella e servite in pubblico su {@code /images/**}
 * (docs/backoffice, "Illustrazioni", deciso il 22 novembre con API-14).
 * <p>
 * Il percorso salvato nel database (es. {@code image_path} delle carte) è relativo alla cartella, come
 * {@code cards/3f2a….png}: ogni caricamento ha un nome nuovo, così un'immagine sostituita non resta nella cache del
 * browser. Il tipo si riconosce dai primi byte del file, mai dal nome o dal {@code Content-Type} del client.
 * <p>
 * Il file system non partecipa alle transazioni: {@link #saveInTransaction} cancella il file nuovo se la transazione
 * fallisce, {@link #deleteAfterCommit} cancella quello vecchio solo dopo il commit. Così il database non indica mai un
 * file che non esiste.
 */
@Slf4j
@Component
public class ImageStorage {

	/** Prefisso pubblico delle immagini: l'URL di un'immagine è {@code URL_PREFIX + percorso}. */
	public static final String URL_PREFIX = "/images/";

	private static final byte[] PNG ={(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
	private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
	private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

	private final Path root;
	private final long maxImageBytes;

	public ImageStorage(ImageStorageProperties properties) {
		this.root = properties.imagesDir().toAbsolutePath().normalize();
		this.maxImageBytes = properties.maxImageSize().toBytes();
		try {
			Files.createDirectories(root);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the images folder " + root, e);
		}
	}

	/**
	 * URL pubblico di un'immagine su questo server, da restituire al client insieme al percorso.
	 *
	 * @param path percorso relativo salvato nel database, es. {@code cards/<uuid>.png}
	 * @return es. {@code /images/cards/<uuid>.png}; null se il percorso è null
	 */
	public static String publicUrl(String path) {
		return path == null ? null : URL_PREFIX + path;
	}

	/** Cartella delle immagini, assoluta: la usa anche la configurazione che le serve su {@code /images/**}. */
	public Path getRoot() {
		return root;
	}

	/**
	 * Controlla e salva un'immagine dentro la transazione in corso: se la transazione non arriva al commit, il file
	 * appena scritto si cancella.
	 *
	 * @param folder  sottocartella, es. {@code cards}
	 * @param content contenuto del file
	 * @return il percorso relativo da salvare nel database, es. {@code cards/<uuid>.png}
	 * @throws InvalidImageException se il file è vuoto, troppo grande o non è PNG, JPEG o WebP
	 * @throws IllegalStateException se non c'è una transazione attiva
	 */
	public String saveInTransaction(String folder, byte[] content) {
		requireTransaction();
		String extension = validate(content);
		String path = folder + "/" + UUID.randomUUID() + extension;
		write(path, content);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					delete(path);
				}
			}
		});
		return path;
	}

	/**
	 * Cancella un'immagine dopo il commit della transazione in corso, es. quella sostituita da una nuova. Un percorso
	 * senza file (le immagini segnaposto delle migrazioni) non è un errore.
	 *
	 * @throws IllegalStateException se non c'è una transazione attiva
	 */
	public void deleteAfterCommit(String path) {
		requireTransaction();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				delete(path);
			}
		});
	}

	// Estensione del file dal suo contenuto; prima i controlli che non leggono i byte
	private String validate(byte[] content) {
		if (content == null || content.length == 0) {
			throw new InvalidImageException("The image is empty");
		}
		if (content.length > maxImageBytes) {
			throw new InvalidImageException("The image must not exceed " + maxImageBytes / (1024 * 1024) + " MB");
		}
		if (startsWith(content, 0, PNG)) {
			return ".png";
		}
		if (startsWith(content, 0, JPEG)) {
			return ".jpg";
		}
		if (startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP)) {
			return ".webp";
		}
		throw new InvalidImageException("The image must be a PNG, JPEG or WebP file");
	}

	private static boolean startsWith(byte[] content, int offset, byte[] signature) {
		return content.length >= offset + signature.length
			&& Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
	}

	private void write(String path, byte[] content) {
		Path file = resolve(path);
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, content);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot save the image " + path, e);
		}
	}

	// Mai un errore: un file rimasto si può togliere a mano, un'operazione già riuscita non deve fallire per questo
	private void delete(String path) {
		try {
			Files.deleteIfExists(resolve(path));
		} catch (IOException | IllegalArgumentException e) {
			log.warn("Image {} not deleted: {}", path, e.getMessage());
		}
	}

	// Il percorso deve restare dentro la cartella delle immagini, anche se nel database ci fosse un "../"
	private Path resolve(String path) {
		Path file = root.resolve(path).normalize();
		if (!file.startsWith(root)) {
			throw new IllegalArgumentException("Image path outside the images folder: " + path);
		}
		return file;
	}

	private static void requireTransaction() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("Images are saved and deleted only inside a transaction");
		}
	}
}
