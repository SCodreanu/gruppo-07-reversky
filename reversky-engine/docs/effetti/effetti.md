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

	/** Inizio di ogni round (passo 1). */
	default void onRoundStart(CreatureContext ctx) { }

	/** Modifica l'attacco che questa creatura sta per infliggere (passo 2). */
	default int modifyOutgoingAttack(CreatureContext ctx, int attack) { return attack; }

	/** Modifica il danno dell'attacco che questa creatura sta per ricevere (passo 3). */
	default int modifyIncomingDamage(CreatureContext ctx, int damage) { return damage; }

	/** Dopo che i danni del round sono stati applicati, anche se la creatura è morta (passo 5). */
	default void afterDamage(CreatureContext ctx, int wound) { }

	/** Fine di ogni round, se la creatura è ancora viva (passo 6). */
	default void onRoundEnd(CreatureContext ctx) { }

	/** La creatura muore, prima di lasciare il campo (passo 7). */
	default void onDeath(CreatureContext ctx) { }
}
```

`code()` viene da `CardEffect`. `onKill` arriverà con il primo effetto che lo usa (The Bloodless Hound, Wendigo, La Regina dei Sentieri Perduti), con le sue regole.

`CreatureContext` dà accesso a: la creatura stessa (`self()`), la creatura avversaria in campo (`enemy()`, per leggerla), i due giocatori (`owner()`, `opponent()`), il round (`round()`) e il `MatchLog` (`log()`). Per **colpire** la creatura avversaria (danno o malus) un effetto usa `targetEnemy()`: è vuoto se l'avversaria non c'è o ha già DEF 0 o meno, e se un Fascicolo Sigillato 0 dell'avversario annulla l'attivazione (deciso il 18 novembre). Si crea un contesto per ogni chiamata, quindi il Fascicolo si controlla una volta per attivazione.

`afterDamage` riceve la **ferita**: la DEF persa davvero sopra 0, dopo gli scudi (`CreatureInPlay.takeDamage` la restituisce).

Ogni effetto tiene da sé lo stato che gli serve per "primo round", "una volta", "contro ogni avversaria" (D-31, deciso il 18 novembre): non c'è un contatore di attivazioni nel motore. I bonus ATK degli effetti si danno come `StatModifier` con origine `EFFECT` e una durata, non in `modifyOutgoingAttack` (deciso il 18 novembre): così compaiono nell'ATK e nel log e gli effetti che sospendono i bonus ATK (Static Interference, L'Addetto alle Uscite, Deathmoths) li vedono. `modifyOutgoingAttack` resta per i casi che dipendono dall'avversaria del momento; la sua modifica compare in `ATTACK_COMPUTED` come `effectBonus`.

L'istanza dell'effetto sta nella creatura (`CreatureInPlay.effect()`): `DrawService` la crea dal registro quando la creatura entra in campo. `CreatureEffectService` chiama i momenti nei passi del round; `DeathResolver` chiama `onDeath`.

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
     - onEnterPlay delle creature che non sono ancora entrate (una volta per entrata, D-31), poi gli effetti che
       guardano l'entrata di una creatura avversaria (Mothman, The Stitched Corridor…); se una Hidden ha ucciso, chi
       non è entrato entra allo scontro dopo (deciso il 18 novembre)
     - si fissa la DEF all'entrata delle creature appena entrate (M-06)
     - effetti secondari dei Terreni "a inizio scontro" (Devouring Abyss)
1. Scudo della Bussola dei Sentieri Falsi (nei primi tre round del suo scontro), poi onRoundStart
2. Calcolo attacco: ATK base + bonus ATK di fazione + modificatori → modifyOutgoingAttack → minimo 0
3. Calcolo danno ricevuto: modifyIncomingDamage della creatura colpita
4. Applicazione SIMULTANEA dei danni
5. afterDamage
6. onRoundEnd (solo creature con DEF > 0)
7. Morti: onDeath / onKill, poi danno al giocatore = DEF negativa della sconfitta (vedi regole-base)
```

