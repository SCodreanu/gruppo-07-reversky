package it.downfall.reversky.engine.core.effect;

/**
 * Effetto di un Oggetto: resta accanto al mazzo finché scatta una volta, poi si consuma (D-47).
 * <p>
 * Come per i Terreni, l'effetto dice <b>cosa</b> fa e il motore ({@code ItemService}) decide <b>quando</b>: ogni
 * Oggetto risponde a uno solo dei momenti qui sotto, gli altri restano ai valori predefiniti. La fazione dell'Oggetto
 * non limita l'effetto: vale per qualunque creatura del proprietario.
 */
public non-sealed interface ItemEffect extends CardEffect {

	/**
	 * La prima volta che una creatura del proprietario morirebbe, per qualunque danno, resta in vita con questa DEF
	 * (D-36, prima del controllo delle morti).
	 *
	 * @return la DEF con cui resta, o 0 se l'Oggetto non salva dalla morte
	 */
	default int survivalDefense() {
		return 0;
	}

	/**
	 * Scudo all'inizio di ognuno dei primi round del prossimo scontro di una creatura del proprietario (D-35).
	 *
	 * @return la DEF dello scudo, o 0 se l'Oggetto non dà scudi a inizio round
	 */
	default int roundStartShield() {
		return 0;
	}

	/**
	 * Per quanti round dello scontro vale {@link #roundStartShield()}.
	 *
	 * @return il numero di round, o 0 se l'Oggetto non dà scudi a inizio round
	 */
	default int roundStartShieldRounds() {
		return 0;
	}

	/**
	 * Annulla del tutto il primo effetto di creatura, Spell o Hidden avversaria che colpirebbe una creatura del
	 * proprietario (non gli attacchi, non i Terreni).
	 *
	 * @return {@code true} se l'Oggetto annulla un effetto avversario
	 */
	default boolean cancelsEnemyEffect() {
		return false;
	}

	/**
	 * Annulla la prima Hidden avversaria che starebbe per attivarsi: si scarta senza effetto.
	 *
	 * @return {@code true} se l'Oggetto annulla una Hidden avversaria
	 */
	default boolean cancelsEnemyHidden() {
		return false;
	}
}
