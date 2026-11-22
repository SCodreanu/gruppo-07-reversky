# Fazioni

## Le fazioni sono dati

Le fazioni e i loro rapporti stanno nel database dell'API (tabelle `factions` e `faction_bonuses`) e si modificano dal backoffice. L'engine non ha database: **riceve fazioni e bonus in ogni richiesta**.

Per questo nel core una fazione **non** è un enum, ma un record:

```java
public record Faction(int id, String name) { }
```

## Le fazioni attuali

Ogni fazione è una delle quattro **Teorie** sull'origine della Pioggia Inversa (proposta al cliente, 2 novembre).
Nel gioco (database, carte, portale) i nomi sono **in inglese**; il nome italiano è solo quello usato nei documenti di lavoro.

| `id` negli esempi | Fazione (nome nel gioco) | Nome di lavoro | Ispirata a | Costante nei test | Terreno | Batte |
|---|---|---|---|---|---|---|
| 1 | Theory of the Wound | Teoria della Ferita | Backrooms | `WOUND` | Echo Rift | Theory of the Dream |
| 2 | Theory of the Invitation | Teoria dell'Invito | Monti Appalachi | `INVITATION` | Hollow Clearing | Theory of the Archive |
| 3 | Theory of the Archive | Teoria dell'Archivio | SCP | `ARCHIVE` | Watching Rift | Theory of the Wound |
| 4 | Theory of the Dream | Teoria del Sogno | Yokai | `DREAM` | Devouring Abyss | Theory of the Invitation |

Gli `id` veri sono quelli della tabella `factions` dell'API; quelli qui sopra sono usati negli [esempi](../../examples/README.md).
Le costanti inglesi servono solo nei test (`TestFactions`): nel core le fazioni restano dati.

## I bonus — `faction_bonuses`

Ogni riga dice: *quando una creatura di `origin` affronta una creatura di `target`, ottiene questi bonus.*

| Campo | Significato |
|---|---|
| `originFactionId` | fazione che riceve il bonus |
| `targetFactionId` | fazione affrontata |
| `atkBonus` | si somma all'attacco **in ogni round** dello scontro; **può essere negativo** (malus) |
| `defBonus` | si somma alla DEF **una volta**, all'inizio dello scontro; sempre ≥ 0 |

Vincoli (garantiti dal database): nessuna riga di una fazione contro se stessa, `defBonus` ≥ 0. Se una coppia non ha una riga, il bonus è 0.
L'attacco effettivo non scende mai sotto 0, anche con il malus.

### Valori (decisione D-04)

**+2 ATK** contro la fazione che si batte, **−1 ATK** contro quella che ti batte, 0 contro la fazione neutra.
Valori presentati al cliente il 2 novembre, da confermare con i test di bilanciamento (`ENG-28`).

| `origin` | `target` | `atkBonus` | `defBonus` |
|---|---|---|---|
| Theory of the Wound | Theory of the Dream | 2 | 0 |
| Theory of the Invitation | Theory of the Archive | 2 | 0 |
| Theory of the Archive | Theory of the Wound | 2 | 0 |
| Theory of the Dream | Theory of the Invitation | 2 | 0 |
| Theory of the Dream | Theory of the Wound | −1 | 0 |
| Theory of the Archive | Theory of the Invitation | −1 | 0 |
| Theory of the Wound | Theory of the Archive | −1 | 0 |
| Theory of the Invitation | Theory of the Dream | −1 | 0 |

Coppie neutre (nessuna riga): Wound–Invitation e Archive–Dream.

```
        Theory of the Archive ───batte───▶ Theory of the Wound
                 ▲                                │
                 │ batte                          │ batte
                 │                                ▼
        Theory of the Invitation ◀───batte─── Theory of the Dream
```

## Come si applica

- **ATK:** nel calcolo di ogni round, `attacco = max(0, ATK + bonus ATK di fazione + altri modificatori)`. Vale per entrambe le creature, ognuna con le sue righe.
- **DEF:** all'inizio di ogni scontro, `DEF += bonus DEF di fazione`. Se la creatura sopravvive e affronta una nuova avversaria, riceve il bonus relativo alla nuova (il precedente resta, come se fosse DEF in più).
- Le fazioni fanno parte del regolamento base richiesto dal brief.

### Esempio

```
Kappa (Theory of the Dream)     ATK 6  DEF 7
Hounds (Theory of the Wound)    ATK 9  DEF 8

Riga Wound → Dream: atkBonus +2    attacco Hounds = 9 + 2 = 11
Riga Dream → Wound: atkBonus −1    attacco Kappa  = 6 − 1 = 5
```

