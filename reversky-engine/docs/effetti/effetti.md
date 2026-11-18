# Effetti speciali

Le abilità delle creature fanno parte del regolamento standard: sono sempre attive.

## Idea di base

Ogni effetto è **una classe Java** che implementa `CreatureEffect`. L'interfaccia definisce i **momenti** del round in cui un effetto può intervenire; ogni effetto implementa solo quelli che gli servono.

La carta indica il suo effetto con un **codice** (`effectCode`, es. `"HIDEBEHIND"`). L'`EffectRegistry` collega il codice alla classe.

## L'interfaccia

```java
public non-sealed interface CreatureEffect extends CardEffect {

	/** Priorità di risoluzione quando più effetti agiscono nello stesso momento. */
	default EffectPriority priority() { return EffectPriority.NORMAL; }

	/** La creatura entra in campo: una volta per entrata (passo 0). */
	default void onEnterPlay(CreatureContext ctx) { }

	/** Inizio di uno scontro: la creatura affronta una nuova avversaria (passo 0, dopo gli onEnterPlay). */
	default void onCombatStart(CreatureContext ctx) { }

	/** Inizio di ogni round (passo 1). */
	default void onRoundStart(CreatureContext ctx) { }

	/** Modifica l'attacco che questa creatura sta per infliggere (passo 2). */
	default int modifyOutgoingAttack(CreatureContext ctx, int attack) { return attack; }

	/** Modifica il danno dell'attacco che questa creatura sta per ricevere (passo 3). */
	default int modifyIncomingDamage(CreatureContext ctx, int damage) { return damage; }

	/** Dopo che i danni del round sono stati applicati, anche se la creatura è morta (passo 5). */
	default void afterDamage(CreatureContext ctx, int damage, int wound) { }

	/** Subito dopo il suo afterDamage, se il suo attacco ha fatto danni (passo 5, ENG-34). */
	default void afterAttack(CreatureContext ctx, int damage) { }

	/** Una ferita, da un attacco o da un effetto: al controllo dopo la fase, se la creatura è ancora viva. */
	default void onWounded(CreatureContext ctx, Wound wound) { }

	/** Controllo della DEF dopo ogni fase in cui può cambiare, se la creatura è ancora viva (soglie, M-06). */
	default void onDefenseCheck(CreatureContext ctx) { }

	/** Fine di ogni round, se la creatura è ancora viva (passo 6). */
	default void onRoundEnd(CreatureContext ctx) { }

	/** La creatura muore, prima di lasciare il campo (passo 7). */
	default void onDeath(CreatureContext ctx) { }

	/** L'avversaria è morta e questa creatura è sopravvissuta, a scontro finito (passo 7, ENG-34). */
	default void onKill(CreatureContext ctx, CreatureInPlay defeated) { }
}
```

`code()` viene da `CardEffect`.

`CreatureContext` dà accesso a: la creatura stessa (`self()`), la creatura avversaria in campo (`enemy()`, per leggerla), i due giocatori (`owner()`, `opponent()`), il round (`round()`) e il `MatchLog` (`log()`). Per **colpire** la creatura avversaria (danno o malus) un effetto usa `targetEnemy()`: è vuoto se l'avversaria non c'è o ha già DEF 0 o meno, e se un Fascicolo Sigillato 0 dell'avversario annulla l'attivazione (deciso il 18 novembre). Si crea un contesto per ogni chiamata, quindi il Fascicolo si controlla una volta per attivazione.

`afterDamage` riceve il **danno** dell'attacco (dopo il passo 3: "un attacco subito" conta solo con danno sopra 0, D-33) e la **ferita**: la DEF persa davvero sopra 0, dopo gli scudi (`CreatureInPlay.takeDamage` la restituisce).

`onWounded` e `onDefenseCheck` (aggiunti il 18 novembre con `ENG-26`) servono agli effetti che reagiscono a **qualunque** perdita di DEF, non solo agli attacchi:

- ogni ferita resta in coda nella creatura (`Wound`, con la creatura che l'ha causata: l'attaccante o la creatura il cui effetto ha inflitto il danno; nessuna causa per Spell, Hidden, Terreni, Oggetti e modificatori in attesa). Per questo un effetto infligge danno con `takeDamage(danno, causa)` (in `EffectActions.damage(ctx, bersaglio, danno)`), una carta che non è una creatura con `takeDamage(danno)`;
- al **controllo della DEF** (`CreatureEffectService.defenseCheck`) il motore chiama, per ogni creatura viva e nell'ordine D-30, `onWounded` una volta per ferita, poi `onDefenseCheck`; se intanto qualcuno subisce nuove ferite (es. il danno di The Unseen Matriarch) il controllo si ripete. Le ferite di una creatura con DEF 0 o meno restano in coda: se un Badge la salva arrivano dopo;
- il controllo si fa all'inizio di ogni controllo delle morti (`DeathResolver`, quindi dopo ogni fase, D-08), di nuovo dopo gli `onDeath`, e al passo 5 dopo `afterDamage`.

Li usano Wretches, Shuten-dōji e SCP-096 (`onWounded`), Nure-onna e The Unseen Matriarch (`onDefenseCheck`). SCP-096 marchia solo una ferita causata da una creatura avversaria.

