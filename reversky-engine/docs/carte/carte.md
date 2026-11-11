# Carte

## Come arriva una carta all'engine

L'engine non ha un catalogo: ogni carta arriva nella richiesta con tutti i dati che servono, presi da `catalog_cards` dell'API.

```java
public record CardDefinition(
		int cardId,
		String name,
		CardType type,          // CREATURE, SPELL, HIDDEN, TERRAIN, ITEM (come l'enum del database)
		String rarity,          // nome della rarità
		int rarityOrder,        // rarities.sorting_order: più alto = più rara
		Integer factionId,      // Creature, Terreni e Oggetti; null per Spell e Hidden, che sono condivise
		Integer secondaryFactionId, // seconda fazione facoltativa (es. Mutant), altrimenti null
		Integer atk,            // solo creature, altrimenti null
		Integer def,            // solo creature, altrimenti null
		CardSubtype subtype,    // Spell/Hidden: DAMAGE, SHIELD, BURN, SURVIVAL; altrimenti null
		String effectCode) {    // catalog_cards.effect_code, oppure null
}
```

Sottotipo ed effetto sono colonne di `catalog_cards` (`subtype`, `effect_code`): vedi `docs/database` nell'API.

Un mazzo è una `List<CardDefinition>` con **un elemento per copia**.

## Rarità

Le rarità sono dati (tabella `rarities`): l'engine riceve nome e `rarityOrder` e confronta le rarità **solo tramite `rarityOrder`**, mai tramite il nome.

