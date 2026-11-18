package it.downfall.reversky.engine.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.downfall.reversky.engine.core.validation.ErrorCode;
import it.downfall.reversky.engine.core.validation.InvalidSimulationInputException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Trasforma ogni eccezione in un {@code ProblemDetail} (RFC 9457) con il campo {@code code}, nello stesso formato
 * dell'API (docs/api-engine, "Errori"). Unico punto in cui un errore diventa uno status HTTP.
 * <p>
 * Estende {@link ResponseEntityExceptionHandler}: anche gli altri errori di Spring MVC (metodo non supportato, 404 di
 * routing...) hanno già il formato ProblemDetail, senza {@code code}. I testi sono in inglese come nell'API.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String INVALID_INPUT_TITLE = "Invalid simulation input";
	private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";

	/**
	 * Un errore su un singolo campo, per la lista {@code errors} della risposta.
	 *
	 * @param field   percorso del campo, es. {@code challenger.deck[0].name}
	 * @param message messaggio del vincolo, in inglese
	 */
	public record FieldViolation(String field, String message) {
	}

	/**
	 * Input incoerente trovato dal core (controlli 1–9 del contratto): 400 con il codice del controllo.
	 *
	 * @param ex      errore del core
	 * @param request richiesta HTTP
	 * @return la risposta 400
	 */
	@ExceptionHandler(InvalidSimulationInputException.class)
	public ResponseEntity<ProblemDetail> handleInvalidInput(InvalidSimulationInputException ex,
			HttpServletRequest request) {
		ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, INVALID_INPUT_TITLE, ex.getMessage(),
				ex.code().name(), request.getRequestURI());
		return ResponseEntity.badRequest().body(problem);
	}

	/**
	 * Bug durante la simulazione: 500 con un messaggio generico. Seed e stack trace vanno solo nel log tecnico,
	 * così la partita si può riprodurre.
	 *
	 * @param ex      errore con il seed della partita
	 * @param request richiesta HTTP
	 * @return la risposta 500
	 */
	@ExceptionHandler(SimulationFailedException.class)
	public ResponseEntity<ProblemDetail> handleSimulationFailed(SimulationFailedException ex,
			HttpServletRequest request) {
		log.error("Simulazione fallita: seed {}", ex.seed(), ex.getCause());
		return internalError(request.getRequestURI());
	}

	/**
	 * Qualunque altro errore inatteso: 500 con un messaggio generico, lo stack trace solo nel log.
	 *
	 * @param ex      errore
	 * @param request richiesta HTTP
	 * @return la risposta 500
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Errore inatteso su {}", request.getRequestURI(), ex);
		return internalError(request.getRequestURI());
	}

	/** Bean Validation dei DTO fallita ({@code @Valid}): 400 VALIDATION_FAILED con l'elenco dei campi. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
				.toList();
		ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, INVALID_INPUT_TITLE,
				"One or more fields are invalid", ErrorCode.VALIDATION_FAILED.name(), path(request));
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	/** JSON malformato o con un valore del tipo sbagliato (anche un enum che non esiste): 400 VALIDATION_FAILED. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, INVALID_INPUT_TITLE,
				"Malformed JSON or a value of the wrong type", ErrorCode.VALIDATION_FAILED.name(), path(request));
		return ResponseEntity.badRequest().body(problem);
	}

	private static ResponseEntity<ProblemDetail> internalError(String path) {
		ProblemDetail problem = createProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
				"An unexpected error occurred in the game engine", INTERNAL_ERROR_CODE, path);
		return ResponseEntity.internalServerError().body(problem);
	}

	private static String path(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

	// Formato comune di tutte le risposte di errore, uguale a quello dell'API
	private static ProblemDetail createProblem(HttpStatus status, String title, String detail, String code,
			String path) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("code", code);
		problem.setInstance(URI.create(path));
		return problem;
	}
}
