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

Il client controlla lo stato con `GET /api/v1/matchmaking/queue` ogni pochi secondi: `200` con `{ "status": "WAITING", "deckId": 3, "rating": 1013, "queuedAt": "…", "window": 250 }` finché è in coda, `404` quando il ticket non esiste più. A quel punto legge l'ultima partita con `GET /api/v1/battles?mode=RANKED&size=1`.

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
- Elo: 1000 contro 1100 con K = 20, vince 1000 → +13 / −13; vince 1100 → −7 / +7; pareggio a rating uguale → 0 (`EloTest`, API-46).
- Retry di una classificata non completata → l'Elo viene applicato una sola volta.
- Due retry sovrapposti della stessa classificata → Elo e crediti assegnati una sola volta.
- Pareggio in classificata mentre i due giocatori accettano uno scambio → nessun deadlock, crediti e carte corretti.
- Secondo invito `PENDING` verso lo stesso avversario → `409`; un invito nella direzione opposta → ok.
- Rifiuto o ritiro di un invito → riga eliminata.
- Eliminazione del mazzo di un invito già accettato → ok, `challenger_deck_id` diventa `NULL`, la partita resta leggibile.
- Due utenti appena affrontati in classificata → non vengono abbinati di nuovo per 10 minuti, anche se sono gli unici in coda.

## Comportamento già implementato

### EngineClient (API-35, #105)

