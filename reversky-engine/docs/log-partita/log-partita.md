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

| `type` | Quando | `data` principale |
|---|---|---|
| `MATCH_STARTED` | inizio | punti vita iniziali |
| `CARD_DRAWN` | un giocatore pesca una Spell, una Hidden, un Terreno o un Oggetto (per le creature basta `CREATURE_ENTERED`, D-07) | carta |
| `CREATURE_ENTERED` | una creatura entra in campo | carta, ATK, DEF |
| `SPELL_RESOLVED` | una Spell viene risolta e scartata | carta, effetto, bersaglio |
| `BURN_REVEALED` | una Spell `BURN` rivela la prossima carta del mazzo (D-13) | Spell, carta rivelata, bonus applicato (nessuno se non è una creatura) |
| `HIDDEN_SET` | una Hidden viene messa coperta | — (la carta resta nascosta nel messaggio) |
| `HIDDEN_TRIGGERED` | una Hidden si attiva | carta, effetto |
| `TERRAIN_PLACED` | un Terreno entra in gioco (e sostituisce quello già in gioco, D-19) | carta, Terreno sostituito se c'era |
| `TERRAIN_REMOVED` | un effetto toglie il Terreno in gioco (The Null Witness) o lo inghiotte (The Hollow Wyrm, anche appena pescato) | Terreno, causa (carta che lo ha tolto) |
| `DECK_REORDERED` | un effetto sposta una carta nel mazzo di un giocatore (La Regina dei Sentieri Perduti) | carta che sposta, carta spostata, nuova posizione |
| `ITEM_PLACED` | un Oggetto entra in gioco | carta |
| `ITEM_CONSUMED` | un Oggetto applica il suo effetto e si consuma (D-47) | carta, effetto, creatura |
| `ROUND_STARTED` | inizio di un round | numero round |
| `ATTACK_COMPUTED` | calcolo dell'attacco | ATK base, bonus ATK di fazione, modificatori, totale |
| `DAMAGE_DEALT` | una creatura colpisce l'altra | sorgente, bersaglio, danni, DEF rimasta |
| `EFFECT_TRIGGERED` | un'abilità agisce | creatura, descrizione |
| `CREATURE_DIED` | una creatura muore | carta |
| `PLAYER_DAMAGED` | un giocatore perde punti vita | danni, punti vita rimasti |
| `MATCH_ENDED` | fine | vincitore, `endReason` |

## Esempio di sequenza

```
#1  round 0  MATCH_STARTED     Match started — 30 life points each
#2  round 0  CREATURE_ENTERED  Challenger plays Crawlers (ATK 4, DEF 5)
#3  round 0  CREATURE_ENTERED  Opponent plays Kappa (ATK 6, DEF 7)
#4  round 0  EFFECT_TRIGGERED  Kappa: +2 ATK in the first round
#5  round 1  ROUND_STARTED     Round 1
#6  round 1  ATTACK_COMPUTED   Crawlers attacks with 6 (4 base, +2 faction)
#7  round 1  ATTACK_COMPUTED   Kappa attacks with 7 (6 base, −1 faction, +2 effect)
#8  round 1  DAMAGE_DEALT      Crawlers deals 6 damage to Kappa (DEF 1)
#9  round 1  DAMAGE_DEALT      Kappa deals 7 damage to Crawlers (DEF −2)
#10 round 1  CREATURE_DIED     Crawlers is destroyed
#11 round 1  PLAYER_DAMAGED    Challenger loses 2 life points (28 left)
...
```

## Regole

1. **Ogni cambiamento di stato** visibile deve avere un evento: se qualcosa cambia ATK, DEF o PV e non c'è un evento, è un bug.
2. Gli eventi si aggiungono **solo** tramite `MatchLog` (metodi come `log.damage(...)`, `log.effect(...)`), mai costruendo `MatchEvent` a mano in giro per il codice.
3. I messaggi sono in **inglese** (li leggono i giocatori nel portale e nel Verbale), brevi, al presente. I `data` restano strutturati e senza testo.
4. L'ordine degli eventi è quello reale di risoluzione: per i danni simultanei prima lo sfidante, poi l'avversario.

## Dimensione

Una partita può arrivare a qualche migliaio di eventi (limite di round × eventi per round). Va bene per una colonna JSON nel database dell'API; se diventasse troppo grande si può ridurre il dettaglio di `ATTACK_COMPUTED`.