Dopo **ogni** passo si controllano le morti con le regole del passo 7 (D-08), con il `DeathResolver`: una creatura uccisa da un danno da effetto prima del passo 4 non attacca.
Se una creatura muore prima del passo 4, il round si interrompe: **il round conta** (in `totalRounds` e per il limite di round, come il suo `ROUND_STARTED` nel log), nessuno attacca e i modificatori a round della sopravvissuta scadono come a fine round (deciso il 17 novembre).
Lo stesso controllo vale fuori dai round: una Spell pescata o una Hidden all'inizio dello scontro possono uccidere, e la morte ha le stesse conseguenze (danno al giocatore, creatura sconfitta, fine dello scontro). Se all'inizio dello scontro resta una sola creatura, il round non si gioca e chi ha il campo vuoto pesca di nuovo.

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

Il danno può uccidere anche l'avversaria: le morti si ricontrollano dopo gli `onDeath`, quindi possono morire **entrambe** (e in quel caso nessun giocatore subisce danni). Nel log `CREATURE_DIED` della creatura viene prima del suo effetto alla morte.
Il +1 ATK va nei modificatori in attesa del proprietario (D-34) e si applica alla sua prossima creatura che entra.

## Effetti di Spell, Hidden, Terreni e Oggetti

Le carte che non sono creature hanno un'interfaccia per tipo, sotto l'interfaccia chiusa `CardEffect` (in `core/effect`), con solo i momenti che servono a quel tipo (ENG-22):

| Interfaccia | Momento | Chi la chiama |
|---|---|---|
| `SpellEffect` | `resolve(ctx)`: una volta, quando la Spell viene pescata, poi la carta si scarta | `ENG-19` |
| `HiddenEffect` | `trigger(ctx)`: una volta, quando la Hidden si attiva, poi la carta si scarta | `ENG-20` |
| `TerrainEffect` | `attackBonus()` e `defenseBonus()`: il bonus base, applicato dal motore alle creature della fazione | `ENG-21` |
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

Una creatura può non avere codice (le Common): allora gioca senza effetto. Se ha un codice, deve essere registrato come effetto di creatura, come per le altre carte (`UNKNOWN_EFFECT`, deciso il 18 novembre): gli effetti delle creature non ancora implementati danno `400` finché non arrivano (`ENG-25`, `ENG-26`), e intanto l'API lascia vuoto `effect_code`.

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
| `SCP_096` | SCP-096 (Shy Guy) | creatura, `afterDamage` (+3 ATK fino a fine scontro) | fatto, `Scp096EffectTest` |
| `SCP_2317` | SCP-2317 (A Door in the Woods) | creatura, `onDeath` | fatto, `Scp2317EffectTest` |
| `SMILER` | Smiler | creatura, `onRoundEnd` | fatto, `SmilerEffectTest` |
| `BITE_OF_REVERSE` | Morso del Rovescio (Bite of the Reverse) | Spell `DAMAGE`, alla pescata | fatto, `DamageSpellEffectTest` |
| `SKIN_FRACTURE` | Frattura della Pelle (Skin Fracture) | Spell `DAMAGE`, alla pescata | fatto, `DamageSpellEffectTest` |
| `DISSOLUTION` | Dissoluzione (Dissolution) | Spell `DAMAGE`, alla pescata | fatto, `DamageSpellEffectTest` |
| `VEIL_OF_ANCHORED` | Velo degli Ancorati (Veil of the Anchored) | Spell `SHIELD`, in attesa | fatto, `ShieldSpellEffectTest` |
| `REGISTRY_PACT` | Patto del Registro (Registry Pact) | Spell `SHIELD`, in attesa | fatto, `ShieldSpellEffectTest` |
| `RAIN_ARMOR` | Armatura della Pioggia (Rain Armor) | Spell `SHIELD`, in attesa | fatto, `ShieldSpellEffectTest` |
| `SCP_458` | SCP-458 · Pizza Senza Fine | Spell `SHIELD`, in attesa per due creature | fatto, `ShieldSpellEffectTest` |
| `CRACK_CALL` | Richiamo della Crepa (Call of the Rift) | Spell `BURN`, in attesa | fatto, `BurnSpellEffectTest` |
| `REVERSE_FUSION` | Fusione del Rovescio (Reverse Fusion) | Spell `BURN`, in attesa | fatto, `BurnSpellEffectTest` |
| `FORGOTTEN_ECHO` | Eco dei Dimenticati (Echo of the Forgotten) | Spell `BURN`, in attesa | fatto, `BurnSpellEffectTest` |
| `LAST_SANCTUARY` | Last Sanctuary | Hidden `SURVIVAL`, inizio scontro | fatto, `HiddenEffectsTest` |
| `SURVIVAL_INSTINCT` | Survival Instinct | Hidden `SURVIVAL`, inizio scontro | fatto, `HiddenEffectsTest` |
| `HOSTILE_PRESENCE` | Hostile Presence | Hidden `DAMAGE`, inizio scontro | fatto, `HiddenEffectsTest` |
| `ECHO_STRIKE` | Echo Strike | Hidden `DAMAGE`, inizio scontro | fatto, `HiddenEffectsTest` |
| `BREAKING_POINT` | Breaking Point | Hidden `DAMAGE`, inizio scontro | fatto, `HiddenEffectsTest` |
| `ECHO_RIFT` | Echo Rift (Crepa Eco) | Terreno, bonus base | fatto, `TerrainServiceTest` |
| `HOLLOW_CLEARING` | Hollow Clearing (Crepa Radura) | Terreno, bonus base | fatto, `TerrainServiceTest` |
| `WATCHING_RIFT` | Watching Rift (Crepa Occhi) | Terreno, bonus base | fatto, `TerrainServiceTest` |
| `DEVOURING_ABYSS` | Devouring Abyss (Crepa Cavità) | Terreno, bonus base | fatto, `TerrainServiceTest` |
| `DENIED_EXIT_BADGE` | Badge di Uscita Negata | Oggetto, controllo delle morti | fatto, `ItemServiceTest` |
| `FALSE_PATHS_COMPASS` | Bussola dei Sentieri Falsi | Oggetto, inizio scontro e inizio round | fatto, `ItemServiceTest` |
| `SEALED_FILE_ZERO` | Fascicolo Sigillato 0 | Oggetto, Spell, Hidden o effetto di creatura avversaria in arrivo | fatto, `ItemServiceTest`, `Scp2317EffectTest`, `SmilerEffectTest` |
| `TORII_BELL` | Campanello del Torii | Oggetto, Hidden avversaria in arrivo | fatto, `ItemServiceTest` |

