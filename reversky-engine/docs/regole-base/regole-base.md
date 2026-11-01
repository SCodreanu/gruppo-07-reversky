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

## Valori (`RuleConfig`)

| Parametro | Valore | Stato |
|---|---|---|
| Punti vita iniziali | 30 | **[DA CONFERMARE]** |
| Limite di round totali | 100 | **[DA CONFERMARE]** dopo le simulazioni |

I bonus di fazione **non** sono qui: arrivano nella richiesta, presi dalla tabella `faction_bonus` dell'API (vedi [fazioni](../fazioni/fazioni.md)).

```java
public record RuleConfig(String rulesVersion, int startingLifePoints, int maxRounds) {

	public static RuleConfig defaults() {
		return new RuleConfig("1.0", 30, 100);
	}
}
```

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
