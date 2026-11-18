# Log della partita

## A cosa serve

Il brief chiede che il risultato della partita sia disponibile **turno per turno** a entrambi i giocatori. L'engine produce una lista ordinata di **eventi**: il frontend la usa per mostrare (o animare) la partita, l'API la salva nello storico.

## Formato di un evento

```java
public record MatchEvent(
		int sequence,           // 1, 2, 3… ordine assoluto
		int round,              // round in cui avviene (0 = preparazione)
		EventType type,
		Side side,              // CHALLENGER, OPPONENT o null se riguarda entrambi
		Map<String, Object> data,
		String message) {       // frase leggibile in inglese (la leggono i giocatori)
}
```

In JSON:

```json
{
  "sequence": 42,
  "round": 7,
  "type": "DAMAGE_DEALT",
  "side": "CHALLENGER",
  "data": { "source": "Hounds", "target": "Kappa", "amount": 11, "targetDefense": -2 },
  "message": "Hounds deals 11 damage to Kappa"
}
```

- `data` contiene i valori **strutturati** (per animazioni e grafica).
- `message` è pronto da mostrare in una lista testuale.

## Tipi di evento

| `type` | Quando | Chiavi di `data` | Esempio di `message` |
|---|---|---|---|
| `MATCH_STARTED` | inizio | `startingLifePoints` | `Match started — 30 life points each` |
| `CARD_DRAWN` | un giocatore pesca una Spell, una Hidden, un Terreno o un Oggetto (per le creature basta `CREATURE_ENTERED`, D-07) | `card`, `cardType` | `Challenger draws Veil of the Anchored (SPELL)` |
| `CREATURE_ENTERED` | una creatura entra in campo | `card`, `attack`, `defense` | `Challenger plays Crawlers (ATK 4, DEF 5)` |
| `SPELL_RESOLVED` | una Spell viene risolta e scartata | `card`, `effect`, `target` (nullo se si applica alla prossima creatura) | `Veil of the Anchored resolves on Kappa` |
| `BURN_REVEALED` | una Spell `BURN` rivela la prossima carta del mazzo (D-13) | `card`, `revealed` (nullo a mazzo vuoto), `bonusApplied` | `Call of the Rift reveals Kappa` |
| `HIDDEN_SET` | una Hidden viene messa coperta | nessuna: la carta resta nascosta | `Challenger sets a Hidden card` |
| `HIDDEN_TRIGGERED` | una Hidden si attiva | `card`, `effect` | `Last Sanctuary triggers` |
| `TERRAIN_PLACED` | un Terreno entra in gioco (e sostituisce quello già in gioco, D-19) | `card`, `replaced` (nullo se non c'era un Terreno) | `Echo Rift enters play, replacing Watching Rift` |
| `TERRAIN_REMOVED` | un effetto toglie il Terreno in gioco (The Null Witness) o lo inghiotte (The Hollow Wyrm, anche appena pescato) | `card`, `cause` | `Echo Rift is removed by The Null Witness` |
| `DECK_REORDERED` | un effetto sposta una carta nel mazzo di un giocatore (La Regina dei Sentieri Perduti) | `source`, `card`, `position` (da 1) | `La Regina dei Sentieri Perduti moves Kappa to position 3` |
| `ITEM_PLACED` | un Oggetto entra in gioco | `card` | `Challenger places Campanello del Torii` |
| `ITEM_CONSUMED` | un Oggetto applica il suo effetto e si consuma (D-47) | `card`, `effect`, `creature` | `Campanello del Torii is used on Kappa` |
| `ROUND_STARTED` | inizio di un round | `round` | `Round 1` |
| `ATTACK_COMPUTED` | calcolo dell'attacco | `attacker`, `baseAttack`, `factionBonus`, `modifiers`, `total` | `Crawlers attacks with 6 (4 base, +2 faction)` |
| `DAMAGE_DEALT` | una creatura colpisce l'altra | `source`, `target`, `amount`, `targetDefense` | `Crawlers deals 6 damage to Kappa (DEF 1)` |
| `EFFECT_TRIGGERED` | un'abilità agisce; anche il bonus DEF di fazione a inizio scontro, se maggiore di 0 | `card`, `description` | `Kappa: +3 ATK in the first round` |
| `CREATURE_DIED` | una creatura muore | `card` | `Crawlers is destroyed` |
| `PLAYER_DAMAGED` | un giocatore perde punti vita | `amount`, `lifePoints` | `Challenger loses 3 life points (27 left)` |
| `MATCH_ENDED` | fine | `result`, `endReason` | `Challenger wins (no creatures left)` |

I valori di `data` non hanno testo: le carte sono indicate per nome, gli enum con il loro nome (`"SPELL"`, `"CHALLENGER_WIN"`).
Nei messaggi le parti a 0 di `ATTACK_COMPUTED` non compaiono, e i numeri negativi usano il segno meno tipografico (−).

## Esempio di sequenza

```
#1  round 0  MATCH_STARTED     Match started — 30 life points each
#2  round 0  CREATURE_ENTERED  Challenger plays Crawlers (ATK 4, DEF 5)
#3  round 0  CREATURE_ENTERED  Opponent plays Kappa (ATK 6, DEF 7)
#4  round 0  EFFECT_TRIGGERED  Kappa: +3 ATK in the first round
#5  round 1  ROUND_STARTED     Round 1
#6  round 1  ATTACK_COMPUTED   Crawlers attacks with 6 (4 base, +2 faction)
#7  round 1  ATTACK_COMPUTED   Kappa attacks with 8 (6 base, −1 faction, +3 modifiers)
#8  round 1  DAMAGE_DEALT      Crawlers deals 6 damage to Kappa (DEF 1)
#9  round 1  DAMAGE_DEALT      Kappa deals 8 damage to Crawlers (DEF −3)
#10 round 1  CREATURE_DIED     Crawlers is destroyed
#11 round 1  PLAYER_DAMAGED    Challenger loses 3 life points (27 left)
...
```

## Regole

1. **Ogni cambiamento di stato** visibile deve avere un evento: se qualcosa cambia ATK, DEF o PV e non c'è un evento, è un bug.
2. Gli eventi si aggiungono **solo** tramite `MatchLog` (metodi come `log.damage(...)`, `log.effect(...)`), mai costruendo `MatchEvent` a mano in giro per il codice.
3. I messaggi sono in **inglese** (li leggono i giocatori nel portale e nel Verbale), brevi, al presente. I `data` restano strutturati e senza testo.
4. L'ordine degli eventi è quello reale di risoluzione: per i danni simultanei prima lo sfidante, poi l'avversario.

## Dimensione

Una partita può arrivare a qualche migliaio di eventi (limite di round × eventi per round). Va bene per una colonna JSON nel database dell'API; se diventasse troppo grande si può ridurre il dettaglio di `ATTACK_COMPUTED`.
