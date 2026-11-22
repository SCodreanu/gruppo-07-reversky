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
| Punti vita iniziali | `rule_sets.starting_life_points` | 30 | deciso (D-01, 22 novembre); si cambia dal backoffice |
| Limite di round totali | `rule_sets.max_rounds` | 75 | deciso (D-02, 9 novembre); si cambia dal backoffice |
| Bonus e malus di fazione | tabella `faction_bonuses` | +2 / −1 ATK | vedi [fazioni](../fazioni/fazioni.md) |

```java
/** Regole di una partita, costruite dal blocco {@code rules} della richiesta. Nessun valore predefinito. */
public record RuleConfig(String rulesVersion, int startingLifePoints, int maxRounds) {

	public RuleConfig {
		if (startingLifePoints < 1 || maxRounds < 1) {
			// Stesso codice della Bean Validation nello strato web (docs/api-engine, controllo 1)
			throw new InvalidSimulationInputException(ErrorCode.VALIDATION_FAILED, "Regole non valide: ...");
		}
	}
}
```

Restano **nel codice** solo le regole che non sono numeri da bilanciare: l'ordine dei passi di un round, il danno al giocatore
come DEF negativa, l'attacco minimo 0, l'ordine delle condizioni di fine partita. Nei test si usa `TestRules.standard()` (30, 75).

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
- L'ATK di una creatura può scendere sotto 0 per i malus degli effetti (es. Smiler); il minimo 0 vale per l'attacco effettivo, cioè dopo aver sommato il bonus di fazione: ATK 2 con un malus di −6 e +2 di fazione attacca con 0 (−4 + 2 = −2), non con 2 (`ENG-29`).

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
> La bozza del regolamento per i giocatori proponeva invece 8 − (−3) = 11: con **D-03** (decisa il 9 novembre) vale 3 e la bozza va allineata. Vedi [decisioni aperte](../decisioni-aperte/decisioni-aperte.md#regole-base).

La regola vale qualunque sia la causa della morte (attacco, effetti di fine round, effetti alla morte): conta la DEF finale.

### 6. Pescare

- Quando una creatura muore, il suo proprietario pesca la carta successiva del mazzo.
- La creatura sopravvissuta **resta in campo** con la DEF che le è rimasta.
- Se si pesca una Spell, una Hidden, un Terreno o un Oggetto, la carta viene risolta (vedi [carte](../carte/carte.md)) e si continua a pescare finché esce una creatura.
- Prima pesca lo sfidante, poi l'avversario. Una Spell `DAMAGE` può uccidere la creatura avversaria in campo, anche quella appena pescata: le morti si controllano subito (D-08) e chi ha il campo vuoto pesca di nuovo prima del round.
- Se nel mazzo restano solo carte non creatura, si risolvono tutte in ordine e poi si controlla la fine della partita (condizione 3 o 4, D-06).

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

Quando si controlla (deciso il 22 novembre, `ENG-29`):

- Dopo un round chi ha il campo vuoto pesca (prima lo sfidante), poi si controllano tutte le condizioni. Le condizioni 3 e 4 hanno bisogno della pescata, perché le carte non creatura rimaste si risolvono prima (D-06).
- Le condizioni 1 e 2 (punti vita) si controllano anche **prima di ogni pescata**: chi è a 0 punti vita o meno ha già perso e non pesca più. Così una sua Spell `DAMAGE` non può più portare a 0 anche l'altro giocatore e cambiare una vittoria in pareggio. Prima di `ENG-29` succedeva (bug corretto).
- Il danno al giocatore viene solo da una morte singola, quindi con questo controllo la condizione 1 non si raggiunge più in una partita; resta nel codice per sicurezza.
- Il limite di round (condizione 5) si controlla dopo le pescate, come le altre: se nell'ultimo round muore una creatura, il suo proprietario pesca ancora e una Spell pescata può cambiare i punti vita che decidono `ROUND_LIMIT`.

Le condizioni 3, 4 e 5 erano proposte del team, approvate dai DSG il 9 novembre (D-05); servono perché il brief chiede una conclusione anche con mazzi esauriti o scontri infiniti (es. due creature con ATK 0).

## Casi limite da testare

- Due creature con ATK 0 → la partita termina per `ROUND_LIMIT`, non va in loop.
- Creatura che muore con DEF esattamente 0 → nessun danno al giocatore.
- Danno al giocatore che porta i punti vita sotto zero → la partita finisce subito, prima che il perdente peschi.
- Morte simultanea all'ultimo round utile → entrambi pescano, poi `ROUND_LIMIT`.
- Mazzo che finisce mentre l'avversario ha ancora una creatura in campo.
- ATK negativo per un malus, con un bonus di fazione positivo → l'attacco effettivo è 0.

Quasi tutti questi casi hanno una richiesta pronta, con il calcolo passo per passo, in [`examples/`](../../examples/README.md) (01–07 e 10); i test di `ENG-29` sono in [test](../test/test.md#casi-limite-eng-29).
