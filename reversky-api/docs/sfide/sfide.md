# Sfide e storico

## Cosa fa

Ci sono due modalità di sfida (`battles.mode`):

| Modalità | Come si trova l'avversario | Rating |
|---|---|---|
| `FRIENDLY` (amichevole) | l'utente A (sfidante) invita un **amico** B scegliendo uno dei propri mazzi; B accetta scegliendo a sua volta un mazzo | non cambia |
| `RANKED` (classificata) | l'utente entra in coda con un mazzo e il **matchmaking** gli assegna un avversario con rating simile | Elo aggiornato |

In entrambi i casi la partita viene **simulata subito dall'engine** e il risultato (round per round) è visibile ai due giocatori. Ogni partita finisce nello **storico**.

L'API **non calcola** la partita: prepara i dati, chiama `reversky-engine`, salva il risultato.

Tabelle: `battle_invites`, `battles`, `matchmaking_queue`, `player_ratings`.

## Regolamento

Esiste **un solo regolamento**: creature con abilità, Spell, Hidden, Terreni e Oggetti fanno tutti parte del gioco standard. Non ci sono varianti di regole da scegliere (decisione concordata con il cliente): `FRIENDLY` e `RANKED` cambiano solo come si trova l'avversario e se il rating si aggiorna, non le regole della partita.

## Amichevole: invito — `battle_invites`

| Stato | Significato |
|---|---|
| `PENDING` | in attesa della risposta dell'avversario |
| `ACCEPTED` | accettato: esiste la partita in `battles` |

```
PENDING ──accept (B)──▶ ACCEPTED ──▶ battles (1 sola partita per invito)
   ├──reject (B)──────▶ riga eliminata
   └──ritiro (A)──────▶ riga eliminata
```

Gli inviti **non scadono**: restano `PENDING` finché l'avversario non risponde o lo sfidante non li ritira. Le amichevoli non danno crediti né rating, quindi un invito vecchio non ha conseguenze.

### Mazzo dello sfidante

Finché l'invito è `PENDING`, il mazzo dello sfidante non si può eliminare (`409`, vedi [mazzi](../mazzi/mazzi.md)): per eliminarlo bisogna prima ritirare l'invito. Si può invece modificare, tanto viene validato di nuovo all'accettazione. Quando l'invito è stato accettato, eliminare il mazzo mette `challenger_deck_id` a `NULL`: la partita conserva comunque la propria fotografia del mazzo.

### Creazione

`POST /api/v1/battle-invites`

```json
{ "opponentId": 12, "deckId": 3 }
```

Controlli:
- niente sfide a se stessi (`chk_battle_invites_no_self_challenge`);
- il mazzo è mio ed è **valido**;
- l'avversario esiste ed è attivo (`is_enabled`);
- l'avversario è un **amico** (`friendships` con stato `ACCEPTED`, in una delle due direzioni), altrimenti `403`;
- non ho già un invito `PENDING` verso quell'avversario (`uq_battle_invites_pending_pair`), altrimenti `409`. Anche l'avversario può averne uno aperto verso di me: sono due partite diverse.

## Amichevole: accettazione e simulazione

`POST /api/v1/battle-invites/{id}/accept` con `{ "deckId": 8 }`

Il flusso è diviso in **tre passi**, per non tenere aperta una transazione mentre si aspetta l'engine:

```
1) Transazione 1 — preparazione
   - blocca l'invito, controlla che sia PENDING e che io sia l'avversario
   - controlla che i due giocatori siano ancora amici
   - valida di nuovo ENTRAMBI i mazzi (le collezioni possono essere cambiate)
   - genera il seed con SecureRandom
   - legge il rule set attivo e i bonus di fazione
   - crea la riga in battles (mode = FRIENDLY, invite_id = invito):
       challenger_deck / opponent_deck = "fotografia" dei mazzi (jsonb)
       battle_stats = { seed, rules, factionBonuses }
       rounds_log   = []
       result       = NULL           ← simulazione non ancora completata
   - invito → ACCEPTED

2) Chiamata HTTP all'engine (fuori transazione)
   EngineClient.simulate(request)

3) Transazione 2 — salvataggio
   - blocca la riga di battles; se result è già valorizzato si ferma (vedi sotto)
   - result, battle_stats completo, rounds_log
   - nessun credito: le amichevoli non danno ricompense (vedi crediti)
```

