package it.downfall.reversky.api.common.exception;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Trasforma ogni eccezione in una risposta {@code ProblemDetail} (RFC 9457) con il campo {@code code}
 * (docs/api-rest). Unico punto in cui un errore diventa uno status HTTP.
 * <p>
 * Estende {@link ResponseEntityExceptionHandler}: così anche gli errori di Spring MVC
 * (JSON malformato, metodo non supportato, 404 di routing...) hanno già il formato ProblemDetail.
 * <p>
 * Anche i 401 e i 403 di Spring Security arrivano qui: SecurityConfig li inoltra a questa classe.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** Un errore su un singolo campo, per la lista {@code errors} della risposta. */
	public record FieldViolation(String field, String message) {
	}

	/**
	 * Errori di business: lo status dipende dalla categoria dell'eccezione.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest request) {
		HttpStatus status = toStatus(ex.getCategory());

		ProblemDetail problem = createProblem(status, ex.getTitle(), ex.getMessage(), ex.getCode(), request.getRequestURI());

		return ResponseEntity.status(status).body(problem);
	}

	/**
	 * Richiesta senza token, con token scaduto o non valido: 401.
	 * Arriva da Spring Security tramite SecurityConfig.
	 * <p>
	 * Il messaggio non dice perché il token non è valido: in ogni caso il frontend prova il rinnovo
	 * e, se fallisce, torna al login.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		ProblemDetail problem = createProblem(HttpStatus.UNAUTHORIZED, "Authentication required", "Please log in to continue", "AUTH_REQUIRED", request.getRequestURI());

		// Lo standard HTTP richiede l'header WWW-Authenticate con ogni 401
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").body(problem);
	}

	/**
	 * Utente autenticato ma senza il ruolo necessario (es. USER su /admin/**): 403.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		ProblemDetail problem = createProblem(HttpStatus.FORBIDDEN, "Access denied", "You do not have permission to perform this operation", "ACCESS_DENIED", request.getRequestURI());

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}

	/**
	 * Validazione dei DTO fallita (@Valid): 400 con l'elenco dei campi non validi.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
			.toList();

		return validationProblem(errors, request);
	}

	/**
	 * Validazione dei parametri fallita con la validazione dei metodi di Spring: succede quando il corpo
	 * è una lista ({@code List<@NotNull @Valid XxxRequest>}, es. PUT /admin/faction-bonuses) o quando un
	 * parametro ha un vincolo diretto. Stessa risposta di {@link #handleMethodArgumentNotValid}:
	 * {@code 400 VALIDATION_FAILED} con {@code errors}.
	 * <p>
	 * Per un elemento di una lista il campo comincia con l'indice: {@code [2].defBonus} per un campo
	 * della terza riga, {@code [2]} per una riga {@code null}. Per un parametro semplice è il suo nome.
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = new ArrayList<>();

		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			String index = result.getContainerIndex() != null ? "[" + result.getContainerIndex() + "]" : null;

			if (result instanceof ParameterErrors parameterErrors) {
				// Errori sui campi di un oggetto (DTO), anche dentro una lista
				for (FieldError error : parameterErrors.getFieldErrors()) {
					String field = index != null ? index + "." + error.getField() : error.getField();
					errors.add(new FieldViolation(field, error.getDefaultMessage()));
				}
			} else {
				// Errori sul valore stesso: un elemento null di una lista o un parametro con un vincolo
				String field = index != null ? index : result.getMethodParameter().getParameterName();
				for (MessageSourceResolvable error : result.getResolvableErrors()) {
					errors.add(new FieldViolation(field, error.getDefaultMessage()));
				}
			}
		}

		return validationProblem(errors, request);
	}

	/**
	 * Errori inattesi: 500 con un messaggio generico. Lo stack trace va solo nel log, mai al client.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {}", request.getRequestURI(), ex);

		ProblemDetail problem = createProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred. Please try again later.", "INTERNAL_ERROR", request.getRequestURI());

		return ResponseEntity.internalServerError().body(problem);
	}

	// Tabella degli status in docs/api-rest
	private HttpStatus toStatus(BusinessException.Category category) {
		return switch (category) {
			case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
			case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case CONFLICT -> HttpStatus.CONFLICT;
			case RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_CONTENT;
			case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
		};
	}

	// 400 VALIDATION_FAILED con l'elenco dei campi non validi (docs/api-rest)
	private ResponseEntity<Object> validationProblem(List<FieldViolation> errors, WebRequest request) {
		// getDescription(false) restituisce "uri=/api/v1/...": serve solo il percorso
		String path = request.getDescription(false).replace("uri=", "");

		ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Invalid data", "One or more fields are invalid", "VALIDATION_FAILED", path);
		problem.setProperty("errors", errors);

		return ResponseEntity.badRequest().body(problem);
	}

	// Formato comune di tutte le risposte di errore (docs/api-rest)
	private ProblemDetail createProblem(HttpStatus status, String title, String detail, String code, String path) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("code", code);
		problem.setInstance(URI.create(path));

		return problem;
	}
}