### Carte con due fazioni

Una carta può avere una **seconda fazione** facoltativa (`secondaryFactionId`, nel database `catalog_cards.secondary_faction_id`),
per esempio le due Mutant del catalogo: The Rootbound Doppelganger (Theory of the Invitation × Theory of the Dream) e The Stitched Corridor (Theory of the Wound × Theory of the Archive). Decisione D-22:
la carta prende **sia i vantaggi sia gli svantaggi** di entrambe le fazioni.

Regola: il bonus è la **somma su tutte le coppie** (fazione di chi attacca, fazione di chi difende). Vale per l'ATK e per la DEF,
e vale anche quando è l'avversaria ad attaccare la carta con due fazioni.

```
Rootbound Doppelganger (Dream + Invitation)  contro  Archive Puppet (Archive)
  Rootbound Doppelganger:  Dream → Archive 0   + Invitation → Archive +2   = +2
  Archive Puppet:          Archive → Dream 0   + Archive → Invitation −1   = −1

Rootbound Doppelganger (Dream + Invitation)  contro  Kodama (Dream)
  Rootbound Doppelganger:  Dream → Dream 0     + Invitation → Dream −1     = −1
  Kodama:                  Dream → Dream 0     + Dream → Invitation +2     = +2
```

Gli stessi casi, con il risultato completo, sono gli esempi `11` e `12` di [`examples/`](../../examples/README.md).

- Le due fazioni di una carta devono essere **diverse** (`400 INVALID_CARD_FACTIONS`).
- **Terreni:** una carta con due fazioni riceve il bonus del Terreno se **una delle due** è quella del Terreno (una volta sola).
- **Spell e Hidden** non hanno fazione: sono condivise e non hanno bonus di fazione.
- Due carte con due fazioni l'una contro l'altra: si sommano tutte e quattro le coppie.

## Nel codice

`FactionMatrix` viene costruita a ogni simulazione dai dati della richiesta. Lavora su **liste di fazioni** (una o due per carta):

```java
public final class FactionMatrix {

	private final Map<FactionPair, FactionBonus> bonuses;

	public FactionMatrix(List<FactionBonus> bonusRows) {
		this.bonuses = bonusRows.stream()
				.collect(Collectors.toMap(
						row -> new FactionPair(row.originFactionId(), row.targetFactionId()),
						Function.identity()));
	}

	/**
	 * Bonus ATK di una carta contro un'altra: somma delle righe di tutte le coppie
	 * (fazione di chi attacca, fazione di chi difende). Può essere negativo (malus).
	 */
	public int atkBonus(List<Integer> attackerFactions, List<Integer> defenderFactions) {
		int total = 0;
		for (int origin : attackerFactions) {
			for (int target : defenderFactions) {
				FactionBonus bonus = bonuses.get(new FactionPair(origin, target));
				total += bonus == null ? 0 : bonus.atkBonus();
			}
		}
		return total;
	}

	/** Bonus DEF, stessa regola di {@link #atkBonus}. */
	public int defBonus(List<Integer> ownFactions, List<Integer> opponentFactions) { ... }
}
```

`CardDefinition.factions()` restituisce `List.of(factionId)` oppure `List.of(factionId, secondaryFactionId)`: sempre in quest'ordine, così il calcolo è deterministico.

Le ricerche per chiave sulla mappa vanno bene; non si **itera** sulla mappa (l'ordine non è garantito, vedi [mescolamento](../mescolamento/mescolamento.md)).

## Test minimi

- Coppia con riga → bonus della riga (anche −1); coppia senza riga → 0; stessa fazione → 0.
- L'attacco effettivo con il malus non scende sotto 0 (creatura con ATK 0 e −1).
- Il bonus DEF si applica una sola volta per scontro, non a ogni round.
- Carta con due fazioni: somma di tutte le coppie, sia quando attacca sia quando viene attaccata (esempi `11` e `12`); due carte con due fazioni → quattro coppie.
- Riga con `originFactionId == targetFactionId` o `defBonus` negativo → `400 INVALID_FACTION_BONUS`; `atkBonus` negativo → accettato.
- `secondaryFactionId` uguale a `factionId` → `400 INVALID_CARD_FACTIONS`; `secondaryFactionId` non tra le `factions` → `400 UNKNOWN_FACTION`.
