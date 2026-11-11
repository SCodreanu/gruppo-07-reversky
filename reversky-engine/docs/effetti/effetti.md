# Effetti speciali

Le abilità delle creature fanno parte del regolamento standard: sono sempre attive.

## Idea di base

Ogni effetto è **una classe Java** che implementa `CreatureEffect`. L'interfaccia definisce i **momenti** del round in cui un effetto può intervenire; ogni effetto implementa solo quelli che gli servono.

La carta indica il suo effetto con un **codice** (`effectCode`, es. `"HIDEBEHIND"`). L'`EffectRegistry` collega il codice alla classe.

## L'interfaccia

```java
public interface CreatureEffect {

	/** Codice univoco, uguale a quello salvato sulla carta. */
	String code();

	/** Priorità di risoluzione quando più effetti agiscono nello stesso momento. */
	default EffectPriority priority() { return EffectPriority.NORMAL; }

	/** La creatura entra in campo (inizio dello scontro). */
	default void onEnterPlay(EffectContext ctx) { }

	/** Inizio di ogni round. */
	default void onRoundStart(EffectContext ctx) { }

	/** Modifica l'attacco che questa creatura sta per infliggere. */
	default int modifyOutgoingAttack(EffectContext ctx, int attack) { return attack; }

	/** Modifica il danno che questa creatura sta per ricevere. */
	default int modifyIncomingDamage(EffectContext ctx, int damage) { return damage; }

	/** Dopo che i danni del round sono stati applicati. */
	default void afterDamage(EffectContext ctx, int damageTaken) { }

	/** Fine di ogni round, se la creatura è ancora viva. */
	default void onRoundEnd(EffectContext ctx) { }

	/** La creatura muore. */
	default void onDeath(EffectContext ctx) { }

	/** La creatura ha ucciso quella avversaria. */
	default void onKill(EffectContext ctx) { }
}
```

`EffectContext` dà accesso a: la creatura stessa (`self()`), la creatura avversaria (`opponent()`), i due giocatori, il numero del round, il `MatchLog`.

## Ordine di risoluzione di un round

L'ordine fa parte delle regole: **non cambiarlo senza aggiornare questo documento.**

```
0. Entrata in campo (solo a inizio scontro)
     - bonus DEF di fazione
     - bonus del Terreno
     - effetto dell'Oggetto (dopo il Terreno, D-48)
     - Hidden in attesa senza condizione (prima DAMAGE, poi SURVIVAL)
     - onEnterPlay di entrambe le creature, poi gli effetti che guardano
       l'entrata di una creatura avversaria (Mothman, The Stitched Corridor…)
     - effetti secondari dei Terreni "a inizio scontro" (Devouring Abyss)
1. onRoundStart
2. Calcolo attacco: ATK base + bonus ATK di fazione + modificatori → modifyOutgoingAttack → minimo 0
3. Calcolo danno ricevuto: modifyIncomingDamage della creatura colpita
4. Applicazione SIMULTANEA dei danni
5. afterDamage
6. onRoundEnd (solo creature con DEF > 0)
7. Morti: onDeath / onKill, poi danno al giocatore = DEF negativa della sconfitta (vedi regole-base)
```

Dopo **ogni** passo si controllano le morti con le regole del passo 7 (D-08): una creatura uccisa da un danno da effetto prima del passo 4 non attacca.

Le Hidden **con una condizione** (D-15) non scattano al passo 0: restano coperte e si controllano quando la loro condizione si può verificare: Unseen Exit al passo 3 (un attacco ucciderebbe la creatura, dopo tutte le altre riduzioni), Remnants of Reality dopo ogni penalità ricevuta. Una Hidden pescata nella pescata iniziale non si attiva nel primo scontro.

### Danno da effetto

Il danno inflitto da un effetto, una Spell, una Hidden, un Terreno o un Oggetto è una **perdita permanente di DEF**, non un attacco: non passa da `modifyIncomingDamage`, non attiva gli effetti che reagiscono agli attacchi (SCP-106, SCP-682, Unseen Exit) e non conta come "primo attacco". Può uccidere fuori dal passo 4 (D-08). Lo riducono solo gli effetti che parlano di penalità o di danni da effetto (Hollow Clearing, Remnants of Reality, Fascicolo Sigillato 0).

### Soglie

- **Ferita** = perdita reale di DEF sopra 0, dopo gli scudi.
- **Metà DEF** = DEF ≤ 50% della DEF all'entrata, bonus compresi; si controlla dopo ogni fase in cui la DEF cambia, solo se la creatura è ancora viva.

### Più effetti nello stesso momento

1. Prima per **priorità**: `NULLIFY` (effetti che annullano altri effetti) → `NORMAL`.
2. A parità di priorità: prima la creatura dello **sfidante**, poi quella dell'avversario.

Così l'ordine è sempre lo stesso e la partita è riproducibile. Approvato dai DSG il 9 novembre (D-30).

### Effetti annullati

Il Predicatore sotto la Pioggia e SCP-2521 annullano l'effetto della creatura avversaria **finché resta in campo**; Baku divora ogni attivazione di un effetto avversario **finché Baku è in campo**. Hanno priorità `NULLIFY`, quindi agiscono **prima** che l'effetto avversario possa partire nello stesso momento.

