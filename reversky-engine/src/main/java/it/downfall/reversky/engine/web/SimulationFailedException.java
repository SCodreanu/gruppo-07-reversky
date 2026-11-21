package it.downfall.reversky.engine.web;

/**
 * Errore inatteso durante una simulazione: è un bug del motore (docs/api-engine, status 500).
 * <p>
 * Porta il seed della partita fino al {@link GlobalExceptionHandler}, che lo scrive nel log tecnico: con lo stesso
 * input e lo stesso seed la partita si riproduce identica. Al client non arriva né il seed né la causa.
 */
public class SimulationFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final long seed;

	/**
	 * Crea l'eccezione.
	 *
	 * @param seed  seed della partita fallita
	 * @param cause errore originale
	 */
	public SimulationFailedException(long seed, Throwable cause) {
		super("Simulazione fallita con seed " + seed, cause);
		this.seed = seed;
	}

	/**
	 * Seed della partita fallita.
	 *
	 * @return il seed
	 */
	public long seed() {
		return seed;
	}
}