Nel motore la rarità serve solo a qualche effetto (Bigfoot: "contro Rare o superiori +4 ATK e 2 danni in meno da ogni loro attacco, contro Common e Uncommon +2 ATK"; SCP-3000: "contro Rare o superiori subisce metà dei danni dai loro attacchi"; SCP-2521: ruba l'effetto solo a una Rare o inferiore; La Regina dei Sentieri Perduti: sposta in fondo al mazzo avversario la creatura di rarità più alta). Corrispondenza con la classificazione della lore:

| Rarità | Classe della lore |
|---|---|
| Common | Safe |
| Uncommon | Euclid |
| Rare | Keter |
| Mythic | Thaumiel |
| Legendary | Apollyon |
| Mutant | — |
| Speciale (carte evento fuori dal catalogo di 120, es. The Last Gift) | — (rarità da definire, D-23) |

La rarità **non** limita ATK e DEF: le carte personalizzate possono superare qualsiasi soglia (decisione del 9 novembre).

Una carta può avere **due fazioni** (le due Mutant del catalogo: The Rootbound Doppelganger, Theory of the Invitation × Theory of the Dream; The Stitched Corridor, Theory of the Wound × Theory of the Archive) e prende vantaggi e svantaggi di entrambe: vedi [fazioni](../fazioni/fazioni.md#carte-con-due-fazioni).

## Tipi di carta

### Creatura (`CREATURE`)

Ha ATK, DEF, fazione (per i bonus di fazione) e un eventuale effetto.

In campo diventa un `CreatureInPlay`:

```java
public final class CreatureInPlay {
	private final CardDefinition card;
	private final int initialDefense;   // serve agli effetti "quando scende al 50%"
	private int defense;                // DEF attuale
	private final List<StatModifier> attackModifiers;
	private CreatureEffect effect;      // null se la carta non ha effetto
	private boolean effectDisabled;     // es. annullato da SCP-2521 o da Il Predicatore sotto la Pioggia (finché resta in campo)
	private int roundsInCombat;         // per effetti "nei primi 2 round"
	...
}
```

### Spell (`SPELL`)

Quando viene pescata si risolve in automatico, poi la carta viene **scartata** e il giocatore continua a pescare (D-10, deciso il 9 novembre):

- le Spell che potenziano la propria creatura (`SHIELD`) **restano in attesa** finché entra in campo la prossima creatura del proprietario. Se prima ne vengono pescate altre, **si sommano tutte** su quella creatura (modificatori in attesa nel `PlayerState`, D-34);
- le Spell `DAMAGE` colpiscono **subito** la creatura avversaria in campo. Se l'avversario non ne ha (inizio partita), si applicano alla prossima creatura avversaria che entra (D-11).

| Sottotipo | Bersaglio | Esempi |
|---|---|---|
| `DAMAGE` (3) | creatura avversaria in campo, subito | Morso del Rovescio (−3 ATK), Frattura della Pelle (−5 DEF), Dissoluzione (−2/−2) |
| `SHIELD` (6) | la **prossima** propria creatura che entra in campo, sommata alle altre in attesa | Velo degli Ancorati (+5 DEF), Patto del Registro (+2 DEF/+1 ATK), SCP-500 (primo danno −4), SCP-458 (+2 DEF a due creature di fila) |
| `BURN` (3) | la prossima carta del mazzo, se è una creatura (vedi sotto) | Richiamo della Crepa, Fusione del Rovescio, Eco dei Dimenticati |

Le 12 Spell sono nel [catalogo degli effetti](../effetti/catalogo-effetti.md#spell). Una Spell si pesca solo quando la propria creatura è morta: quando il catalogo dice "creatura attiva" si intende la **prossima** creatura del proprietario.

- I modificatori delle Spell durano finché la creatura che li riceve resta in campo, anche negli scontri successivi (D-12, deciso il 10 novembre).
- **Burn** (D-13, decisa l'11 novembre, *Nessuna carta persa*): si rivela la prossima carta del mazzo del proprietario. Se è una creatura, è proprio la prossima creatura che verrà pescata ed **entra con il bonus fisso** della Burn (modificatore in attesa, D-34); se non è una creatura **non si scarta nulla** e la carta resta al suo posto; con il mazzo vuoto non succede nulla. Evento di log `BURN_REVEALED`.

### Hidden (`HIDDEN`)

Quando viene pescata viene messa **coperta** nella zona Hidden del giocatore. Si attiva da sola quando si verifica la sua condizione, poi viene scartata.

| Sottotipo | Effetto | Esempi |
|---|---|---|
| `SURVIVAL` (4) | aiuta la propria creatura | Last Sanctuary (+5 DEF), Survival Instinct (+2 ATK/+1 DEF), Remnants of Reality, Unseen Exit |
| `DAMAGE` (4) | indebolisce la creatura avversaria | Hostile Presence (−5 ATK nel primo round), Echo Strike (−2/−2), Breaking Point, Static Interference |

Attivazione (D-15, decisa il 9 novembre e modificata il 10):
- una Hidden **con una condizione** (Remnants of Reality: quando la propria creatura riceve una penalità; Unseen Exit: quando un attacco ucciderebbe la propria creatura) resta coperta finché la condizione si verifica, nel passo del round in cui si verifica;
- le altre si attivano all'inizio del **prossimo scontro** del proprietario (cioè quando una nuova coppia di creature si affronta), contro la creatura avversaria in campo in quel momento;
- una Hidden pescata nella pescata iniziale **non si attiva nel primo scontro** ("non nel primo turno" del catalogo, regola adottata l'11 novembre);
- se più Hidden si attivano nello stesso momento, le `DAMAGE` si risolvono prima delle `SURVIVAL`.

Durate: "nello scontro corrente" = finché una delle due creature muore; "prima del calcolo dello scontro" (Hidden `DAMAGE`) = il malus ATK vale solo il primo round, il danno alla DEF è permanente. Dettagli nel [catalogo degli effetti](../effetti/catalogo-effetti.md#hidden).

### Terreno (`TERRAIN`)

Quando viene pescato viene messo **accanto al mazzo** e resta in gioco finché un giocatore (lui o l'avversario) pesca un altro Terreno: in gioco c'è **un solo Terreno** alla volta, e quello nuovo sostituisce il precedente (D-19, decisa il 10 novembre). Può anche essere tolto da un effetto: The Null Witness lo rimuove se l'ha pescato l'avversario, The Hollow Wyrm lo **inghiotte** chiunque l'abbia pescato (anche un Terreno pescato mentre il Wyrm è in campo, che allora non si piazza). Evento di log `TERRAIN_REMOVED`, con la causa. Dà un bonus passivo a **tutte** le creature della sua fazione in campo, comprese quelle dell'avversario (D-17, decisa il 9 novembre).

| Terreno | Fazione | Effetto base | Effetto secondario |
|---|---|---|---|
| Echo Rift (Crepa Eco) | Theory of the Wound | +1 ATK | la prima volta che ogni creatura viene ferita in uno scontro, +1 ATK per il resto dello scontro |
| Hollow Clearing (Crepa Radura) | Theory of the Invitation | +2 DEF | ogni penalità DEF ricevuta è ridotta di 1 |
| Watching Rift (Crepa Occhi) | Theory of the Archive | +1 ATK e +1 DEF | la prima Spell `DAMAGE` subita in uno scontro fa 1 punto in meno (prima dalla DEF, poi dall'ATK) |
| Devouring Abyss (Crepa Cavità) | Theory of the Dream | +1 ATK | all'inizio di ogni scontro 1 danno alla creatura avversaria (danno da effetto) |

I quattro Terreni valgono circa 3,5 PS per creatura della loro fazione (revisione dell'11 novembre). Gli effetti secondari sono decisi e si implementano dopo il bonus base (D-18).

Quando un Terreno esce (sostituito, rimosso o inghiottito), i suoi bonus si tolgono: l'ATK subito, la DEF con la regola dello scudo (si toglie solo la parte non ancora consumata dai danni, D-35), quindi l'uscita di un Terreno non uccide mai (D-19, deciso l'11 novembre).

La fazione del Terreno è il suo `factionId`; l'effetto è il suo `effectCode`. Una creatura con due fazioni riceve il bonus se una delle due è quella del Terreno (una volta sola).

Il bonus DEF del Terreno si applica quando la creatura **entra in campo** (o subito, se è già in campo quando il Terreno viene pescato).

### Oggetto (`ITEM`)

Quando viene pescato viene messo **accanto al mazzo**, come un Terreno, e non può essere distrutto né scartato da altre carte. È **monouso** (D-47, decisa il 10 novembre): resta finché il suo effetto scatta una volta, sulla prima creatura del proprietario che ne soddisfa la condizione, poi si consuma. Ha una fazione (`factionId`), un effetto (`effectCode`, obbligatorio) e rarità Rare. Ogni mazzo ne contiene uno (lo controlla l'API) e ogni fazione ne ha uno nel catalogo.

| Oggetto | Fazione | Effetto (revisione dell'11 novembre) |
|---|---|---|
| Badge di Uscita Negata | Theory of the Wound | la prima volta che la creatura del proprietario morirebbe, resta con 1 DEF |
| Bussola dei Sentieri Falsi | Theory of the Invitation | uno scudo di 2 DEF all'inizio di ognuno dei primi tre round del prossimo scontro |
| Fascicolo Sigillato 0 | Theory of the Archive | annulla del tutto il primo effetto di creatura, Spell o Hidden avversaria che colpirebbe la creatura del proprietario |
| Campanello del Torii | Theory of the Dream | annulla la prima Hidden nemica che starebbe per attivarsi |

La fazione dell'Oggetto non limita il suo effetto: vale per qualunque creatura del proprietario.

Quando scattano insieme all'entrata, prima il Terreno e poi l'Oggetto (D-48, deciso l'11 novembre). Implementazione: `ENG-33`.

### Carte condivise

Spell e Hidden **non hanno fazione** (`factionId` e `secondaryFactionId` sono `null`): sono condivise da tutte le fazioni e non ricevono né danno bonus di fazione.

## Validazione dell'input

L'engine **non** controlla le regole del mazzo (numero di carte, copie…): lo fa l'API. Controlla solo che i dati siano coerenti:

- ogni mazzo ha almeno una creatura;
- le creature hanno ATK ≥ 0 e DEF > 0;
- Creature, Terreni e Oggetti hanno un `factionId`; Spell e Hidden non hanno né `factionId` né `secondaryFactionId`;
- ogni `factionId` e `secondaryFactionId` è tra le `factions` ricevute, e le due fazioni di una carta sono diverse;
- `effectCode`, se presente, esiste nell'`EffectRegistry`;
- Spell/Hidden hanno un `subtype`;
- Spell, Hidden, Terreni e Oggetti hanno un `effectCode` (per le creature è facoltativo).

Se qualcosa non va → `400 Bad Request`.
