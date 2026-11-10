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

Nel motore la rarità serve solo a qualche effetto (es. Bigfoot: "+2 contro carte di rarità superiore"; SCP-3000: "contro Keter, Thaumiel e Apollyon"). Corrispondenza con la classificazione della lore:

| Rarità | Classe della lore |
|---|---|
| Common | Safe |
| Uncommon | Euclid |
| Rare | Keter |
| Mythic | Thaumiel |
| Legendary | Apollyon |
| Mutant | — |
| Speciale (es. The Last Gift, edizione di Natale) | — (rarità da definire, D-23) |

La rarità **non** limita ATK e DEF: le carte personalizzate possono superare qualsiasi soglia (decisione del 9 novembre).

Una carta può avere **due fazioni** (es. la Mutant Ashen Kitsune: Theory of the Dream × Theory of the Invitation) e prende vantaggi e svantaggi di entrambe: vedi [fazioni](../fazioni/fazioni.md#carte-con-due-fazioni).

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
	private boolean effectDisabled;     // es. annullato da SCP-2521 o da The Rain Preacher
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
| `DAMAGE` | creatura avversaria in campo, subito | Bite of the Reverse (−3 ATK), Skin Fracture (−3 DEF), Dissolution (−2/−2) |
| `SHIELD` | la **prossima** propria creatura che entra in campo, sommata alle altre in attesa | Veil of the Anchored (+3 DEF), Registry Pact (+2 DEF/+1 ATK) |
| `BURN` | vedi sotto | Call of the Rift, Reverse Fusion, Echo of the Forgotten |

Regole proposte **[DA CONFERMARE]**:
- I modificatori delle Spell durano finché la creatura resta in campo (D-12).
- **Burn:** si rivela la carta successiva del mazzo. Se è una creatura, viene "fusa" con la creatura che il giocatore schiera: ATK e DEF si sommano (più l'eventuale bonus della Spell). Se non è una creatura, viene scartata.

### Hidden (`HIDDEN`)

Quando viene pescata viene messa **coperta** nella zona Hidden del giocatore. Si attiva da sola quando si verifica la sua condizione, poi viene scartata.

| Sottotipo | Effetto | Esempi |
|---|---|---|
| `SURVIVAL` | aiuta la propria creatura | Last Sanctuary (+4 DEF), Survival Instinct (+2/+2), Remnants of Reality |
| `DAMAGE` | indebolisce la creatura avversaria | Hostile Presence (−4 ATK), Echo Strike (−2/−2), Breaking Point |

Attivazione (D-15, decisa il 9 novembre): una Hidden si attiva all'inizio del **prossimo scontro** del proprietario (cioè quando una nuova coppia di creature si affronta). Le Hidden `DAMAGE` si risolvono prima delle `SURVIVAL`.

### Terreno (`TERRAIN`)

Quando viene pescato viene messo **accanto al mazzo** e resta in gioco **fino alla fine della partita**: non può essere distrutto né scartato. Dà un bonus passivo a **tutte** le creature della sua fazione in campo, comprese quelle dell'avversario (D-17, decisa il 9 novembre).

| Terreno | Fazione | Effetto base |
|---|---|---|
| Echo Rift (Crepa Eco) | Theory of the Wound | +1 ATK |
| Hollow Clearing (Crepa Radura) | Theory of the Invitation | +2 DEF |
| Watching Rift (Crepa Occhio) | Theory of the Archive | +1 ATK e +1 DEF |
| Devouring Abyss (Crepa Cavità) | Theory of the Dream | +1 ATK |

La fazione del Terreno è il suo `factionId`; l'effetto è il suo `effectCode`. Una creatura con due fazioni riceve il bonus se una delle due è quella del Terreno (una volta sola).

Il bonus DEF del Terreno si applica quando la creatura **entra in campo** (o subito, se è già in campo quando il Terreno viene pescato).

### Oggetto (`ITEM`)

Funziona **come un Terreno**: quando viene pescato viene messo accanto al mazzo e resta in gioco **fino alla fine della partita**, senza poter essere distrutto né scartato. Ha una fazione (`factionId`), un effetto passivo (`effectCode`, obbligatorio) e rarità Rare. Ogni mazzo ne contiene uno (lo controlla l'API) e ogni fazione ne ha uno nel catalogo.

**[DA CONFERMARE]** L'effetto di ciascuno dei 4 Oggetti (D-47) e l'ordine in cui si applicano Terreno e Oggetto quando sono entrambi in gioco (D-48). Implementazione: `ENG-33`.

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