`afterAttack` e `onKill` (aggiunti il 18 novembre con `ENG-34`):

- `afterAttack` riceve il danno che l'attacco della creatura ha fatto all'avversaria, dopo il passo 3, scudi e DEF sotto 0 compresi; si chiama al passo 5 subito dopo il suo `afterDamage`, solo con danno sopra 0, anche se la creatura ha DEF 0 o meno (The Bloodless Hound);
- `onKill` scatta per **ogni** morte dell'avversaria, per qualunque causa, come `creaturesDefeated` (D-42): attacco, effetto, Spell, Hidden, Terreno. Lo chiama il `DeathResolver` a scontro finito, quando l'avversaria ha lasciato il campo, il suo proprietario ha perso i punti vita e i modificatori fino a fine scontro della sopravvissuta sono scaduti. In una morte doppia nessuno lo riceve (The Bloodless Hound, Wendigo; poi La Regina dei Sentieri Perduti).

La **guarigione** è `CreatureInPlay.heal(n)` (deciso il 18 novembre): DEF permanente in più, senza superare la DEF all'entrata, scudi compresi, quindi uno scudo dell'entrata scaduto si può recuperare. Una creatura con DEF 0 o meno è morta e non recupera niente (D-16), quindi una guarigione non salva mai da una morte. Le ferite già subite restano in `woundsTaken()`. Nel log è un `EFFECT_TRIGGERED` ("recovers 3 DEF (DEF 9)"), nessun evento nuovo.

