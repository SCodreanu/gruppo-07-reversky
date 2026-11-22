package it.downfall.reversky.api.common.exception;

import lombok.Getter;

/**
 * Errore di business previsto (credenziali errate, email già usata, token scaduto...).
 * <p>
 * Ogni errore concreto è una sottoclasse specifica. Il service la lancia senza sapere nulla di HTTP:
 * è {@link GlobalExceptionHandler} a trasformarla in una risposta {@code ProblemDetail}
 * con lo status corrispondente alla {@link Category}.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

	/**
	 * Tipo di errore. Lo status HTTP indicato nei commenti lo applica solo GlobalExceptionHandler.
	 */
	public enum Category {
		INVALID_REQUEST,    // 400 dati non validi
		UNAUTHENTICATED,    // 401 non autenticato / token non valido
		FORBIDDEN,          // 403 autenticato ma senza permesso
		NOT_FOUND,          // 404 inesistente o non visibile all'utente
		CONFLICT,           // 409 stato non compatibile
		RULE_VIOLATION,     // 422 regola di gioco violata
		SERVICE_UNAVAILABLE // 503 engine non raggiungibile
	}

	private final Category category;
	private final String code;
	private final String title;

	/**
	 * @param detail messaggio leggibile in inglese: diventa il campo {@code detail} della risposta
	 */
	protected BusinessException(Category category, String code, String title, String detail) {

		super(detail);

		this.category = category;
		this.code = code;
		this.title = title;
	}
}
