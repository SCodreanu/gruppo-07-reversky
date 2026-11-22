package it.downfall.reversky.api.common.storage;

/**
 * File che {@link ImageStorage} non accetta: vuoto, troppo grande o di un tipo non ammesso.
 * <p>
 * Non è una {@code BusinessException}: il codice di errore dipende da cosa si sta caricando, quindi lo sceglie il
 * service che chiama (es. {@code CARD_IMAGE_INVALID} per le carte). Il messaggio è in inglese e diventa il
 * {@code detail} della risposta.
 */
public class InvalidImageException extends RuntimeException {

	public InvalidImageException(String message) {
		super(message);
	}
}
