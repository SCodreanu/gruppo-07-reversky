# Demo per il SAL del 23 novembre

Scaletta per mostrare al SAL la prima sfida simulata dal motore (`ENG-18`): una partita via REST con risultato riproducibile e la lettura del log round per round.
Durata: circa 10 minuti. Le richieste sono gli esempi di [`examples/`](../../examples/README.md), con il risultato calcolato a mano.

## Prima del SAL

| # | Cosa | Risultato atteso |
|---|---|---|
| 1 | `gradlew.bat build` dalla cartella `reversky-engine` | `BUILD SUCCESSFUL`: tutti i test verdi, compresi gli esempi e il test di contratto con l'API (`ENG-24`) |
| 2 | `scripts\start-engine.bat` (oppure il container usa e getta della [guida](../guida-sviluppo/guida-sviluppo.md#2-avviare-lengine)) | `Started ReverskyEngineApplication` sulla porta 8081 |
| 3 | apri `http://localhost:8081/swagger-ui.html` | Swagger con la sola sezione `Simulations`: l'engine espone solo `POST /api/v1/simulations` (`ENG-43`) |
| 4 | prova una volta gli esempi `full` e `special` con *Try it out* | `200`, risultati come nella tabella qui sotto |

## Scaletta

1. **Cos'è il motore (1 minuto).** Un componente Java isolato: riceve mazzi, regole, fazioni, bonus e seed, e restituisce la partita round per round. Non ha database; il giocatore non prende decisioni.
2. **Partita completa: esempio 08 (3 minuti).** In Swagger, `POST /api/v1/simulations`, esempio `full` (10 carte per parte, seed 42), *Execute*. La risposta è sotto il riquadro *Curl*.
   - Risultato: `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 14 round, PV 30–27.
   - **Riproducibilità:** si esegue di nuovo, stesso risultato e stesso log. Cambiando `seed` in 43 cambia la partita (PV 27–23).
3. **Leggere il log (3 minuti).** Ogni evento ha `sequence`, `round`, `type` e un `message` in inglese per i giocatori:
   - round 0: `MATCH_STARTED`, le pescate e le creature che entrano (`CREATURE_ENTERED`);
   - ogni round: `ROUND_STARTED`, i due `ATTACK_COMPUTED` (ATK base, bonus di fazione, modificatori), i due `DAMAGE_DEALT` con la DEF che resta, poi le morti (`CREATURE_DIED`) e il danno al giocatore (`PLAYER_DAMAGED`, la DEF negativa della creatura sconfitta);
   - alla fine `MATCH_ENDED` con l'esito e il motivo.
4. **Carte speciali: esempio 13 (3 minuti).** Esempio `special`. Risultato `CHALLENGER_WIN`, 3 round, PV 30–27. Nel log si vedono, in ordine:
   - `SPELL_RESOLVED` di Veil of the Anchored senza bersaglio e, all'inizio dello scontro, `SCP-131: +5 DEF from Veil of the Anchored`;
   - `TERRAIN_PLACED` di Watching Rift e il suo bonus alle creature Archive;
   - `ITEM_PLACED` del Badge di Uscita Negata e, al round 1, `ITEM_CONSUMED`: Faceling resta con 1 DEF invece di morire;
   - `HIDDEN_SET` senza nome (la carta è coperta) e, nel secondo scontro, `Echo Strike triggers`;
   - `SPELL_RESOLVED` di Skin Fracture, che colpisce subito Archive Puppet.
   I passaggi del calcolo sono in [`examples/README.md`](../../examples/README.md#come-sono-stati-calcolati).
5. **Errori (opzionale, 1 minuto).** Una richiesta con un `effectCode` scritto male (es. `VEIL_OF_ANCHORD` su una Spell) dà `400` con `code` `UNKNOWN_EFFECT`.

## Riferimenti rapidi

| Esempio | In Swagger | Risultato atteso |
|---|---|---|
| `08-partita-completa-seed-42` | `full` | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 14 round, PV 30–27 |
| `13-carte-speciali` | `special` | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 3 round, PV 30–27 |

Con curl, al posto di Swagger (cmd, dalla cartella `reversky-engine`):

```bat
curl -s -X POST http://localhost:8081/api/v1/simulations -H "Content-Type: application/json" --data-binary @examples\13-carte-speciali.json
```

Tutti i 92 effetti del catalogo sono registrati (fino a `ENG-40`, con i numeri di `ENG-42`): l'esempio 09 (Hidebehind) mostra una prevenzione nel log. Gli esempi 08 e 13 usano creature senza effetto.

La parte della demo con l'API (una sfida tra `alice` e `bob` dei dati di prova, accettata e simulata dall'engine vero, con il retry a engine spento) si aggiunge qui dopo la prova completa di `API-54`, che è il primo passo dopo questi documenti (vedi la [roadmap dell'API](../../../reversky-api/docs/roadmap/roadmap.md)).
