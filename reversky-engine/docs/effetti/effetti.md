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
     - Hidden in attesa
     - onEnterPlay di entrambe le creature
1. onRoundStart
2. Calcolo attacco: ATK base + bonus ATK di fazione + modificatori → modifyOutgoingAttack → minimo 0
3. Calcolo danno ricevuto: modifyIncomingDamage della creatura colpita
4. Applicazione SIMULTANEA dei danni
5. afterDamage
6. onRoundEnd (solo creature con DEF > 0)
7. Morti: onDeath / onKill, poi danno al giocatore = DEF negativa della sconfitta (vedi regole-base)
```

### Più effetti nello stesso momento

1. Prima per **priorità**: `NULLIFY` (effetti che annullano altri effetti) → `NORMAL`.
2. A parità di priorità: prima la creatura dello **sfidante**, poi quella dell'avversario.

Così l'ordine è sempre lo stesso e la partita è riproducibile. **[DA CONFERMARE]** con il team di game design.

### Effetti che annullano altri effetti

Predicatore sotto la Pioggia, SCP-2521 e Baku disattivano l'effetto avversario. Hanno priorità `NULLIFY`, quindi agiscono **prima** che l'effetto avversario possa partire nello stesso momento.

```java
@Override
public void onEnterPlay(EffectContext ctx) {
	ctx.opponent().disableEffect();
	ctx.log().effect(ctx.self(), "cancella l'effetto di " + ctx.opponent().name());
}
```

Se due effetti `NULLIFY` si incontrano, agisce prima quello dello sfidante e annulla l'altro.

## Esempi

### Effetto semplice: Hidebehind

> Il primo attacco che subisce viene schivato (0 danni). Dal secondo round non ha più effetto.

```java
/**
 * Hidebehind — il primo attacco subito nello scontro infligge 0 danni.
 */
public final class HidebehindEffect implements CreatureEffect {

	public static final String CODE = "HIDEBEHIND";

	private boolean dodgeUsed = false;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int modifyIncomingDamage(EffectContext ctx, int damage) {
		if (!dodgeUsed) {
			dodgeUsed = true;
			ctx.log().effect(ctx.self(), "schiva il primo attacco");
			return 0;
		}
		return damage;
	}
}
```

Nota: l'effetto ha uno **stato** (`dodgeUsed`). Per questo il registro crea **una nuova istanza per ogni creatura che entra in campo**.

### Effetto con soglia: SCP-096

> Quando la sua difesa scende al 50% o meno del valore iniziale, ottiene +3 attacco per il resto dello scontro.

```java
@Override
public void afterDamage(EffectContext ctx, int damageTaken) {
	CreatureInPlay self = ctx.self();
	if (!enraged && self.defense() > 0 && self.defense() * 2 <= self.initialDefense()) {
		enraged = true;
		self.addAttackModifier(3, "Furia incontrollabile");
	}
}
```

### Effetto alla morte: SCP-2317

> Quando muore, infligge 4 danni alla creatura avversaria.

Il danno può uccidere anche l'avversaria: le morti si ricontrollano dopo gli `onDeath`, quindi possono morire **entrambe** (e in quel caso nessun giocatore subisce danni).

## Registro degli effetti

```java
public final class EffectRegistry {

	private final Map<String, Supplier<CreatureEffect>> factories = new LinkedHashMap<>();

	public static EffectRegistry withAllEffects() {
		EffectRegistry registry = new EffectRegistry();
		registry.register(HidebehindEffect.CODE, HidebehindEffect::new);
		registry.register(Scp096Effect.CODE, Scp096Effect::new);
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

Effetti che richiedono chiarimenti prima di essere implementati **[DA CONFERMARE]**:
- **Mothman**: "se sta per perdere la sfida… ribalta il risultato" — riguarda la partita intera o lo scontro? Cosa significa "scarto"?
- **Skin-Stealers**: "copia il 25% di difesa e attacco… solo per 1 turno" — turno = round o scontro?
- **Wendigo**: "recupera 3 DEF" supera la DEF iniziale o si ferma al massimo?
- **SCP-001**: "perde metà della DEF residua (per difetto)" a ogni round — con DEF 1 la perdita è 0, quindi l'effetto da solo non uccide mai: verificare che sia voluto.
