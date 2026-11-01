# Carte

## Come arriva una carta all'engine

L'engine non ha un catalogo: ogni carta arriva nella richiesta con tutti i dati che servono, presi da `catalog_cards` dell'API.

```java
public record CardDefinition(
		int cardId,
		String name,
		CardType type,          // CREATURE, SPELL, HIDDEN, TERRAIN (come l'enum del database)
		String rarity,          // nome della rarità
		int rarityOrder,        // rarities.sorting_order: più alto = più rara
		int factionId,          // tutte le carte hanno una fazione
		Integer atk,            // solo creature, altrimenti null
		Integer def,            // solo creature, altrimenti null
		CardSubtype subtype,    // Spell/Hidden: DAMAGE, SHIELD, BURN, SURVIVAL (dai tag); altrimenti null
		String effectCode) {    // dal tag EFFECT_<CODICE>, oppure null
}
```

Sottotipo ed effetto non sono colonne del database: l'API li ricava dai tag della carta (vedi `docs/database` nell'API).

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
| Mutant | — (da definire) |

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
	private boolean effectDisabled;     // es. annullato da SCP-2521 o dal Predicatore
	private int roundsInCombat;         // per effetti "nei primi 2 round"
	...
}
```

### Spell (`SPELL`)

Quando viene pescata, **il suo effetto si applica subito** in automatico e la carta viene **scartata**. Poi il giocatore continua a pescare.

| Sottotipo | Bersaglio | Esempi |
|---|---|---|
| `DAMAGE` | creatura avversaria in campo | Morso del Rovescio (−3 ATK), Frattura della Pelle (−3 DEF), Dissoluzione (−2/−2) |
| `SHIELD` | la **prossima** propria creatura che entra in campo | Velo degli Ancorati (+3 DEF), Patto del Registro (+2 DEF/+1 ATK) |
| `BURN` | vedi sotto | Richiamo della Crepa, Fusione del Rovescio, Eco dei Dimenticati |

Regole proposte **[DA CONFERMARE]**:
- Spell `DAMAGE` pescata quando l'avversario non ha una creatura in campo (inizio partita): si applica alla prossima creatura avversaria che entra.
- I modificatori delle Spell durano finché la creatura resta in campo.
- **Burn:** si rivela la carta successiva del mazzo. Se è una creatura, viene "fusa" con la creatura che il giocatore schiera: ATK e DEF si sommano (più l'eventuale bonus della Spell). Se non è una creatura, viene scartata.

### Hidden (`HIDDEN`)

Quando viene pescata viene messa **coperta** nella zona Hidden del giocatore. Si attiva da sola quando si verifica la sua condizione, poi viene scartata.

| Sottotipo | Effetto | Esempi |
|---|---|---|
| `SURVIVAL` | aiuta la propria creatura | Last Sanctuary (+4 DEF), Survival Instinct (+2/+2), Remnants of Reality |
| `DAMAGE` | indebolisce la creatura avversaria | Hostile Presence (−4 ATK), Echo Strike (−2/−2), Breaking Point |

Proposta di attivazione **[DA CONFERMARE]**: una Hidden si attiva all'inizio del **prossimo scontro** del proprietario (cioè quando una nuova coppia di creature si affronta). Le Hidden `DAMAGE` si risolvono prima delle `SURVIVAL`.

### Terreno (`TERRAIN`)

Quando viene pescato viene messo **accanto al mazzo** e resta in gioco **fino alla fine della partita**: non può essere distrutto né scartato. Dà un bonus passivo alle creature della sua fazione.

| Terreno | Fazione | Effetto base |
|---|---|---|
| Echo Rift (Crepa Eco) | Backrooms | +1 ATK |
| Hollow Clearing (Crepa Radura) | Monti Appalachi | +2 DEF |
| Watching Rift (Crepa Occhio) | SCP | +1 ATK e +1 DEF |
| Devouring Abyss (Crepa Cavità) | Yokai | +1 ATK |

La fazione del Terreno è il suo `factionId`; l'effetto è il suo `effectCode`.

**[DA CONFERMARE]**: il Terreno aiuta solo le creature **del proprietario** (proposta) o tutte le creature della fazione in campo?

Il bonus DEF del Terreno si applica quando la creatura **entra in campo** (o subito, se è già in campo quando il Terreno viene pescato).

## Validazione dell'input

L'engine **non** controlla le regole del mazzo (40 carte, copie…): lo fa l'API. Controlla solo che i dati siano coerenti:

- ogni mazzo ha almeno una creatura;
- le creature hanno ATK ≥ 0 e DEF > 0;
- ogni `factionId` è tra le `factions` ricevute;
- `effectCode`, se presente, esiste nell'`EffectRegistry`;
- Spell/Hidden hanno un `subtype`.

Se qualcosa non va → `400 Bad Request`.
