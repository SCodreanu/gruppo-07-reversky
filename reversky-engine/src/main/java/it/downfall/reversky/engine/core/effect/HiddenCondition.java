package it.downfall.reversky.engine.core.effect;

/**
 * Quando si attiva una Hidden (D-15): all'inizio del prossimo scontro del proprietario, oppure quando si verifica la
 * sua condizione (ENG-37).
 */
public enum HiddenCondition {

	/** Nessuna condizione: all'inizio del prossimo scontro del proprietario ({@link HiddenEffect#trigger}). */
	NONE,

	/**
	 * La propria creatura riceve una penalità (Remnants of Reality): si controlla dopo la fase, all'inizio di ogni
	 * controllo delle morti ({@link HiddenEffect#cancelPenalties}).
	 */
	PENALTY,

	/**
	 * Un attacco avversario ucciderebbe la propria creatura (Unseen Exit): si controlla al passo 3, dopo tutte le
	 * altre riduzioni ({@link HiddenEffect#preventLethalAttack}).
	 */
	LETHAL_ATTACK
}