Le Spell di uno stesso sottotipo condividono una classe base (`DamageSpellEffect`, `ShieldSpellEffect`, `BurnSpellEffect`, D-37): ogni carta ha la sua classe con il testo della carta e i suoi numeri. Mancano `SCP_500` e `SCP_109` (`P2`): finché il loro effetto non esiste, le carte non entrano nel catalogo.
I Terreni hanno solo il bonus base (`BaseTerrainEffect`); gli effetti secondari arrivano con `ENG-27`.
Gli Oggetti dicono cosa fanno (`ItemEffect`) e `ItemService` li fa scattare nel loro momento; il Fascicolo Sigillato 0 annulla la prima Spell, Hidden o attivazione di un effetto di creatura avversaria che colpirebbe la creatura del proprietario. Valgono i testi del catalogo finché D-25 è aperta.
Le Hidden senza condizione hanno due classi base (`SurvivalHiddenEffect`, `FirstRoundHiddenEffect`); Breaking Point ha la sua. Mancano le Hidden `P2`: `REMNANTS_OF_REALITY`, `UNSEEN_EXIT` (con condizione) e `STATIC_INTERFERENCE`.

L'elenco di **tutti** gli effetti da implementare (creature, Spell, Hidden, Terreni, Oggetti), con codici proposti, momenti e note, è nel [catalogo degli effetti](catalogo-effetti.md).
Gli effetti sono stati decisi nella [revisione carta per carta](../bilanciamento/revisione-effetti.md) del 10 e 11 novembre: lì ci sono, per ogni carta, i dettagli di motore e le interazioni.
Le modifiche al motore che gli effetti richiedono (es. numero massimo di attivazioni, morti fuori dal round) sono nei [pezzi di motore](catalogo-effetti.md#pezzi-di-motore-richiesti-dagli-effetti) e nelle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md#effetti).