Il JSON scambiato con l'engine è controllato da un test di contratto da entrambe le parti (`ENG-24`, #62): `EngineContractTest` qui e `SimulationContractTest` nell'engine leggono le stesse richieste e fotografie delle risposte di `reversky-engine/examples/` (vedi [test](../test/test.md#test-di-contratto-eng-24)).

- `battle/client/EngineClient` con `RestClient` di Spring (nessuna dipendenza nuova), indirizzo e timeout da `reversky.engine.base-url` (default `http://localhost:8081`, sostituibile con `ENGINE_URL`) e `reversky.engine.timeout` (10 secondi, connessione e risposta).
- `SimulationRequest` e `SimulationResponse` seguono il contratto di `reversky-engine/docs/api-engine`; il `log` resta un JSON qualsiasi, salvato così com'è.
- Engine spento, timeout o risposta `5xx` → `EngineUnavailableException`, `503 ENGINE_UNAVAILABLE`; il seed finisce nel log dell'API per riprodurre la partita.
- Risposta `4xx` → `EngineInputRejectedException`, `500 ENGINE_INPUT_REJECTED` (API-71, deciso il 22 novembre): l'engine ha rifiutato i dati della partita, un errore nostro. Il `code` e il `detail` del `ProblemDetail` dell'engine (es. `UNKNOWN_EFFECT`) vanno solo nel log, a livello `error`, con il seed. La partita resta senza risultato e la risposta porta `battleId`, ma il retry manda la stessa fotografia e riceve lo stesso `500`. L'API chiama **solo** `POST /api/v1/simulations`: gli effetti validi stanno nella tabella `effects` dell'API (API-36, vedi [backoffice](../backoffice/backoffice.md#effetti-comportamento-già-implementato-task-api-36-106)), e `GET /api/v1/effects` dell'engine non si usa.

### Controlli dell'engine e dell'API (API-71, #146)

Ogni controllo della tabella "Errori" di `reversky-engine/docs/api-engine` è già garantito dai dati dell'API, così una partita preparata da dati validi del database non riceve mai `400`:

| Controllo dell'engine | Dove lo garantisce l'API |
|---|---|
| `VALIDATION_FAILED` (campi obbligatori, `rulesVersion` vuota, regole < 1, mazzo vuoto, fazione ripetuta) | colonne `NOT NULL`, `RuleSetRequest` (`version` non vuota, punti vita e round ≥ 1, `chk_rule_sets_match`), chiave primaria di `factions`, composizione esatta del rule set nel `DeckValidator` |
| `UNKNOWN_FACTION` | `BattleService` manda **tutte** le fazioni; le carte e i bonus hanno chiavi esterne verso `factions` |
| `INVALID_FACTION_BONUS` | chiave primaria `(origin, target)`, `chk_faction_bonuses_no_self_matchup`, `chk_faction_bonuses_def_nonnegative` e i controlli di `FactionBonusService` |
| `INVALID_CARD_FACTIONS` | `chk_catalog_cards_faction_matches_type`, `chk_catalog_cards_distinct_factions`, `chk_catalog_cards_secondary_faction_creature_only` (`V14`) e `CatalogCardService` |
| `NO_CREATURES_IN_DECK` | `deck_creatures ≥ 1` nel rule set e composizione esatta nel `DeckValidator` |
| `INVALID_STATS` | `chk_catalog_cards_stats_match_type` (ATK ≥ 0, DEF ≥ 1) |
| `MISSING_SUBTYPE` | `chk_catalog_cards_subtype_matches_type` e `CardType.acceptsSubtype` |
| `MISSING_EFFECT` | `chk_catalog_cards_effect_required`, `effectCode` non vuoto nel DTO, e un codice vuoto non è in `effects` |
| `UNKNOWN_EFFECT` | `fk_catalog_cards_effect` verso la tabella `effects` (`V13`, API-36) |

Restano possibili solo dati scritti a mano nel database (es. un nome vuoto) o una tabella `effects` diversa dal registro dell'engine: in quel caso la partita risponde `500 ENGINE_INPUT_REJECTED` e il log dice quale controllo è fallito.

### Inviti amichevoli (API-37 #107, API-38 #108, API-39 #109)

- `battle/controller/BattleInviteController`, `battle/service/BattleInviteService`: `POST /battle-invites` con `{ opponentId, deckId }`, `GET /battle-invites?box=incoming|outgoing` (in attesa, dal più recente), `POST /battle-invites/{id}/reject` e `DELETE /battle-invites/{id}` (eliminano la riga, `204`).
- `battle/service/PendingInviteService` serve gli inviti `PENDING` alle altre funzionalità: `isDeckInPendingInvite` per `DeckService` (un mazzo in attesa non si elimina) e `deletePendingBetween` per `FriendshipService` (fine di un'amicizia). Dipende solo da `BattleInviteRepository`, perché `BattleInviteService` e `BattleService` usano già `DeckService` e `FriendshipService` e il contrario creerebbe un ciclo di bean.
- Controlli in quest'ordine: non a se stessi (`422 BATTLE_SELF_CHALLENGE`), avversario esistente e attivo (`404 USER_NOT_FOUND`), amici (`403 NOT_FRIENDS`, `FriendshipService.areFriends`), mazzo mio (`404 DECK_NOT_FOUND`) e valido adesso, possesso compreso (`422 DECK_INVALID` con `violations`, `DeckService.getPlayableDeck`), nessun altro invito in attesa verso lo stesso avversario (`409 INVITE_ALREADY_PENDING`, anche con due richieste contemporanee). Un invito nella direzione opposta è un'altra sfida.
- Risposta: `id` (UUID), `challenger` e `opponent` `{ id, username }`, `createdAt` e `deck { id, name }` **solo per lo sfidante**: l'avversario scopre il mazzo nella partita (deciso il 21 novembre).
- Rifiuto e ritiro di un invito che non è mio o non è più in attesa: `404 BATTLE_INVITE_NOT_FOUND`. Un `box` diverso: `400 INVALID_BOX`.

### Accettazione, simulazione e storico (API-40 #110, API-41 #111, API-42 #112, API-43 #113)

- `POST /battle-invites/{id}/accept` con `{ deckId }` (`BattleService.accept`), nei tre passi descritti sopra con `TransactionTemplate`: preparazione (blocca l'invito, amici, entrambi i mazzi validi adesso, seed dal bean `RandomGenerator`, rule set attivo, fazioni e bonus di fazione, fotografia dei mazzi, invito `ACCEPTED`), chiamata a `EngineClient` fuori transazione, salvataggio (blocca la partita e non scrive niente se è già completata). Nessun credito né rating per le amichevoli.
- La richiesta all'engine si costruisce **solo** da ciò che è salvato nella partita (`BattleService.toRequest`): seed, regole, fazioni e bonus in `battle_stats`, mazzi dalle fotografie, una voce per copia. Così il retry manda la stessa identica partita.
- Risposta (anche di `GET /battles/{id}`) dal punto di vista di chi guarda (deciso il 21 novembre): `id`, `mode`, `createdAt`, `completed`, `result`, `outcome` (`WIN`, `LOSS`, `DRAW`), `mySide` (`CHALLENGER` o `OPPONENT`, come il campo `side` del log), `endReason`, `totalRounds`, `rulesVersion`, `seed`, `me` e `opponent` (`id`, `username`, `finalLifePoints`, `creaturesDefeated`, `deck` come era alla preparazione) e `log`.
- Engine non disponibile: `503 ENGINE_UNAVAILABLE` con `battleId`; la partita resta salvata con `completed: false` e si completa con il retry.
- `GET /battles?mode=FRIENDLY|RANKED&page&size`: dalla più recente, con `battleId`, `createdAt`, `mode`, `opponent`, `completed`, `outcome`, `endReason`, punti vita finali, `rounds`, `ratingDelta` (null per le amichevoli). La query legge solo i campi che servono (`BattleHistoryRow`), senza log né mazzi.

| Caso | Status | `code` |
|---|---|---|
| invito inesistente o non per me | `404` | `BATTLE_INVITE_NOT_FOUND` |
| invito già accettato | `409` | `BATTLE_INVITE_ALREADY_ACCEPTED` |
| non più amici | `403` | `NOT_FRIENDS` |
| uno dei due mazzi non valido: l'invito resta in attesa, nessuna partita | `422` | `DECK_INVALID` |
| partita inesistente o non mia | `404` | `BATTLE_NOT_FOUND` |

### Retry (API-44 #114)

- `POST /battles/{id}/retry` (`BattleService.retry`): solo i due giocatori (`404 BATTLE_NOT_FOUND` per gli altri). Rimanda all'engine la richiesta costruita dalla partita salvata (stesso seed, stesse fotografie dei mazzi, stesse regole e bonus), anche se nel frattempo il catalogo o il rule set sono cambiati; il salvataggio blocca la riga e non riscrive una partita già completata.
- Una partita già completata si restituisce così com'è con `200`, senza chiamare l'engine (deciso il 21 novembre: il retry è idempotente). Engine ancora spento: `503 ENGINE_UNAVAILABLE` con `battleId`.

### Rating Elo (API-46 #116)

- `battle/service/Elo`: `eloDelta(rating, opponentRating, score, gamesPlayed)` come nella sezione "Elo" sopra (K 40 nelle prime 10 partite, poi 20; mai sotto 0) e `score(outcome)` (vittoria 1, pareggio 0,5, sconfitta 0). Funzioni pure, provate in `EloTest` con gli esempi 1000 contro 1100.
- `battle/entity/PlayerRating` mappa `player_ratings`, con `INITIAL_RATING` = 1000. `recordGame(outcome, delta)` applica una partita: rating, `peak_rating`, `games_played` e il contatore dell'esito, così i controlli della tabella restano veri; un rating sotto 0 è `IllegalArgumentException`. Lo userà l'aggiornamento dell'Elo della partita classificata (`API-50`, non ancora fatto): oggi nessun codice cambia i rating.
- `RankingService.createIfMissing(userId)` crea la riga con i valori iniziali (`INSERT … ON CONFLICT DO NOTHING`, quindi due ingressi in coda contemporanei non falliscono): la chiamerà l'ingresso in coda di `API-47`.
- `GET /api/v1/rankings/me` (`RankingController`), con il token: `{ rating, peakRating, gamesPlayed, wins, losses, draws }`. Chi non ha ancora una riga riceve 1000 e contatori a 0, e la riga **non** si crea (una lettura non scrive). Nessuna classifica né posizione (Q-10).

### Coda classificata (API-47 #117)

- `POST /api/v1/matchmaking/queue` con `{ deckId }` (`MatchmakingController`, `MatchmakingQueueService.join`), in una transazione: il mazzo dev'essere mio e valido adesso (`DeckService.getPlayableDeck`: `404 DECK_NOT_FOUND`, `422 DECK_INVALID` con le violazioni), poi `RankingService.createIfMissing` crea il rating al primo ingresso e il ticket salva il rating attuale e l'ora del `Clock`. Il ticket si inserisce con `INSERT … ON CONFLICT DO NOTHING`: un secondo ingresso, anche con un altro mazzo o nello stesso momento, è `409 ALREADY_IN_QUEUE`. Risponde `201`.
- `GET /api/v1/matchmaking/queue`: `{ status: "WAITING", deckId, rating, queuedAt, window }`, con la finestra calcolata adesso da `MatchmakingTicket.searchWindow` (`min(100 + 50 × (secondi / 10), 400)`, mai sotto 100 se l'orologio va indietro); senza ticket `404 QUEUE_TICKET_NOT_FOUND`.
- `DELETE /api/v1/matchmaking/queue`: `204` anche se non ero in coda (il ticket può essere appena stato abbinato o tolto); il rating resta.
- `QueueTicketService` (dipende solo dal repository, come `PendingInviteService`) toglie il ticket quando il mazzo viene modificato (`DeckService.update`, al posto della query nativa in `DeckRepository`) e quando un admin disattiva l'account (`UserAdminService.disable`). L'eliminazione del mazzo lo toglie con `ON DELETE CASCADE`.
- L'ingresso pubblica un `QueueJoinedEvent`, che dopo il commit fa partire subito un abbinamento (vedi sotto, API-49). Le regole sul mazzo in coda di `API-48` (#118) sono già qui: la issue è chiusa con questa.

### Abbinamento (API-49 #119)

- `battle/service/MatchmakingService.matchAll()` legge gli id dei ticket in ordine di `queued_at` (poi `user_id`) senza bloccare niente e prova ogni ticket nella **sua** transazione, una partita per transazione:
	1. blocca il ticket con `FOR UPDATE SKIP LOCKED` (`MatchmakingTicketRepository.lockIfFree`): se non c'è più o un'altra esecuzione lo tiene, si passa al prossimo senza aspettare;
	2. controlla di nuovo il suo mazzo con `DeckService.getPlayableDeck`; se non è più valido toglie solo questo ticket;
	3. cerca e blocca l'avversario (`lockOpponent`, anche lui `SKIP LOCKED`): un altro utente con il rating del ticket entro la finestra di adesso (`MatchmakingTicket.searchWindow`, del ticket che cerca), prima il rating più vicino, poi chi aspetta da più tempo, saltando chi ha giocato con me una `RANKED` preparata negli ultimi 10 minuti (anti-boosting, dal `Clock` dell'applicazione);
	4. controlla il mazzo dell'avversario: se non è più valido toglie il suo ticket e cerca il prossimo avversario nella stessa transazione;
	5. toglie i due ticket e prepara la partita con `BattleService.prepareRanked`: `mode = RANKED`, `invite_id` nullo (`Battle.ranked`), sfidante chi è in coda da più tempo (a pari ora l'id minore), seed, rule set attivo, fazioni, bonus e fotografie dei mazzi come nelle amichevoli.
- Dopo il commit `BattleService.simulateRanked` chiama l'engine e salva l'esito nella transazione di salvataggio delle amichevoli (blocca la partita, non riscrive una partita già completata). Se l'engine non risponde o rifiuta i dati la partita resta senza risultato, i ticket sono già tolti e uno dei due giocatori la completa con `POST /battles/{id}/retry`. Il risultato di una `RANKED` si salva insieme all'Elo (`chk_battles_ranked_rating_complete`), che arriva con `API-50`: fino ad allora una classificata abbinata resta senza risultato.
- Basta un giro: l'abbinamento toglie soltanto ticket, quindi un ticket rimasto senza avversario non ne trova uno nuovo più avanti nello stesso giro. Un errore inatteso su un ticket (es. nessun rule set attivo) si scrive nel log a livello `error`, annulla solo quel tentativo e il giro continua con il ticket dopo.
- `DeckService.getPlayableDeck` ha `noRollbackFor` per `DeckNotFoundException` e `InvalidDeckException`: legge soltanto, e senza questo l'eccezione segnerebbe come da annullare la transazione dell'abbinamento, che non potrebbe più togliere il ticket né creare la partita con l'avversario dopo (`UnexpectedRollbackException`, provato il 22 novembre). Chi lascia propagare l'eccezione (inviti, ingresso in coda) annulla la propria transazione come prima.
- `battle/job/MatchmakingJob` fa partire `matchAll` ogni 2 secondi (`fixedDelay`, quindi due giri programmati non si sovrappongono) e, con un `@TransactionalEventListener` sul `QueueJoinedEvent`, subito dopo il commit di ogni ingresso, sul `TaskScheduler` di Spring: `POST /matchmaking/queue` risponde `201` senza aspettare. Se due giri corrono insieme (il tentativo dopo un ingresso, o due istanze dell'API) li separa `SKIP LOCKED`.
- `reversky.matchmaking.enabled` (default `true`) accende il job; i test lo spengono in `src/test/resources/config/application.properties` (vedi [test](../test/test.md#configurazione-dei-test)).

### Integrazione reale con l'engine (API-54 #124)

Provata il 22 novembre sullo stack completo di Docker Compose (`compose.yaml` nella cartella principale: database, Mailpit, engine e API), con la copia di `reversky-engine` di `backend_api` uguale a quella di `backend_engine` (#212, #213). Il JSON scambiato resta controllato in automatico dal test di contratto (ENG-24); questa prova mostra che le due applicazioni vere si parlano.

#### Prova sullo stack completo

I passi si ripetono con [`scripts/prova-sfida.ps1`](../../../scripts/prova-sfida.ps1), che esegue tutte le chiamate, ferma e riavvia da solo il container dell'engine ed esce con codice `1` se un controllo fallisce. Dalla cartella principale del repository:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"     # dati di prova alice, bob e admin (vedi database)
docker compose up --build -d
powershell -ExecutionPolicy Bypass -File scripts\prova-sfida.ps1
```

Al posto della variabile si può scrivere `SPRING_PROFILES_ACTIVE=dev` nel file `.env` (vedi `.env.example`); senza, l'API parte con il profilo `default` e senza utenti di prova. Per ripartire da zero: `docker compose down -v`. La porta `8025` di Mailpit è la stessa del `compose.yaml` di `reversky-api` usato da `bootRun`: se quei container sono accesi vanno fermati prima (`docker compose -f reversky-api/compose.yaml stop`).

| # | Passo | Risultato atteso |
|---|---|---|
| 1 | login di `alice` e `bob` (dati di prova, vedi [database](../database/database.md#dati-di-prova-solo-sviluppo)), `GET /decks` e `GET /decks/{id}` | *Alice Invitation* e *Bob Wound* validi, 30 carte: 20 creature, 6 Spell, 2 Hidden (`SURVIVAL` e `DAMAGE`), 1 Terreno, 1 Oggetto |
| 2 | `alice` invita `bob` con `POST /battle-invites`, `bob` accetta con `POST /battle-invites/{id}/accept` | `200`, `completed: true`; `GET /battles/{id}` con il log da `MATCH_STARTED` a `MATCH_ENDED`, un `ROUND_STARTED` per round, e le carte speciali: `SPELL_RESOLVED`, `HIDDEN_SET` (e `HIDDEN_TRIGGERED`), `TERRAIN_PLACED`, `ITEM_PLACED` e `ITEM_CONSUMED`; `outcome` opposto per i due giocatori |
| 3 | `docker compose stop engine`, nuovo invito e accettazione | `503 ENGINE_UNAVAILABLE` con `battleId` (dopo al massimo 10 secondi, il timeout del client); `GET /battles/{battleId}` dà `completed: false` e `result` nullo |
| 4 | `docker compose start engine`, poi `POST /battles/{battleId}/retry` | `200`, partita completata con lo stesso seed salvato alla preparazione |
| 5 | secondo `POST /battles/{battleId}/retry` | `200` con la stessa partita; nel log dell'engine (`docker compose logs engine`) non compare una nuova riga `Simulazione completata` |
| 6 | scambio: `alice` propone con `POST /trades` (1 copia contro 1), `bob` contropropone con `POST /trades/{id}/counter` (1 copia contro 2), `bob` prova ad accettare, poi accetta `alice` | `201` e copia di alice riservata; `200` e riserva passata a bob; `409 TRADE_NOT_YOUR_TURN`; `200 ACCEPTED`, copie spostate (alice −2/+1, bob +2/−1), nessuna copia riservata e mazzi ancora validi |

Esito del 22 novembre, su un volume nuovo: tutti i controlli superati. Partita 1 in 39 round (`LIFE_POINTS_ZERO`, 393 eventi, 12 `SPELL_RESOLVED`, 4 `HIDDEN_SET`, 2 `TERRAIN_PLACED`, 2 `ITEM_PLACED`); partita 2 `503` a engine spento, completata dal retry in 39 round, secondo retry senza nuova chiamata all'engine; scambio accettato. Nessun bug trovato.

Due cose da sapere per la demo: i testi delle carte nel log sono ancora in italiano (nomi inglesi dai DSG nell'ultima settimana); Windows PowerShell 5.1 legge le risposte JSON come Latin-1 e mostra male le lettere accentate con `Invoke-RestMethod`, per questo lo script decodifica le risposte in UTF-8.
