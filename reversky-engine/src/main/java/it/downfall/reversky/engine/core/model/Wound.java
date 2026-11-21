package it.downfall.reversky.engine.core.model;

import java.util.Optional;

/**
 * Ferita (docs/effetti, "Soglie"): una perdita reale di DEF sopra 0, dopo gli scudi, con la creatura che l'ha
 * causata. Il motore la riferisce all'effetto della creatura ferita con {@code onWounded} (deciso il 18 novembre).
 * <p>
 * La causa è la creatura che ha attaccato o il cui effetto ha inflitto il danno; manca quando il danno viene da una
 * carta che non è una creatura (Spell, Hidden, Terreno, Oggetto) o da un modificatore in attesa.
 */
public final class Wound {

	private final int amount;
	private final CreatureInPlay cause;

	/**
	 * Crea la ferita.
	 *
	 * @param amount DEF persa davvero, almeno 1
	 * @param cause  creatura che l'ha causata, o {@code null} se il danno non viene da una creatura
	 * @throws IllegalArgumentException se {@code amount} è minore di 1
	 */
	Wound(int amount, CreatureInPlay cause) {
		if (amount < 1) {
			throw new IllegalArgumentException("Una ferita vale almeno 1 DEF: " + amount);
		}
		this.amount = amount;
		this.cause = cause;
	}

	/**
	 * DEF persa davvero.
	 *
	 * @return almeno 1
	 */
	public int amount() {
		return amount;
	}

	/**
	 * Creatura che ha causato la ferita, con un attacco o con il suo effetto.
	 *
	 * @return la creatura, o vuoto se il danno non viene da una creatura
	 */
	public Optional<CreatureInPlay> cause() {
		return Optional.ofNullable(cause);
	}
}