Il **sigillo dei bonus ATK** (deciso il 18 novembre, `ENG-35`) è un modificatore (`StatModifier.sealAttack`, `Stat.ATTACK_SEAL`) con origine, lato e durata come gli altri: finché è attivo, i bonus ATK della creatura (i modificatori di ATK positivi, di qualunque origine, anche quelli ricevuti dopo) contano tanti punti in meno, mai sotto 0. I malus contano sempre per intero e il bonus di fazione, che non è un modificatore, non si sigilla. `CreatureInPlay.effectiveAttackBonus()` dà i bonus che contano davvero: "se non ne ha" (L'Addetto alle Uscite, Static Interference) vuol dire che è 0, quindi un bonus già tutto sigillato non conta. Il sigillo dell'Addetto e di Deathmoths resta sull'avversaria per tutta la sua permanenza, anche dopo la loro morte; quello di Static Interference dura fino a fine scontro. Lo userà anche Kitsune (`ENG-38`).

SCP-3999 **toglie** i bonus (`CreatureInPlay.removeBonuses()`, deciso il 18 novembre): i modificatori di ATK positivi e gli scudi, di qualunque origine, per sempre. Restano malus, sigilli, congelamenti e la DEF permanente (bonus DEF di fazione e "+N DEF" degli effetti compresi); dello scudo sparisce solo la parte non usata, quindi non uccide mai. Il bonus del Terreno tolto non torna finché non arriva un altro Terreno (D-17). Il suo +1 ATK arriva anche se il Fascicolo Sigillato 0 annulla l'attivazione sull'avversaria, come il bonus di Smiler.

Un **effetto collegato** è l'effetto di un'altra carta che la creatura porta finché resta in campo (deciso il 18 novembre, `ENG-34`): arriva con un modificatore in attesa (`PendingModifier.attached()`), che lo passa alla creatura (`CreatureInPlay.attachEffect`). Il motore chiama tutti gli effetti di una creatura (`CreatureInPlay.effects()`): prima il suo, poi quelli collegati, nell'ordine in cui sono arrivati; la priorità D-30 la decide l'effetto della creatura. Un effetto collegato è della Spell, non della creatura: annullare l'effetto della creatura non lo tocca. Lo usa SCP-109.

`onCombatStart` (aggiunto il 18 novembre con `ENG-25`, D-31) scatta a ogni inizio di scontro per entrambe le creature, cioè ogni volta che una creatura affronta una nuova avversaria: quando entra e quando sopravvive e ne arriva un'altra. Serve agli effetti "contro ogni avversaria" (Mothman, Spearfinger, Bigfoot). `onEnterPlay` resta una volta per entrata.

Ogni effetto tiene da sé lo stato che gli serve per "primo round", "una volta", "contro ogni avversaria" (D-31, deciso il 18 novembre): non c'è un contatore di attivazioni nel motore. I bonus ATK degli effetti si danno come `StatModifier` con origine `EFFECT` e una durata, non in `modifyOutgoingAttack` (deciso il 18 novembre): così compaiono nell'ATK e nel log e gli effetti che sospendono i bonus ATK (Static Interference, L'Addetto alle Uscite, Deathmoths) li vedono. `modifyOutgoingAttack` resta per i casi che dipendono dall'avversaria del momento; la sua modifica compare in `ATTACK_COMPUTED` come `effectBonus`.

L'istanza dell'effetto sta nella creatura (`CreatureInPlay.effect()`): `DrawService` la crea dal registro quando la creatura entra in campo. `CreatureEffectService` chiama i momenti nei passi del round; `DeathResolver` chiama `onDeath` e `onKill`.

## Ordine di risoluzione di un round

L'ordine fa parte delle regole: **non cambiarlo senza aggiornare questo documento.**

```
0. Entrata in campo (solo a inizio scontro)
     - bonus DEF di fazione
     - modificatori in attesa del proprietario (D-34), solo alla creatura al suo primo scontro: Spell SHIELD e BURN,
       Spell DAMAGE pescate senza bersaglio; poi controllo delle morti (D-08)
     - bonus del Terreno, alle creature della sua fazione che non l'hanno ancora ricevuto (una volta per Terreno)
     - effetto dell'Oggetto (dopo il Terreno, D-48)
     - Hidden in attesa senza condizione (prima DAMAGE, poi SURVIVAL; a parità prima lo sfidante); se una Hidden
       uccide, lo scontro finisce qui e le Hidden non ancora attivate restano coperte per lo scontro dopo
     - onEnterPlay delle creature che non sono ancora entrate (una volta per entrata, D-31), poi onCombatStart di
       entrambe (Mothman, Spearfinger, Bigfoot…); dopo ogni effetto si controllano le morti, come per le Hidden: se
       uno uccide lo scontro finisce lì, e chi non è entrato entra allo scontro dopo (deciso il 18 novembre)
     - si fissa la DEF all'entrata delle creature appena entrate (M-06): bonus compresi, danni presi fin qui esclusi
     - effetti secondari dei Terreni "a inizio scontro" (Devouring Abyss): prima lo sfidante, con il controllo delle
       morti dopo ognuno (deciso il 18 novembre)
1. Scudo della Bussola dei Sentieri Falsi (nei primi tre round del suo scontro), poi onRoundStart
2. Calcolo attacco: ATK base + bonus ATK di fazione + modificatori → modifyOutgoingAttack → minimo 0; un attacco
   congelato (Yuki-onna) vale 0
3. Calcolo danno ricevuto: modifyIncomingDamage della creatura colpita
4. Applicazione SIMULTANEA dei danni
5. afterDamage e afterAttack, poi il controllo della DEF: onWounded per ogni ferita, poi onDefenseCheck
6. onRoundEnd (solo creature con DEF > 0)
7. Morti: onDeath, poi danno al giocatore = DEF negativa della sconfitta (vedi regole-base), poi onKill della
   sopravvissuta
```

Dopo **ogni** passo si controllano le morti con le regole del passo 7 (D-08), con il `DeathResolver`: una creatura uccisa da un danno da effetto prima del passo 4 non attacca. Ogni controllo delle morti comincia con il controllo della DEF (`onWounded`, `onDefenseCheck`, deciso il 18 novembre), che può uccidere a sua volta.
Se una creatura muore prima del passo 4, il round si interrompe: **il round conta** (in `totalRounds` e per il limite di round, come il suo `ROUND_STARTED` nel log), nessuno attacca e i modificatori a round della sopravvissuta scadono come a fine round (deciso il 17 novembre).
Lo stesso controllo vale fuori dai round: una Spell pescata o una Hidden all'inizio dello scontro possono uccidere, e la morte ha le stesse conseguenze (danno al giocatore, creatura sconfitta, fine dello scontro). Se all'inizio dello scontro resta una sola creatura, il round non si gioca e chi ha il campo vuoto pesca di nuovo.

Le Hidden **con una condizione** (D-15) non scattano al passo 0: restano coperte e si controllano quando la loro condizione si può verificare: Unseen Exit al passo 3 (un attacco ucciderebbe la creatura, dopo tutte le altre riduzioni), Remnants of Reality dopo ogni penalità ricevuta. Una Hidden pescata nella pescata iniziale non si attiva nel primo scontro.

### Danno da effetto

Il danno inflitto da un effetto, una Spell, una Hidden, un Terreno o un Oggetto è una **perdita permanente di DEF**, non un attacco: non passa da `modifyIncomingDamage`, non attiva gli effetti che reagiscono agli attacchi (SCP-106, SCP-682, Unseen Exit) e non conta come "primo attacco". È però una ferita, se toglie DEF sopra 0: arriva a `onWounded` (Wretches, Shuten-dōji, SCP-096). Può uccidere fuori dal passo 4 (D-08). Lo riducono solo gli effetti che parlano di penalità o di danni da effetto (Hollow Clearing, Remnants of Reality, Fascicolo Sigillato 0).

### Soglie

- **Ferita** = perdita reale di DEF sopra 0, dopo gli scudi, da un attacco o da un danno da effetto; ha come causa la creatura che l'ha inflitta, se c'è.
- **DEF all'entrata** = la DEF alla fine dell'inizio dello scontro in cui la creatura entra, **bonus compresi e danni esclusi** (deciso il 18 novembre): contano bonus DEF di fazione, Spell `SHIELD`, Terreno, Oggetto, Hidden `SURVIVAL` e la DEF del proprio `onEnterPlay`; non contano danni in attesa, Hidden `DAMAGE`, danni d'entrata avversari (Il Direttore dell'Edificio). Prima di fissarla si calcola sulla DEF attuale più i danni presi.
- **Metà DEF** = DEF ≤ 50% della DEF all'entrata; si controlla in `onDefenseCheck` dopo ogni fase in cui la DEF cambia, solo se la creatura è ancora viva.
- **Già ferita** (Hounds) = DEF attuale sotto la DEF all'entrata (deciso il 18 novembre): conta anche uno scudo consumato o il bonus di un Terreno uscito.

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

Hidebehind e Nurikabe estendono la classe base `FirstAttackShieldEffect` (D-37), che fa tutto il lavoro:

```java
@Override
public final int modifyIncomingDamage(CreatureContext ctx, int damage) {
	CreatureInPlay attacker = ctx.enemy().orElse(null);
	// D-33: conta il primo attacco con danno > 0 di ogni avversaria
	if (damage <= 0 || attacker == null || attacker == usedAgainst) {
		return damage;
	}
	usedAgainst = attacker;
	int reduced = Math.max(0, damage - prevented);
	ctx.log().effectTriggered(ctx.self().side(), ctx.self().name(),
			"prevents " + (damage - reduced) + " damage from the first attack of " + attacker.name());
	return reduced;
}
```

Nota: l'effetto ha uno **stato** (`usedAgainst`). Per questo il registro crea **una nuova istanza per ogni creatura che entra in campo**.

### Effetto con soglia: The Unseen Matriarch

> La prima volta che la sua DEF scende a metà o meno, la creatura avversaria subisce danni pari alla DEF che ha perso (massimo 8) e −1 ATK finché resta in campo.

```java
@Override
public void onDefenseCheck(CreatureContext ctx) {
	CreatureInPlay self = ctx.self();
	if (used || self.defense() * 2 > self.initialDefense()) {
		return;
	}
	used = true;
	int damage = Math.min(MAX_DAMAGE, self.woundsTaken());
	List<String> parts = new ArrayList<>();
	ctx.targetEnemy().ifPresent(enemy -> {
		if (damage > 0) {
			parts.add(EffectActions.damage(ctx, enemy, damage));
		}
		parts.add(EffectActions.attack(enemy, self, ENEMY_ATTACK, ModifierDuration.whileInPlay(), "while in play"));
	});
	EffectActions.log(ctx, parts);
}
```

La soglia si controlla in `onDefenseCheck`, che il motore chiama dopo ogni fase in cui la DEF può cambiare (passo 0, passi 1, 5 e 7, Spell e Hidden), non solo dopo i danni del round. La DEF persa conta solo le ferite vere (`woundsTaken()`), non la parte assorbita dagli scudi. Se l'avversaria non si può colpire (campo vuoto, Fascicolo Sigillato 0) l'effetto è consumato lo stesso (deciso il 18 novembre).

### Effetto alla morte: SCP-2317

> Alla morte infligge 4 danni alla creatura avversaria e dà +1 ATK alla prossima creatura alleata.

Il danno può uccidere anche l'avversaria: le morti si ricontrollano dopo gli `onDeath`, quindi possono morire **entrambe** (e in quel caso nessun giocatore subisce danni). Nel log `CREATURE_DIED` della creatura viene prima del suo effetto alla morte.
Il +1 ATK va nei modificatori in attesa del proprietario (D-34) e si applica alla sua prossima creatura che entra.

## Effetti di Spell, Hidden, Terreni e Oggetti

Le carte che non sono creature hanno un'interfaccia per tipo, sotto l'interfaccia chiusa `CardEffect` (in `core/effect`), con solo i momenti che servono a quel tipo (ENG-22):

| Interfaccia | Momento | Chi la chiama |
|---|---|---|
| `SpellEffect` | `resolve(ctx)`: una volta, quando la Spell viene pescata, poi la carta si scarta | `ENG-19` |
| `HiddenEffect` | `trigger(ctx)`: una volta, quando la Hidden si attiva, poi la carta si scarta | `ENG-20` |
| `TerrainEffect` | `attackBonus()` e `defenseBonus()`: il bonus base, applicato dal motore alle creature della fazione; per gli effetti secondari `onCombatStart`, `onWounded`, `reduceDefensePenalty`, `reduceDamageSpell` | `ENG-21`, `ENG-27` |
| `ItemEffect` | nessuno per ora: i momenti degli Oggetti arrivano con `ENG-33` | `ENG-33` |

Gli effetti `P2` aggiungono i loro momenti all'interfaccia del loro tipo (es. le condizioni di Unseen Exit e Remnants of Reality, gli effetti secondari dei Terreni).
`EffectContext` dà la partita (`state()`), il giocatore che possiede la carta (`owner()`), il suo avversario (`opponent()`), la carta (`card()`) e il log (`log()`).

## Registro degli effetti

`EffectRegistry` collega ogni codice al **tipo di carta** a cui appartiene e alla classe che lo implementa. Un codice vale per un solo tipo di carta: il codice di un Terreno su una Spell è un errore dell'input (`UNKNOWN_EFFECT`, come un codice che non esiste).

```java
public static EffectRegistry withAllEffects() {
	return new EffectRegistry()
			.registerCreature(HidebehindEffect.CODE, HidebehindEffect::new)
			.registerSpell(VeilOfAnchoredEffect.CODE, VeilOfAnchoredEffect::new)
			.registerHidden(LastSanctuaryEffect.CODE, LastSanctuaryEffect::new)
			.registerTerrain(EchoRiftEffect.CODE, EchoRiftEffect::new);
}
```

- `exists(codice, tipoDiCarta)` e `appliesTo(codice)` servono alla validazione dell'input;
- `createCreature(codice)`, `createSpell(codice)`, `createHidden(codice)`, `createTerrain(codice)`, `createItem(codice)` creano una **nuova istanza** a ogni uso, perché un effetto può avere uno stato;
- `codes()` dà tutti i codici con il tipo di carta, nell'ordine di registrazione.

Il registro si crea una volta all'avvio (`EngineConfiguration`) e poi si legge soltanto: la stessa istanza serve tutte le partite. Il `MatchSimulator` lo riceve nel costruttore; nei test si usa un registro con effetti di prova (`TestEffects.stubsFor(...)` per gli effetti finti).

Una creatura può non avere codice (le Common): allora gioca senza effetto. Se ha un codice, deve essere registrato come effetto di creatura, come per le altre carte (`UNKNOWN_EFFECT`, deciso il 18 novembre): gli effetti delle creature non ancora implementati (i `P2`) danno `400` finché non arrivano, e intanto l'API lascia vuoto `effect_code`.

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
| `HIDEBEHIND` | Hidebehind | creatura, `modifyIncomingDamage` | fatto, `FirstAttackShieldEffectTest`, esempio 09 |
| `NURIKABE` | Nurikabe | creatura, `modifyIncomingDamage` | fatto, `FirstAttackShieldEffectTest` |
| `KAPPA` | Kappa | creatura, `onEnterPlay` (+3 ATK per un round) | fatto, `KappaEffectTest` |
| `SCP_096` | SCP-096 (Shy Guy) | creatura, `onWounded` (+3 ATK fino a fine scontro) | fatto, `Scp096EffectTest` |
| `SCP_2317` | SCP-2317 (A Door in the Woods) | creatura, `onDeath` | fatto, `Scp2317EffectTest` |
| `SMILER` | Smiler | creatura, `onRoundEnd` | fatto, `SmilerEffectTest` |
| `WAMPUS_CAT` | Wampus Cat | creatura, `onEnterPlay` (avv. −1 ATK finché resta in campo) | fatto, `EntryEffectsTest` |
| `SILENT_SHEPHERD` | Il Pastore Muto (The Silent Shepherd) | creatura, `onDeath` (scudo di 2 DEF alla prossima) | fatto, `CombatEffectsTest` |
| `WHITE_STAG` | Cervo Bianco (White Stag) | creatura, `onEnterPlay` (classe base `EntryAndNextDamageEffect`) | fatto, `EntryEffectsTest` |
| `NAMELESS_CALLER` | The Nameless Caller | creatura, `onEnterPlay` | fatto, `EntryEffectsTest` |
| `FOOTPRINT_GATHERER` | Il Raccoglitore di Orme | creatura, `onEnterPlay` (classe base `EntryDefenseEffect`) | fatto, `EntryEffectsTest` |
| `SPEARFINGER` | Spearfinger | creatura, `onCombatStart` (+2 ATK per un round) | fatto, `CombatEffectsTest` |
| `BELL_WITCH` | Bell Witch | creatura, `onRoundStart`, `onDeath` | fatto, `CombatEffectsTest` |
| `MOTHMAN` | Mothman | creatura, `onCombatStart` | fatto, `CombatEffectsTest` |
| `BIGFOOT` | Bigfoot | creatura, `onCombatStart`, `modifyIncomingDamage` | fatto, `CombatEffectsTest` |
| `THORNED_MATRIARCH` | The Thorned Matriarch | creatura, `onRoundStart` | fatto, `CombatEffectsTest` |
| `SCP_173` | SCP-173 (The Sculpture) | creatura, `onEnterPlay` (classe base `EntryDamageEffect`) | fatto, `EntryEffectsTest` |
| `SCP_049` | SCP-049 (Plague Doctor) | creatura, `onEnterPlay` (classe base `FadingAttackPenaltyEffect`) | fatto, `EntryEffectsTest` |
| `NAME_COLLECTOR` | Il Collezionista di Nomi | creatura, `onEnterPlay` (classe base `EntryAndNextDamageEffect`) | fatto, `EntryEffectsTest` |
| `SCP_053` | SCP-053 (The Young Girl) | creatura, `onEnterPlay` | fatto, `EntryEffectsTest` |
| `SCP_2006` | SCP-2006 (Too Spooky) | creatura, `onEnterPlay` (cambia l'ATK base) | fatto, `EntryEffectsTest` |
| `INDEX_MAW` | The Index Maw | creatura, `onEnterPlay` (classe base `EntryDefenseEffect`) | fatto, `EntryEffectsTest` |
| `SCP_106` | SCP-106 (Old Man) | creatura, `afterDamage` | fatto, `CombatEffectsTest` |
| `SCP_682` | SCP-682 (Hard-to-Destroy Reptile) | creatura, `modifyIncomingDamage` | fatto, `CombatEffectsTest` |
| `DROWNED_MEMORY` | The Drowned Memory | creatura, `onEnterPlay` | fatto, `EntryEffectsTest` |
| `SCP_3000` | SCP-3000 (Anantashesha) | creatura, `onEnterPlay`, `modifyIncomingDamage` | fatto, `CombatEffectsTest` |
| `SCP_001` | SCP-001 (When Day Breaks) | creatura, `onEnterPlay`, `onRoundStart` | fatto, `CombatEffectsTest` |
| `HOWLERS` | Howlers | creatura, `onEnterPlay` (classe base `FadingAttackPenaltyEffect`) | fatto, `WoundEffectsTest` |
| `WRETCHES` | Wretches | creatura, `onWounded` (+2 ATK finché resta in campo, una volta) | fatto, `WoundEffectsTest` |
| `FADED_TAG` | Cartellino Sbiadito (Faded Tag) | creatura, `onEnterPlay` (classe base `EntryAndNextDamageEffect`) | fatto, `WoundEffectsTest` |
| `LIVING_WAITING_ROOM` | Sala d'Attesa Vivente (Living Waiting Room) | creatura, `modifyIncomingDamage` (classe base `FirstAttackShieldEffect`) | fatto, `FirstAttackShieldEffectTest` |
| `GNAWING_WITNESS` | The Gnawing Witness | creatura, `onEnterPlay` (classe base `EntryDamageEffect`) | fatto, `WoundEffectsTest` |
| `SKIN_STEALERS` | Skin-Stealers | creatura, `onEnterPlay` (metà della DEF base avversaria, massimo 4) | fatto, `WoundEffectsTest` |
| `SERVICE_DOUBLE` | Il Doppio di Servizio (The Service Double) | creatura, `onEnterPlay` (+3 ATK per due round) | fatto, `WoundEffectsTest` |
| `BUILDING_DIRECTOR` | Il Direttore dell'Edificio (The Building Director) | creatura, `onEnterPlay` (40% della DEF base avversaria, massimo 4) | fatto, `WoundEffectsTest` |
| `HOUNDS` | Hounds | creatura, `modifyOutgoingAttack` (+4 contro un'avversaria sotto la sua DEF all'entrata) | fatto, `WoundEffectsTest` |
| `NOPPERA_BO` | Noppera-bō | creatura, `onEnterPlay` (classe base `FadingAttackPenaltyEffect`) | fatto, `WoundEffectsTest` |
| `ROKUROKUBI` | Rokurokubi | creatura, `onEnterPlay` (classe base `EntryAndNextDamageEffect`) | fatto, `WoundEffectsTest` |
| `THREADBOUND_DOLL` | The Threadbound Doll | creatura, `onDeath` (4 danni) | fatto, `DreamEffectsTest` |
| `TORII_SHADOW` | L'Ombra del Torii (Torii Shadow) | creatura, `onEnterPlay` (+3 DEF con il Terreno della sua fazione, altrimenti +2) | fatto, `DreamEffectsTest` |
| `MASKED_BLOOM` | The Masked Bloom | creatura, `onEnterPlay` (classe base `FadingAttackPenaltyEffect`) | fatto, `WoundEffectsTest` |
| `YUKI_ONNA` | Yuki-onna | creatura, `onEnterPlay` (attacco avversario congelato per un round) | fatto, `DreamEffectsTest` |
| `JOROGUMO` | Jorōgumo | creatura, `modifyIncomingDamage` (−3 ai primi due attacchi) | fatto, `DreamEffectsTest` |
| `NURE_ONNA` | Nure-onna | creatura, `onDefenseCheck` (soglia sulla DEF avversaria, una volta per avversaria) | fatto, `DreamEffectsTest` |
| `FACE_COLLECTOR` | The Face Collector | creatura, `onEnterPlay` (ruba 2 e 1 ATK) | fatto, `DreamEffectsTest` |
| `UNSEEN_MATRIARCH` | The Unseen Matriarch | creatura, `onDefenseCheck` (soglia sulla propria DEF, una volta) | fatto, `DreamEffectsTest` |
| `SHUTEN_DOJI` | Shuten-dōji | creatura, `onWounded` (+4 ATK per ferita, massimo +12) | fatto, `DreamEffectsTest` |
| `ROOTBOUND_DOPPELGANGER` | The Rootbound Doppelganger (Mutant) | creatura, `modifyIncomingDamage`, `onRoundStart` (classe base `FirstAttackShieldEffect`) | fatto, `MutantEffectsTest` |
| `STITCHED_CORRIDOR` | The Stitched Corridor (Mutant) | creatura, `onCombatStart`, `modifyIncomingDamage` (classe base `FirstAttackShieldEffect`) | fatto, `MutantEffectsTest` |
| `BLOODLESS_HOUND` | The Bloodless Hound | creatura, `afterAttack` (recupera metà del danno), `onKill` (recupera 4) | fatto, `HealEffectsTest` |
| `WENDIGO` | Wendigo | creatura, `onRoundEnd` (3 danni più 1 per eliminazione, massimo 6), `onKill` (recupera 5) | fatto, `HealEffectsTest` |
| `EXIT_CLERK` | L'Addetto alle Uscite (The Exit Clerk) | creatura, `onEnterPlay` (sigillo di 2 finché l'avversaria resta in campo; senza bonus anche −1 ATK nel primo round) | fatto, `BonusSealEffectsTest` |
| `DEATHMOTHS` | Deathmoths | creatura, `onEnterPlay` (sigillo di 2 e −2 ATK finché l'avversaria resta in campo) | fatto, `BonusSealEffectsTest` |
| `SCP_3999` | SCP-3999 | creatura, `onEnterPlay`, `onRoundStart` (toglie i bonus all'entrata e nel 3°, 5°, … round in campo; +1 ATK ogni volta, massimo +3) | fatto, `BonusSealEffectsTest` |
| `BITE_OF_REVERSE` | Morso del Rovescio (Bite of the Reverse) | Spell `DAMAGE`, alla pescata | fatto, `DamageSpellEffectTest` |
| `SKIN_FRACTURE` | Frattura della Pelle (Skin Fracture) | Spell `DAMAGE`, alla pescata | fatto, `DamageSpellEffectTest` |
| `DISSOLUTION` | Dissoluzione (Dissolution) | Spell `DAMAGE`, alla pescata | fatto, `DamageSpellEffectTest` |
| `VEIL_OF_ANCHORED` | Velo degli Ancorati (Veil of the Anchored) | Spell `SHIELD`, in attesa | fatto, `ShieldSpellEffectTest` |
| `REGISTRY_PACT` | Patto del Registro (Registry Pact) | Spell `SHIELD`, in attesa | fatto, `ShieldSpellEffectTest` |
| `RAIN_ARMOR` | Armatura della Pioggia (Rain Armor) | Spell `SHIELD`, in attesa | fatto, `ShieldSpellEffectTest` |
| `SCP_458` | SCP-458 · Pizza Senza Fine | Spell `SHIELD`, in attesa per due creature | fatto, `ShieldSpellEffectTest` |
| `SCP_109` | SCP-109 · Acqua Infinita | Spell `SHIELD`, in attesa, con un effetto collegato (`onDefenseCheck`, recupera 3 una volta) | fatto, `HealEffectsTest` |
| `CRACK_CALL` | Richiamo della Crepa (Call of the Rift) | Spell `BURN`, in attesa | fatto, `BurnSpellEffectTest` |
| `REVERSE_FUSION` | Fusione del Rovescio (Reverse Fusion) | Spell `BURN`, in attesa | fatto, `BurnSpellEffectTest` |
| `FORGOTTEN_ECHO` | Eco dei Dimenticati (Echo of the Forgotten) | Spell `BURN`, in attesa | fatto, `BurnSpellEffectTest` |
| `LAST_SANCTUARY` | Last Sanctuary | Hidden `SURVIVAL`, inizio scontro | fatto, `HiddenEffectsTest` |
| `SURVIVAL_INSTINCT` | Survival Instinct | Hidden `SURVIVAL`, inizio scontro | fatto, `HiddenEffectsTest` |
| `HOSTILE_PRESENCE` | Hostile Presence | Hidden `DAMAGE`, inizio scontro | fatto, `HiddenEffectsTest` |
| `ECHO_STRIKE` | Echo Strike | Hidden `DAMAGE`, inizio scontro | fatto, `HiddenEffectsTest` |
| `BREAKING_POINT` | Breaking Point | Hidden `DAMAGE`, inizio scontro | fatto, `HiddenEffectsTest` |
| `STATIC_INTERFERENCE` | Static Interference | Hidden `DAMAGE`, inizio scontro (sigillo di 5 fino a fine scontro, o 5 danni) | fatto, `BonusSealEffectsTest` |
| `ECHO_RIFT` | Echo Rift (Crepa Eco) | Terreno, bonus base; secondario `onWounded` (+1 ATK alla prima ferita di ogni scontro) | fatto, `TerrainServiceTest`, `TerrainSecondaryEffectsTest` |
| `HOLLOW_CLEARING` | Hollow Clearing (Crepa Radura) | Terreno, bonus base; secondario `reduceDefensePenalty` (ogni penalità DEF −1) | fatto, `TerrainServiceTest`, `TerrainSecondaryEffectsTest` |
| `WATCHING_RIFT` | Watching Rift (Crepa Occhi) | Terreno, bonus base; secondario `reduceDamageSpell` (prima Spell `DAMAGE` di ogni scontro −1) | fatto, `TerrainServiceTest`, `TerrainSecondaryEffectsTest` |
| `DEVOURING_ABYSS` | Devouring Abyss (Crepa Cavità) | Terreno, bonus base; secondario `onCombatStart` (1 danno a inizio scontro) | fatto, `TerrainServiceTest`, `TerrainSecondaryEffectsTest` |
| `DENIED_EXIT_BADGE` | Badge di Uscita Negata | Oggetto, controllo delle morti | fatto, `ItemServiceTest` |
| `FALSE_PATHS_COMPASS` | Bussola dei Sentieri Falsi | Oggetto, inizio scontro e inizio round | fatto, `ItemServiceTest` |
| `SEALED_FILE_ZERO` | Fascicolo Sigillato 0 | Oggetto, Spell, Hidden o effetto di creatura avversaria in arrivo | fatto, `ItemServiceTest`, `Scp2317EffectTest`, `SmilerEffectTest` |
| `TORII_BELL` | Campanello del Torii | Oggetto, Hidden avversaria in arrivo | fatto, `ItemServiceTest` |

Gli effetti delle creature uguali condividono una classe base (D-37): `FirstAttackShieldEffect` (Hidebehind, Nurikabe, Sala d'Attesa Vivente, e con altre parti le due Mutant, tramite `afterFirstAttack`), `EntryDamageEffect` (SCP-173, The Gnawing Witness), `EntryAndNextDamageEffect` (Cervo Bianco, Il Collezionista di Nomi, Cartellino Sbiadito, Rokurokubi), `FadingAttackPenaltyEffect` (SCP-049, Howlers, Noppera-bō, The Masked Bloom), `EntryDefenseEffect`; `EffectActions` scrive danni, modificatori e log nello stesso modo per tutti.
Yuki-onna usa un modificatore "attacco congelato" (`StatModifier.freezeAttack`, deciso il 18 novembre): finché è attivo l'attacco vale 0 dopo tutto il resto e `ATTACK_COMPUTED` riporta `frozen`. The Rootbound Doppelganger dà il suo −2 ATK all'inizio del round dopo il primo attacco dell'avversaria, per due round (deciso il 18 novembre). L'Ombra del Torii riconosce il Terreno della sua fazione dalle fazioni della carta del Terreno. Bigfoot e SCP-3000 riconoscono "Rare o superiore" con `RARE_ORDER = 3` (`rarityOrder` di Rare): va tenuto uguale al seed delle rarità dell'API (deciso il 18 novembre).
Le Spell di uno stesso sottotipo condividono una classe base (`DamageSpellEffect`, `ShieldSpellEffect`, `BurnSpellEffect`, D-37): ogni carta ha la sua classe con il testo della carta e i suoi numeri. SCP-109 ha una classe sua (`Scp109Effect`, `ENG-34`), perché collega un effetto alla creatura. Manca `SCP_500`: finché il suo effetto non esiste, la carta non entra nel catalogo.
I Terreni hanno il bonus base (`BaseTerrainEffect`) e, dal 18 novembre (`ENG-27`), l'effetto secondario. L'effetto sta nel Terreno in gioco (`PlacedTerrain.effect()`), una nuova istanza per ogni messa in gioco, che tiene lo stato "una volta per scontro" e riparte quando il Terreno è sostituito. Il motore lo chiama solo per le creature della sua fazione (`PlacedTerrain.affects`), di entrambi i giocatori, anche prima che ricevano il bonus:

- **Echo Rift:** al controllo della DEF, dopo l'`onWounded` della creatura, la prima ferita di ogni scontro dà +1 ATK fino a fine scontro, con origine `TERRAIN` (sparisce se il Terreno esce).
- **Hollow Clearing:** ogni penalità DEF è ridotta di 1. Tutti i danni che non sono attacchi passano da `Penalties` (`Penalties.defense`, `Penalties.apply` per i modificatori in attesa e le Spell `DAMAGE`): se vengono dall'avversario sono penalità, quindi effetti di creatura, Spell, Hidden, danni in attesa e Devouring Abyss; non gli attacchi né il sacrificio di The Thorned Matriarch. Breaking Point lascia la creatura a 1 DEF.
- **Watching Rift:** la prima Spell `DAMAGE` dell'avversario di ogni scontro perde un punto, prima dalla DEF, poi dall'ATK; una Spell annullata dal Fascicolo Sigillato 0 non lo consuma.
- **Devouring Abyss:** alla fine del passo 0 ogni creatura della fazione fa 1 danno all'avversaria, prima lo sfidante, con il controllo delle morti dopo ognuno; la ferita ha come causa la creatura (SCP-096 la marchia) e il Fascicolo Sigillato 0 non lo annulla.

"Ogni scontro" per Echo Rift e Watching Rift è `MatchState.combatWindow()`: lo scontro in corso o, fra due scontri, quello che sta per iniziare (deciso il 18 novembre). Una Spell pescata dopo una morte colpisce la sopravvissuta per il suo prossimo scontro.
Gli Oggetti dicono cosa fanno (`ItemEffect`) e `ItemService` li fa scattare nel loro momento; il Fascicolo Sigillato 0 annulla la prima Spell, Hidden o attivazione di un effetto di creatura avversaria che colpirebbe la creatura del proprietario. Valgono i testi del catalogo finché D-25 è aperta.
Le Hidden senza condizione hanno due classi base (`SurvivalHiddenEffect`, `FirstRoundHiddenEffect`); Breaking Point e Static Interference (`ENG-35`) hanno la loro. Mancano le Hidden `P2` con condizione: `REMNANTS_OF_REALITY`, `UNSEEN_EXIT` (`ENG-37`).

L'elenco di **tutti** gli effetti da implementare (creature, Spell, Hidden, Terreni, Oggetti), con codici proposti, momenti e note, è nel [catalogo degli effetti](catalogo-effetti.md).
Gli effetti sono stati decisi nella [revisione carta per carta](../bilanciamento/revisione-effetti.md) del 10 e 11 novembre: lì ci sono, per ogni carta, i dettagli di motore e le interazioni.
Le modifiche al motore che gli effetti richiedono (es. numero massimo di attivazioni, morti fuori dal round) sono nei [pezzi di motore](catalogo-effetti.md#pezzi-di-motore-richiesti-dagli-effetti) e nelle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md#effetti).