Se l'engine non risponde o dà errore, la partita resta con `result = NULL`. `POST /api/v1/battles/{id}/retry` rilancia la simulazione **con lo stesso seed, la stessa fotografia dei mazzi e le stesse regole** (copiate in `battle_stats` alla preparazione, anche se nel frattempo il backoffice le ha cambiate), quindi il risultato non può essere "rigiocato" per ottenerne uno migliore.

Due simulazioni della stessa partita possono sovrapporsi (due retry insieme, o un retry mentre la prima chiamata è ancora in corso). Per questo la transazione 2 blocca la riga di `battles` e, se `result` è già valorizzato, si ferma senza scrivere niente: il risultato sarebbe identico, e così Elo e crediti non si assegnano due volte.

### Perché la fotografia dei mazzi

Le statistiche delle carte possono cambiare dal backoffice (bilanciamento). Una partita giocata deve restare identica nello storico, quindi `challenger_deck` e `opponent_deck` salvano le carte **con i valori del momento**.

```json
[
  { "cardId": 4, "name": "Crawlers", "type": "CREATURE", "rarity": "Common",
    "factionId": 1, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null, "quantity": 3 }
]
```

### Perché il seed lo genera l'API

- Il mescolamento deve avvenire lato server e nessun giocatore deve poterlo influenzare (requisito del brief).
- Salvando il seed, la stessa partita si può rigiocare identica per verificare un bug.

### Contenuto di `battle_stats`

```json
{
  "seed": 8812734650912,
  "rules": { "rulesVersion": "1.0", "startingLifePoints": 30, "maxRounds": 75 },
  "factionBonuses": [ { "originFactionId": 2, "targetFactionId": 3, "atkBonus": 2, "defBonus": 0 } ],
  "rulesVersion": "1.0",
  "endReason": "LIFE_POINTS_ZERO",
  "totalRounds": 37,
  "challenger": { "finalLifePoints": 9, "creaturesDefeated": 14 },
  "opponent":   { "finalLifePoints": 0, "creaturesDefeated": 11 }
}
```

`rules` e `factionBonuses` sono scritti alla preparazione (transazione 1) e servono al retry; il resto arriva dalla risposta dell'engine.

`rounds_log` contiene la lista di eventi restituita dall'engine.

## Classificata: matchmaking — `matchmaking_queue`

### Ingresso in coda

`POST /api/v1/matchmaking/queue`

```json
{ "deckId": 3 }
```

Controlli:
- il mazzo è mio ed è **valido**;
- non sono già in coda (chiave primaria `user_id`, altrimenti `409`);
- se non ho ancora una riga in `player_ratings` viene creata (rating 1000).

Il ticket salva il mazzo scelto e il rating attuale. Si esce dalla coda con `DELETE /api/v1/matchmaking/queue`. Se il mazzo in coda viene modificato o eliminato, il ticket viene rimosso.

Il client controlla lo stato con `GET /api/v1/matchmaking/queue` ogni pochi secondi: `200` con `{ "status": "WAITING", "queuedAt": "…", "window": 250 }` finché è in coda, `404` quando il ticket non esiste più. A quel punto legge l'ultima partita con `GET /api/v1/battles?mode=RANKED&size=1`.

Il ticket sparisce anche senza abbinamento (mazzo non più valido, account disattivato). Perciò la partita è quella appena abbinata solo se il suo `createdAt` è successivo al `queuedAt` visto dal client; altrimenti il client avvisa l'utente che è uscito dalla coda.

### Abbinamento

Un job `@Scheduled` prova ad abbinare i ticket ogni 2 secondi; lo stesso tentativo parte anche subito dopo ogni ingresso in coda.

La finestra di ricerca dipende da quanto aspetta il ticket più vecchio:

```
finestra = min(100 + 50 × (secondi di attesa / 10, per difetto), 400)
```

Esempio: dopo 35 secondi di attesa si cercano avversari entro ±250 punti.

### Anti-boosting

Due amici potrebbero entrare in coda nello stesso momento e farsi abbinare di continuo, passandosi punti Elo. Per evitarlo il matchmaking **salta un avversario già affrontato in una classificata negli ultimi 10 minuti**: se è l'unico disponibile, si continua ad aspettare.

```sql
AND NOT EXISTS (
    SELECT 1 FROM battles b
    WHERE b.mode = 'RANKED'
      AND b.created_at > now() - INTERVAL '10 minutes'
      AND ((b.challenger_id = :me AND b.opponent_id = q.user_id)
        OR (b.challenger_id = q.user_id AND b.opponent_id = :me))
)
```

Il flusso usa gli stessi tre passi delle amichevoli:

