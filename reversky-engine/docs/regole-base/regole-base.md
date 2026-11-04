# Regole base

Questo documento traduce il regolamento base del brief (obbligatorio) e le scelte del team in **passi precisi** per il codice.
Esiste **un solo regolamento** (decisione concordata con il cliente): abilità, Spell, Hidden e Terreni fanno parte del gioco standard e agiscono nei momenti descritti in [effetti](../effetti/effetti.md).

## Vocabolario

| Termine | Significato |
|---|---|
| **Sfidante** (`CHALLENGER`) | chi ha lanciato la sfida |
| **Avversario** (`OPPONENT`) | chi l'ha accettata |
| **Round** | un singolo scambio di colpi simultaneo tra le due creature in campo |
| **Scontro** | la serie di round tra le **stesse** due creature, finché almeno una muore |
| **Partita** | tutti gli scontri, fino alla fine |

## Valori (rule set)

Tutto ciò che **non** è una costante del regolamento si modifica dal **backoffice**, nel database dell'API, e arriva all'engine in ogni richiesta
(blocco `rules`, vedi [api-engine](../api-engine/api-engine.md)). L'engine non ha valori predefiniti.

| Parametro | Dove sta | Valore del rule set `1.0` | Stato |
|---|---|---|---|
| Punti vita iniziali | `rule_sets.starting_life_points` | 30 | **[DA CONFERMARE] D-01** |
| Limite di round totali | `rule_sets.max_rounds` | 100 | **[DA CONFERMARE] D-02** dopo le simulazioni |
| Bonus e malus di fazione | tabella `faction_bonus` | +2 / −1 ATK | vedi [fazioni](../fazioni/fazioni.md) |

```java
/** Regole di una partita, costruite dal blocco {@code rules} della richiesta. Nessun valore predefinito. */
public record RuleConfig(String rulesVersion, int startingLifePoints, int maxRounds) {

	public RuleConfig {
		if (startingLifePoints < 1 || maxRounds < 1) {
			throw new InvalidSimulationInputException("Regole non valide");
		}
	}
}
```

Restano **nel codice** solo le regole che non sono numeri da bilanciare: l'ordine dei passi di un round, il danno al giocatore
come DEF negativa, l'attacco minimo 0, l'ordine delle condizioni di fine partita. Nei test si usa `TestRules.standard()` (30, 100).

## Svolgimento

### 1. Preparazione

1. Ogni giocatore riceve i punti vita iniziali.
2. Ogni mazzo viene **mescolato lato server** con il seed (vedi [mescolamento](../mescolamento/mescolamento.md)).
3. Ogni giocatore pesca finché non ha una creatura in campo.

### 2. Inizio di uno scontro

Quando una nuova coppia di creature si affronta, si applica il **bonus DEF di fazione** (se previsto) alla creatura che lo riceve. Vedi [fazioni](../fazioni/fazioni.md).

### 3. Un round

Le due creature si colpiscono **contemporaneamente**.

```
attaccoA = ATK di A + bonus ATK di fazione (A contro B)
attaccoB = ATK di B + bonus ATK di fazione (B contro A)

DEF di A = DEF di A − attaccoB
DEF di B = DEF di B − attaccoA
```

- Il danno riduce la **DEF in modo permanente**: non si rigenera.
- L'**ATK non cambia** per i danni.
- Una creatura **muore** quando la sua DEF è **≤ 0**.
- Il bonus di fazione può essere negativo (−1, malus) e, per le carte con due fazioni, è la somma di tutte le coppie: vedi [fazioni](../fazioni/fazioni.md#come-si-applica). L'attacco effettivo non scende mai sotto 0.

### 4. Esito del round

| Situazione | Cosa succede |
|---|---|
| Nessuna muore | nuovo round tra le stesse creature |
| Muoiono entrambe | **nessun danno** ai giocatori; entrambi pescano |
| Muore solo una | il proprietario della creatura morta subisce danno; poi pesca |

### 5. Danno al giocatore

**Il danno al giocatore è solo la parte negativa della DEF della creatura sconfitta**, cioè il danno "in eccesso".

```
danno al giocatore = −(DEF finale della creatura sconfitta)      (0 se la DEF è esattamente 0)
```

```java
int playerDamage = Math.max(0, -defeated.defense());
```

Esempio: la DEF della sconfitta passa da 5 a −3 → il giocatore perde **3** punti vita.

> Il brief dice "differenza tra il proprio attacco e la difesa residua della creatura sconfitta": con la difesa residua *prima* del colpo è lo stesso numero (8 − 5 = 3).
> La bozza del regolamento per i giocatori propone invece 8 − (−3) = 11: va allineata, vedi **D-03** in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md#regole-base).

La regola vale qualunque sia la causa della morte (attacco, effetti di fine round, effetti alla morte): conta la DEF finale.

### 6. Pescare

- Quando una creatura muore, il suo proprietario pesca la carta successiva del mazzo.
- La creatura sopravvissuta **resta in campo** con la DEF che le è rimasta.
- Se si pesca una Spell, una Hidden o un Terreno, la carta viene risolta (vedi [carte](../carte/carte.md)) e si continua a pescare finché esce una creatura.

### Esempio completo (senza bonus di fazione)

```
A: Creatura ATK 8, DEF 6        B: Creatura ATK 4, DEF 5

Round 1:  A colpisce B per 8 → DEF B = 5 − 8 = −3  → B muore
          B colpisce A per 4 → DEF A = 6 − 4 =  2  → A vive
Esito:    muore solo B. Il proprietario di B perde 3 punti vita (la DEF negativa).
          Il proprietario di B pesca. A resta in campo con DEF 2.
```

## Fine della partita

Si controlla **dopo ogni round**, in quest'ordine:

| # | Condizione | Risultato | `endReason` |
|---|---|---|---|
| 1 | Entrambi i giocatori a ≤ 0 punti vita | Pareggio | `BOTH_LIFE_POINTS_ZERO` |
| 2 | Un giocatore a ≤ 0 punti vita | Perde lui | `LIFE_POINTS_ZERO` |
| 3 | Entrambi senza creature da schierare | Vince chi ha più punti vita; se uguali pareggio | `BOTH_DECKS_EMPTY` |
| 4 | Un giocatore deve pescare ma non ha più creature, l'altro sì | Perde chi non ha creature | `NO_CREATURES_LEFT` |
| 5 | Raggiunto il limite di round | Vince chi ha più punti vita; se uguali pareggio | `ROUND_LIMIT` |

Il risultato è `CHALLENGER_WIN`, `OPPONENT_WIN` o `DRAW` (gli stessi valori dell'enum `BattleResult` del database).

Le condizioni 3, 4 e 5 sono proposte del team **[DA CONFERMARE]**; servono perché il brief chiede una conclusione anche con mazzi esauriti o scontri infiniti (es. due creature con ATK 0).

## Casi limite da testare

- Due creature con ATK 0 → la partita termina per `ROUND_LIMIT`, non va in loop.
- Creatura che muore con DEF esattamente 0 → nessun danno al giocatore.
- Danno al giocatore che porta i punti vita sotto zero → la partita finisce subito.
- Morte simultanea all'ultimo round utile.
- Mazzo che finisce mentre l'avversario ha ancora una creatura in campo.

Quasi tutti questi casi hanno una richiesta pronta, con il calcolo passo per passo, in [`examples/`](../../examples/README.md) (01–07 e 10).
