package it.downfall.reversky.engine.core.validation;

/**
 * Input di simulazione incoerente. Lo strato {@code web} la trasforma in {@code 400} con un {@code ProblemDetail}
 * che riporta {@link #code()} e il messaggio come {@code detail} (CodingRequirements §8).
 * <p>
 * È diversa da una {@link IllegalStateException} durante la simulazione, che invece indica un bug del motore.
 */
public class InvalidSimulationInputException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ErrorCode code;

	/**
	 * Crea l'eccezione.
	 *
	 * @param code   codice dell'errore, letto dall'API
	 * @param detail spiegazione per chi legge il {@code ProblemDetail}, con la carta o la fazione coinvolta
	 */
	public InvalidSimulationInputException(ErrorCode code, String detail) {
		super(detail);
		this.code = code;
	}

	/**
	 * Codice dell'errore.
	 *
	 * @return il codice del contratto (docs/api-engine, "Errori")
	 */
	public ErrorCode code() {
		return code;
	}
}