```
1) Transazione 1 — abbinamento e preparazione
   - prende il ticket più vecchio                       (FOR UPDATE SKIP LOCKED)
   - cerca l'avversario entro la finestra: rating più vicino, poi il più vecchio,
     escludendo chi ho affrontato in una RANKED negli ultimi 10 minuti
                                                        (FOR UPDATE SKIP LOCKED)
   - valida di nuovo ENTRAMBI i mazzi (le carte possono essere state scambiate)
       se un mazzo non è più valido: elimina solo quel ticket e si ferma
   - elimina i due ticket
   - genera il seed con SecureRandom
   - legge il rule set attivo e i bonus di fazione
   - crea la riga in battles:
       mode = RANKED, invite_id = NULL
       challenger = chi era in coda da più tempo
       fotografie dei mazzi, battle_stats = { seed, rules, factionBonuses }, rounds_log = [], result = NULL

2) Chiamata HTTP all'engine (fuori transazione)

3) Transazione 2 — salvataggio
   - blocca la riga di battles; se result è già valorizzato si ferma
   - result, battle_stats completo, rounds_log
   - blocca le due righe di player_ratings (user_id minore per primo, evita deadlock)
   - calcola l'Elo, aggiorna rating, peak_rating e contatori
   - scrive challenger_rating_before / challenger_rating_delta
     e opponent_rating_before / opponent_rating_delta
   - crediti di vittoria/pareggio, dal rule set della partita (vedi crediti);
     con un pareggio blocca le due righe di inventories, user_id minore per primo
```

Con `SKIP LOCKED` due esecuzioni del job in parallelo non possono abbinare lo stesso utente: un ticket già bloccato da un'altra transazione viene saltato, non atteso.

Se l'engine fallisce si usa lo stesso `retry` delle amichevoli. L'Elo si applica solo nella transazione 2, insieme a `result`, e il retry è possibile solo finché `result` è nullo: ogni partita aggiorna il rating **una sola volta**.

### Elo — `player_ratings`

| Valore | Regola |
|---|---|
| Rating iniziale | 1000 |
| Risultato atteso | `E = 1 / (1 + 10^((R_avversario − R) / 400))` |
| Punteggio | vittoria 1, pareggio 0,5, sconfitta 0 |
| Variazione | `round(K × (punteggio − E))` |
| K | 40 nelle prime 10 partite classificate, poi 20 |
| Minimo | il rating non scende sotto 0 |

Tutti i pareggi previsti dal regolamento (limite di round, entrambi a zero punti vita) valgono 0,5. `games_played` è quello **prima** della partita; se i due giocatori hanno K diversi, le due variazioni non sono uguali e opposte.

```java
/** Variazione Elo di un giocatore dopo una partita classificata. */
public static int eloDelta(int rating, int opponentRating, double score, int gamesPlayed) {
	double expected = 1.0 / (1.0 + Math.pow(10, (opponentRating - rating) / 400.0));
	int k = gamesPlayed < 10 ? 40 : 20;
	int delta = (int) Math.round(k * (score - expected));
	return Math.max(delta, -rating); // il rating non scende sotto 0
}
```

Il sistema è **approvato dal cliente**: si affrontano giocatori con Elo simile, e chi è favorito guadagna meno se vince e perde di più se perde (viceversa per lo sfavorito).

Esempi con 1000 contro 1100, entrambi oltre le 10 partite (`E = 0,36` per chi ha 1000):

| Vince | 1000 | 1100 |
|---|---|---|
| lo sfavorito (1000) | **+13** | **−13** |
| il favorito (1100) | **−7** | **+7** |

## La chiamata all'engine

`EngineClient` è l'unico punto che conosce l'engine. Usa `RestClient`, URL da configurazione, timeout di 10 secondi.

```java
@Component
@RequiredArgsConstructor
public class EngineClient {

	private final RestClient engineRestClient;

	/**
	 * Chiede all'engine di simulare una partita.
	 *
	 * @throws EngineUnavailableException se l'engine non risponde o risponde con errore
	 */
	public SimulationResponse simulate(SimulationRequest request) {
		try {
			return engineRestClient.post()
					.uri("/api/v1/simulations")
					.body(request)
					.retrieve()
					.body(SimulationResponse.class);
		} catch (RestClientException e) {
			throw new EngineUnavailableException(e);
		}
	}
}
```

