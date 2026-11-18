package it.downfall.reversky.engine.core.effect;

import it.downfall.reversky.engine.core.model.Wound;

/**
 * Effetto di una creatura (docs/effetti, "L'interfaccia"): il motore lo chiama nei momenti del round e ogni effetto
 * implementa solo quelli che gli servono.
 * <p>
 * Il registro crea una <b>nuova istanza per ogni entrata in campo</b>: "primo round", "una volta" e "primo attacco" si
 * contano per entrata (D-31, D-33) e ogni effetto tiene da sé lo stato che gli serve (deciso il 18 novembre), ad
 * esempio l'ultima avversaria contro cui ha già agito. Quando più effetti agiscono nello stesso momento, prima quelli
 * con {@link EffectPriority#NULLIFY}, poi la creatura dello sfidante (D-30).
 * <p>
 * Un effetto che colpisce la creatura avversaria la prende da {@link CreatureContext#targetEnemy()}, che controlla il
 * Fascicolo Sigillato 0 dell'avversario. I bonus ATK si danno come {@code StatModifier} con origine
 * {@code EFFECT} (deciso il 18 novembre), così gli effetti che sospendono i bonus ATK li vedono.
 */
public non-sealed interface CreatureEffect extends CardEffect {

	/**
	 * Priorità di risoluzione quando più effetti agiscono nello stesso momento.
	 *
	 * @return la priorità; {@link EffectPriority#NORMAL} se l'effetto non annulla altri effetti
	 */
	default EffectPriority priority() {
		return EffectPriority.NORMAL;
	}

	/**
	 * La creatura entra in campo (passo 0, dopo le Hidden): una volta per entrata. Se una Hidden o l'effetto d'entrata
	 * dell'altra creatura uccide prima, la creatura entra allo scontro dopo.
	 *
	 * @param ctx contesto
	 */
	default void onEnterPlay(CreatureContext ctx) {
	}

	/**
	 * Inizio di uno scontro (passo 0, dopo tutti gli {@code onEnterPlay}): la creatura affronta una nuova avversaria,
	 * sia quando entra sia quando sopravvive e ne arriva un'altra (deciso il 18 novembre; es. Mothman, Spearfinger,
	 * Bigfoot).
	 *
	 * @param ctx contesto
	 */
	default void onCombatStart(CreatureContext ctx) {
	}

	/**
	 * Inizio di ogni round (passo 1).
	 *
	 * @param ctx contesto
	 */
	default void onRoundStart(CreatureContext ctx) {
	}

	/**
	 * Passo 2: modifica l'attacco che questa creatura sta per infliggere, già con il bonus di fazione e i modificatori.
	 * Il motore applica poi il minimo 0.
	 *
	 * @param ctx    contesto
	 * @param attack attacco calcolato
	 * @return l'attacco modificato
	 */
	default int modifyOutgoingAttack(CreatureContext ctx, int attack) {
		return attack;
	}

	/**
	 * Passo 3: modifica il danno dell'attacco che questa creatura sta per ricevere. Il danno da effetto non passa di
	 * qui (docs/effetti, "Danno da effetto").
	 *
	 * @param ctx    contesto
	 * @param damage danno dell'attacco avversario, almeno 0
	 * @return il danno da applicare, almeno 0
	 */
	default int modifyIncomingDamage(CreatureContext ctx, int damage) {
		return damage;
	}

	/**
	 * Passo 5: dopo che i danni del round sono stati applicati, anche se la creatura ha DEF 0 o meno.
	 *
	 * @param ctx    contesto
	 * @param damage danno dell'attacco ricevuto, dopo il passo 3; "un attacco subito" conta solo con danno sopra 0
	 *               (D-33)
	 * @param wound  ferita subita da questo attacco: la DEF persa davvero sopra 0, dopo gli scudi
	 */
	default void afterDamage(CreatureContext ctx, int damage, int wound) {
	}

	/**
	 * La creatura ha perso DEF davvero (una ferita), per un attacco o per un danno da effetto di qualunque origine
	 * (deciso il 18 novembre): si chiama una volta per ferita, al controllo dopo la fase in cui è avvenuta, solo se la
	 * creatura è ancora viva. Una ferita del passo 4 arriva al controllo del passo 5, dopo {@code afterDamage}.
	 *
	 * @param ctx   contesto
	 * @param wound la ferita, con la creatura che l'ha causata se c'è
	 */
	default void onWounded(CreatureContext ctx, Wound wound) {
	}

	/**
	 * Controllo della DEF dopo ogni fase in cui può cambiare (deciso il 18 novembre), dopo gli {@code onWounded}, solo
	 * se la creatura è ancora viva: serve alle soglie "metà DEF" (M-06), sulla propria DEF o su quella avversaria.
	 * Si ripete finché nessuno subisce nuove ferite, quindi un effetto che lo usa deve ricordare se ha già agito.
	 *
	 * @param ctx contesto
	 */
	default void onDefenseCheck(CreatureContext ctx) {
	}

	/**
	 * Passo 6: fine del round, solo se la creatura ha ancora DEF sopra 0.
	 *
	 * @param ctx contesto
	 */
	default void onRoundEnd(CreatureContext ctx) {
	}

	/**
	 * La creatura muore (controllo delle morti, dopo il Badge di Uscita Negata): l'effetto agisce prima che lasci il
	 * campo, poi le morti si ricontrollano.
	 *
	 * @param ctx contesto
	 */
	default void onDeath(CreatureContext ctx) {
	}
}