Regola degli effetti annullati:
- un effetto annullato **non scatta** più (nessun momento dell'interfaccia viene chiamato);
- i suoi **bonus ATK sono sospesi** finché resta annullato;
- la **DEF già guadagnata resta**.

```java
@Override
public void onEnterPlay(EffectContext ctx) {
	ctx.opponent().disableEffect();
	ctx.log().effect(ctx.self(), "cancels the effect of " + ctx.opponent().name());
}
```

Se due effetti `NULLIFY` si incontrano, agisce prima quello dello sfidante e annulla l'altro.

Il Custode delle Porte Incrociate **non** è un effetto `NULLIFY`: è immune agli effetti delle creature avversarie (filtro sul bersaglio), quindi non li spegne ma non ne subisce le conseguenze. Spell, Hidden, Terreni e Oggetti avversari lo colpiscono normalmente.

## Esempi

### Effetto semplice: Hidebehind

> Previene 2 danni dal primo attacco di ogni creatura avversaria che affronta.

```java
/**
 * Hidebehind — "Previene 2 danni dal primo attacco di ogni creatura avversaria".
 */
public final class HidebehindEffect implements CreatureEffect {

	public static final String CODE = "HIDEBEHIND";

	private static final int PREVENTED_DAMAGE = 2;

	/** Ultima avversaria contro cui lo scudo è già stato usato. */
	private CreatureInPlay shieldUsedAgainst;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int modifyIncomingDamage(EffectContext ctx, int damage) {
		CreatureInPlay attacker = ctx.opponent();
		// D-33: conta il primo attacco con danno > 0 di ogni avversaria
		if (attacker == shieldUsedAgainst || damage <= 0) {
			return damage;
		}
		shieldUsedAgainst = attacker;
		int reduced = Math.max(0, damage - PREVENTED_DAMAGE);
		ctx.log().effect(ctx.self(), "prevents " + (damage - reduced) + " damage");
		return reduced;
	}
}
```

Nota: l'effetto ha uno **stato** (`shieldUsedAgainst`). Per questo il registro crea **una nuova istanza per ogni creatura che entra in campo**.

### Effetto con soglia: The Unseen Matriarch

> La prima volta che la sua DEF scende a metà o meno, la creatura avversaria subisce danni pari alla DEF che ha perso (massimo 8) e −1 ATK finché resta in campo.

```java
@Override
public void afterDamage(EffectContext ctx, int damageTaken) {
	CreatureInPlay self = ctx.self();
	if (used || self.defense() <= 0 || self.defense() * 2 > self.initialDefense()) {
		return;
	}
	used = true;
	int lost = Math.min(MAX_DAMAGE, self.initialDefense() - self.defense());
	ctx.opponent().takeEffectDamage(lost, CODE);
	ctx.opponent().addAttackModifier(-1, CODE);
	ctx.log().effect(self, "deals " + lost + " damage to " + ctx.opponent().name());
}
```

La stessa soglia si controlla anche dopo le altre fasi in cui la DEF cambia (danni da effetto all'entrata, a inizio o fine round), non solo in `afterDamage`.

### Effetto alla morte: SCP-2317

> Alla morte infligge 4 danni alla creatura avversaria e dà +1 ATK alla prossima creatura alleata.

Il danno può uccidere anche l'avversaria: le morti si ricontrollano dopo gli `onDeath`, quindi possono morire **entrambe** (e in quel caso nessun giocatore subisce danni).
Il +1 ATK va nei modificatori in attesa del proprietario (D-34) e si applica alla sua prossima creatura che entra.

## Registro degli effetti

```java
public final class EffectRegistry {

	private final Map<String, Supplier<CreatureEffect>> factories = new LinkedHashMap<>();

	public static EffectRegistry withAllEffects() {
		EffectRegistry registry = new EffectRegistry();
		registry.register(HidebehindEffect.CODE, HidebehindEffect::new);
		registry.register(UnseenMatriarchEffect.CODE, UnseenMatriarchEffect::new);
		// ...
		return registry;
	}

	/** Crea una nuova istanza dell'effetto per una creatura che entra in campo. */
	public CreatureEffect create(String code) { ... }

	public boolean exists(String code) { ... }
}
```

L'elenco dei codici è esposto da `GET /api/v1/effects`: il backoffice dell'API lo usa per sapere quali effetti si possono assegnare a una carta.

## Ricetta: aggiungere un nuovo effetto

1. Scrivi il testo della carta in modo **preciso**: quando si attiva, quanto dura, quante volte. Se è ambiguo, chiedi ai DSG.
2. Crea `XxxEffect` in `core/effect/impl/` con Javadoc che riporta il testo della carta.
3. Scegli il codice (`UPPER_SNAKE_CASE`) e registralo in `EffectRegistry.withAllEffects()`.
4. Implementa solo i metodi dei momenti che servono.
5. Ogni volta che l'effetto fa qualcosa, scrivi un evento nel log (`ctx.log().effect(...)`).
6. Scrivi il test `XxxEffectTest` (vedi [test](../test/test.md)): caso in cui si attiva, caso in cui non si attiva, caso con effetto annullato.
7. Aggiorna la tabella qui sotto.

## Effetti implementati

| Codice | Carta | Momento | Stato |
|---|---|---|---|
| — | — | — | nessuno ancora |

L'elenco di **tutti** gli effetti da implementare (creature, Spell, Hidden, Terreni, Oggetti), con codici proposti, momenti e note, è nel [catalogo degli effetti](catalogo-effetti.md).
Gli effetti sono stati decisi nella [revisione carta per carta](../bilanciamento/revisione-effetti.md) del 10 e 11 novembre: lì ci sono, per ogni carta, i dettagli di motore e le interazioni.
Le modifiche al motore che gli effetti richiedono (es. numero massimo di attivazioni, morti fuori dal round) sono nei [pezzi di motore](catalogo-effetti.md#pezzi-di-motore-richiesti-dagli-effetti) e nelle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md#effetti).
