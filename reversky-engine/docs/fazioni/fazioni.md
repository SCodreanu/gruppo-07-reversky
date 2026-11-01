# Fazioni

## Le fazioni sono dati

Le fazioni e i loro rapporti stanno nel database dell'API (tabelle `factions` e `faction_bonus`) e si modificano dal backoffice. L'engine non ha database: **riceve fazioni e bonus in ogni richiesta**.

Per questo nel core una fazione **non** è un enum, ma un record:

```java
public record Faction(int id, String name) { }
```

## Le fazioni attuali

| Fazione | Teoria | Terreno |
|---|---|---|
| Backrooms | Teoria della Ferita | Crepa Eco |
| Monti Appalachi | Teoria dell'Invito | Crepa Radura |
| SCP | Teoria dell'Archivio | Crepa Occhio |
| Yokai | Teoria del Sogno | Crepa Cavità |

## I bonus — `faction_bonus`

Ogni riga dice: *quando una creatura di `origin` affronta una creatura di `target`, ottiene questi bonus.*

| Campo | Significato |
|---|---|
| `originFactionId` | fazione che riceve il bonus |
| `targetFactionId` | fazione affrontata |
| `atkBonus` | si somma all'attacco **in ogni round** dello scontro |
| `defBonus` | si somma alla DEF **una volta**, all'inizio dello scontro |

Vincoli (garantiti dal database): bonus sempre **≥ 0**, nessuna riga di una fazione contro se stessa. Se una coppia non ha una riga, il bonus è 0.

> Il regolamento v1.0 prevedeva anche un malus (−1 ATK) per la fazione debole. Lo schema ammette solo bonus positivi, quindi il malus non esiste: lo svantaggio è "subire il bonus dell'altro".

### Dati proposti

Ciclo: ogni fazione è forte contro una e neutra contro le altre.

| `origin` | `target` | `atkBonus` | `defBonus` |
|---|---|---|---|
| Monti Appalachi | SCP | 2 | 0 |
| SCP | Backrooms | 2 | 0 |
| Backrooms | Yokai | 2 | 0 |
| Yokai | Monti Appalachi | 2 | 0 |

```
 SCP ───forte contro───▶ Backrooms
  ▲                          │
  │ forte contro             │ forte contro
  │                          ▼
Monti Appalachi ◀──forte contro── Yokai
```

## Come si applica

- **ATK:** nel calcolo di ogni round, `attacco = ATK + atkBonus(origine, bersaglio)`. Vale per entrambe le creature, ognuna con la sua riga.
- **DEF:** all'inizio di ogni scontro, `DEF += defBonus(origine, bersaglio)`. Se la creatura sopravvive e affronta una nuova avversaria, riceve il bonus relativo alla nuova (il precedente resta, come se fosse DEF in più).
- Le fazioni fanno parte del regolamento base richiesto dal brief.

### Esempio

```
Kappa (Yokai)        ATK 6  DEF 7
Hounds (Backrooms)   ATK 9  DEF 8

Riga Backrooms → Yokai: atkBonus 2
  attacco Hounds = 9 + 2 = 11
  attacco Kappa  = 6 + 0 = 6
```

## Nel codice

`FactionMatrix` viene costruita a ogni simulazione dai dati della richiesta:

```java
public final class FactionMatrix {

	private final Map<FactionPair, FactionBonus> bonuses;

	public FactionMatrix(List<FactionBonus> bonusRows) {
		this.bonuses = bonusRows.stream()
				.collect(Collectors.toMap(
						row -> new FactionPair(row.originFactionId(), row.targetFactionId()),
						Function.identity()));
	}

	/** Bonus ATK di {@code origin} contro {@code target}; 0 se non c'è una riga. */
	public int atkBonus(int origin, int target) {
		FactionBonus bonus = bonuses.get(new FactionPair(origin, target));
		return bonus == null ? 0 : bonus.atkBonus();
	}

	/** Bonus DEF di {@code origin} contro {@code target}; 0 se non c'è una riga. */
	public int defBonus(int origin, int target) { ... }
}
```

Le ricerche per chiave sulla mappa vanno bene; non si **itera** sulla mappa (l'ordine non è garantito, vedi [mescolamento](../mescolamento/mescolamento.md)).

## Test minimi

- Coppia con riga → bonus della riga; coppia senza riga → 0; stessa fazione → 0.
- Il bonus DEF si applica una sola volta per scontro, non a ogni round.
- Riga con `originFactionId == targetFactionId` o bonus negativo nella richiesta → `400`.