Cosa invia l'API (contratto completo nel modulo engine, `docs/api-engine`):
- il seed;
- le regole della partita (`rules`: versione, punti vita iniziali, limite di round) dal rule set attivo;
- le fazioni e la tabella `faction_bonuses` (l'engine non ha database);
- i due mazzi, **una voce per ogni copia**, con `effectCode` e `subtype` presi da `catalog_cards` (`effect_code`, `subtype`).

L'engine restituisce `CHALLENGER_WIN`, `OPPONENT_WIN` o `DRAW`, gli stessi valori dell'enum `BattleResult`.

## Storico

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/battle-invites?box=incoming` | inviti ricevuti in attesa |
| `GET` | `/api/v1/battle-invites?box=outgoing` | inviti inviati in attesa |
| `POST` | `/api/v1/battle-invites/{id}/accept` | accetta scegliendo il mazzo |
| `POST` | `/api/v1/battle-invites/{id}/reject` | rifiuta |
| `DELETE` | `/api/v1/battle-invites/{id}` | ritira (solo sfidante, solo `PENDING`) |
| `POST` | `/api/v1/matchmaking/queue` | entra in coda classificata con un mazzo |
| `GET` | `/api/v1/matchmaking/queue` | stato del mio ticket (`404` se non sono in coda) |
| `DELETE` | `/api/v1/matchmaking/queue` | esce dalla coda |
| `GET` | `/api/v1/battles` | partite giocate, paginate; filtro opzionale `mode` (`FRIENDLY`, `RANKED`) |
| `GET` | `/api/v1/battles/{id}` | dettaglio con log round per round |
| `POST` | `/api/v1/battles/{id}/retry` | rilancia una simulazione non completata |
| `GET` | `/api/v1/rankings/me` | il mio rating e le statistiche classificate; non c'è una classifica pubblica né una posizione (Q-10, deciso l'11 novembre) |

Elemento dello storico:

```json
{
  "battleId": "0b7e…",
  "createdAt": "2026-11-04T10:20:00Z",
  "mode": "RANKED",
  "opponent": { "id": 12, "username": "sentinella_valle" },
  "outcome": "WIN",
  "endReason": "LIFE_POINTS_ZERO",
  "myFinalLifePoints": 9,
  "opponentFinalLifePoints": 0,
  "rounds": 37,
  "ratingDelta": 13
}
```

`outcome` è calcolato dal punto di vista di chi chiede (`WIN`, `LOSS`, `DRAW`) partendo da `result`. `ratingDelta` è la variazione del rating di chi chiede, `null` per le amichevoli.

Solo i due giocatori possono vedere il dettaglio di una partita (altrimenti `404`).

Lo storico è ordinato per `battles.created_at`, dalla più recente. `rounds_log` e le fotografie dei mazzi possono essere grandi: l'elenco usa una query che legge solo i campi che servono, e `rounds_log` arriva solo dal dettaglio.

## Test minimi

- Accettazione con mazzo non più valido → `422`, invito resta `PENDING`, nessuna partita creata.
- Invito già accettato → seconda accettazione `409` (e il vincolo `unique` su `invite_id` lo garantisce comunque).
- Engine non raggiungibile → partita con `result` nullo, nessun credito assegnato.
- Retry invia lo stesso seed e gli stessi mazzi (verificare con un `EngineClient` finto).
- `outcome` corretto per entrambi i giocatori.
- Invito a un utente che non è amico → `403`, nessun invito creato.
- Amichevole completata → `player_ratings` e crediti invariati, colonne di rating di `battles` nulle.
- Retry di una classificata dopo l'attivazione di un nuovo rule set → crediti dal rule set della partita.
- Ingresso in coda due volte → `409`.
- Due abbinamenti in parallelo non usano lo stesso ticket (`SKIP LOCKED`).
- Mazzo non più valido al momento dell'abbinamento → solo quel ticket viene rimosso, nessuna partita creata.
- Elo: 1000 contro 1100 con K = 20, vince 1000 → +13 / −13; vince 1100 → −7 / +7; pareggio a rating uguale → 0.
- Retry di una classificata non completata → l'Elo viene applicato una sola volta.
- Due retry sovrapposti della stessa classificata → Elo e crediti assegnati una sola volta.
- Pareggio in classificata mentre i due giocatori accettano uno scambio → nessun deadlock, crediti e carte corretti.
- Secondo invito `PENDING` verso lo stesso avversario → `409`; un invito nella direzione opposta → ok.
- Rifiuto o ritiro di un invito → riga eliminata.
- Eliminazione del mazzo di un invito già accettato → ok, `challenger_deck_id` diventa `NULL`, la partita resta leggibile.
- Due utenti appena affrontati in classificata → non vengono abbinati di nuovo per 10 minuti, anche se sono gli unici in coda.
