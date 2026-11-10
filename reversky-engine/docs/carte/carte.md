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

Nel motore la rarità serve solo a qualche effetto (Bigfoot: "+2 ATK contro Rare o superiori, +1 DEF contro Common e Uncommon"; SCP-3000: "+3 ATK contro Rare o superiori"). Corrispondenza con la classificazione della lore:

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
	private boolean effectDisabled;     // es. annullato da SCP-2521 o da Il Predicatore sotto la Pioggia
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
| `DAMAGE` (3) | creatura avversaria in campo, subito | Morso del Rovescio (−3 ATK), Frattura della Pelle (−3 DEF), Dissoluzione (−2/−2) |
| `SHIELD` (6) | la **prossima** propria creatura che entra in campo, sommata alle altre in attesa | Velo degli Ancorati (+3 DEF), Patto del Registro (+2 DEF/+1 ATK), SCP-500 (primo attacco −4 danni) |
| `BURN` (3) | vedi sotto | Richiamo della Crepa, Fusione del Rovescio, Eco dei Dimenticati |

Le 12 Spell sono nel [catalogo degli effetti](../effetti/catalogo-effetti.md#spell). Una Spell si pesca solo quando la propria creatura è morta: quando il catalogo dice "creatura attiva" (SCP-109, SCP-458, Burn) si intende la **prossima** creatura del proprietario.

- I modificatori delle Spell durano finché la creatura che li riceve resta in campo, anche negli scontri successivi (D-12, deciso il 10 novembre).
- **[DA CONFERMARE] Burn** (testo del catalogo): si rivela la prossima carta del mazzo. Se è una creatura, la sua ATK e la sua DEF si aggiungono **temporaneamente** alla creatura del giocatore (più l'eventuale bonus della Spell); se è di un altro tipo viene scartata; con il mazzo vuoto non succede nulla. Restano da decidere se la creatura rivelata viene tolta dal mazzo e quanto dura il "temporaneamente" (D-13).

### Hidden (`HIDDEN`)

Quando viene pescata viene messa **coperta** nella zona Hidden del giocatore. Si attiva da sola quando si verifica la sua condizione, poi viene scartata.

| Sottotipo | Effetto | Esempi |
|---|---|---|
| `SURVIVAL` (4) | aiuta la propria creatura | Last Sanctuary (+4 DEF), Survival Instinct (+2/+2), Remnants of Reality, Unseen Exit |
| `DAMAGE` (4) | indebolisce la creatura avversaria | Hostile Presence (−4 ATK), Echo Strike (−2/−2), Breaking Point, Static Interference |

Attivazione (D-15, decisa il 9 novembre e modificata il 10):
- una Hidden **con una condizione** (Unseen Exit, Static Interference) resta coperta finché la condizione si verifica, nel passo del round in cui si verifica;
- le altre si attivano all'inizio del **prossimo scontro** del proprietario (cioè quando una nuova coppia di creature si affronta);
- se più Hidden si attivano nello stesso momento, le `DAMAGE` si risolvono prima delle `SURVIVAL`.

Il "non nel primo turno" del catalogo e la condizione esatta di ogni Hidden si fissano nella revisione degli effetti ([catalogo degli effetti](../effetti/catalogo-effetti.md#hidden)).

### Terreno (`TERRAIN`)

Quando viene pescato viene messo **accanto al mazzo** e resta in gioco finché un giocatore (lui o l'avversario) pesca un altro Terreno: in gioco c'è **un solo Terreno** alla volta, e quello nuovo sostituisce il precedente (D-19, decisa il 10 novembre). Dà un bonus passivo a **tutte** le creature della sua fazione in campo, comprese quelle dell'avversario (D-17, decisa il 9 novembre).

| Terreno | Fazione | Effetto base |
|---|---|---|
| Echo Rift (Crepa Eco) | Theory of the Wound | +1 ATK |
| Hollow Clearing (Crepa Radura) | Theory of the Invitation | +2 DEF |
| Watching Rift (Crepa Occhi) | Theory of the Archive | +1 ATK e +1 DEF |
| Devouring Abyss (Crepa Cavità) | Theory of the Dream | +1 ATK |

Ogni Terreno ha anche un effetto secondario, elencato nel [catalogo degli effetti](../effetti/catalogo-effetti.md#terreni): si rivede ora con le carte e si implementa dopo il bonus base (D-18).

**[DA CONFERMARE]** Quando un Terreno viene sostituito, i suoi bonus si tolgono: l'ATK subito, la DEF con la regola dello scudo (si toglie solo la parte non ancora consumata dai danni, D-35).

La fazione del Terreno è il suo `factionId`; l'effetto è il suo `effectCode`. Una creatura con due fazioni riceve il bonus se una delle due è quella del Terreno (una volta sola).

Il bonus DEF del Terreno si applica quando la creatura **entra in campo** (o subito, se è già in campo quando il Terreno viene pescato).

### Oggetto (`ITEM`)

Quando viene pescato viene messo **accanto al mazzo**, come un Terreno, e non può essere distrutto né scartato da altre carte. È **monouso** (D-47, decisa il 10 novembre): resta finché il suo effetto scatta una volta, sulla prima creatura del proprietario che ne soddisfa la condizione, poi si consuma. Ha una fazione (`factionId`), un effetto (`effectCode`, obbligatorio) e rarità Rare. Ogni mazzo ne contiene uno (lo controlla l'API) e ogni fazione ne ha uno nel catalogo.

| Oggetto | Fazione | Effetto (catalogo) |
|---|---|---|
| Badge di Uscita Negata | Theory of the Wound | previene 2 danni dal primo attacco, poi si consuma |
| Bussola dei Sentieri Falsi | Theory of the Invitation | +2 DEF per il primo round, poi si consuma |
| Fascicolo Sigillato 0 | Theory of the Archive | annulla fino a 2 punti della prima penalità ATK/DEF, poi si consuma |
| Campanello del Torii | Theory of the Dream | alla prima Hidden avversaria attivata +2 DEF, poi si consuma |

**[DA CONFERMARE]** L'ordine rispetto al Terreno quando scattano insieme all'entrata (D-48). Implementazione: `ENG-33`.

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
