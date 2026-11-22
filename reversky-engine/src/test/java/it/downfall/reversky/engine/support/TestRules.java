package it.downfall.reversky.engine.support;

import it.downfall.reversky.engine.core.rules.RuleConfig;

/**
 * Rule set di prova per i test. L'engine non ha valori predefiniti: questi numeri esistono solo nei test.
 */
public final class TestRules {

	/** Versione del rule set di prova, uguale a quella degli esempi. */
	public static final String VERSION = "1.0";

	/** Punti vita iniziali del rule set {@code 1.0} (D-01, deciso il 22 novembre). */
	public static final int STARTING_LIFE_POINTS = 30;

	/** Limite di round totali del rule set {@code 1.0} (D-02). */
	public static final int MAX_ROUNDS = 75;

	private TestRules() {
	}

	/**
	 * Regole del rule set {@code 1.0}, le stesse degli esempi: 30 punti vita, limite di 75 round.
	 *
	 * @return le regole standard
	 */
	public static RuleConfig standard() {
		return new RuleConfig(VERSION, STARTING_LIFE_POINTS, MAX_ROUNDS);
	}

	/**
	 * Regole con valori scelti dal test, per esempio un limite di round basso.
	 *
	 * @param startingLifePoints punti vita iniziali
	 * @param maxRounds          limite di round
	 * @return le regole con versione {@link #VERSION}
	 */
	public static RuleConfig of(int startingLifePoints, int maxRounds) {
		return new RuleConfig(VERSION, startingLifePoints, maxRounds);
	}
}
