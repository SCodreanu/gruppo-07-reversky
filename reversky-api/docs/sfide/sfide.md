# Sfide e storico

## Cosa fa

L'utente A (sfidante) invita l'utente B (avversario) scegliendo uno dei propri mazzi. Quando B accetta scegliendo a sua volta un mazzo, la partita viene **simulata subito dall'engine** e il risultato (round per round) è visibile a entrambi. Ogni partita finisce nello **storico**.

L'API **non calcola** la partita: prepara i dati, chiama `reversky-engine`, salva il risultato.

Tabelle: `battle_invites`, `battles`.

## Regolamento

Esiste **un solo regolamento**: creature con abilità, Spell, Hidden e Terreni fanno tutti parte del gioco standard. Non ci sono modalità da scegliere (decisione concordata con il cliente).

## Invito — `battle_invites`

| Stato | Significato |
|---|---|
| `PENDING` | in attesa della risposta dell'avversario |
| `ACCEPTED` | accettato: esiste la partita in `battles` |
| `REJECTED` | rifiutato dall'avversario |

```
PENDING ──accept (B)──▶ ACCEPTED ──▶ battles (1 sola partita per invito)
   ├──reject (B)──────▶ REJECTED
   └──ritiro (A)──────▶ riga eliminata
```

Lo sfidante può ritirare un invito ancora `PENDING`: la riga viene eliminata (non c'è uno stato dedicato).

### Creazione

`POST /api/v1/battle-invites`

```json
{ "opponentId": 12, "deckId": 3 }
```

Controlli:
- niente sfide a se stessi (`chk_battle_invites_no_self_challenge`);
- il mazzo è mio ed è **valido**;
- l'avversario esiste ed è attivo (`is_enabled`).

## Accettazione e simulazione

`POST /api/v1/battle-invites/{id}/accept` con `{ "deckId": 8 }`

Il flusso è diviso in **tre passi**, per non tenere aperta una transazione mentre si aspetta l'engine:

```
1) Transazione 1 — preparazione
   - blocca l'invito, controlla che sia PENDING e che io sia l'avversario
   - valida di nuovo ENTRAMBI i mazzi (le collezioni possono essere cambiate)
   - genera il seed con SecureRandom
   - crea la riga in battles:
       challenger_deck / opponent_deck = "fotografia" dei mazzi (jsonb)
       battle_stats = { seed }
       rounds_log   = []
       result       = NULL           ← simulazione non ancora completata
   - invito → ACCEPTED

2) Chiamata HTTP all'engine (fuori transazione)
   EngineClient.simulate(request)

3) Transazione 2 — salvataggio
   - result, battle_stats completo, rounds_log
   - crediti di vittoria/pareggio (vedi crediti)
```

Se l'engine non risponde o dà errore, la partita resta con `result = NULL`. `POST /api/v1/battles/{id}/retry` rilancia la simulazione **con lo stesso seed e la stessa fotografia dei mazzi**, quindi il risultato non può essere "rigiocato" per ottenerne uno migliore.

### Perché la fotografia dei mazzi

Le statistiche delle carte possono cambiare dal backoffice (bilanciamento). Una partita giocata deve restare identica nello storico, quindi `challenger_deck` e `opponent_deck` salvano le carte **con i valori del momento**.

```json
[
  { "cardId": 4, "name": "Crawlers", "type": "CREATURE", "rarity": "Common",
    "factionId": 1, "atk": 4, "def": 5, "tags": [], "quantity": 3 }
]
```

### Perché il seed lo genera l'API

- Il mescolamento deve avvenire lato server e nessun giocatore deve poterlo influenzare (requisito del brief).
- Salvando il seed, la stessa partita si può rigiocare identica per verificare un bug.

### Contenuto di `battle_stats`

```json
{
  "seed": 8812734650912,
  "rulesVersion": "1.0",
  "endReason": "LIFE_POINTS_ZERO",
  "totalRounds": 37,
  "challenger": { "finalLifePoints": 9, "creaturesDefeated": 14 },
  "opponent":   { "finalLifePoints": 0, "creaturesDefeated": 11 }
}
```

`rounds_log` contiene la lista di eventi restituita dall'engine.

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
- le fazioni e la tabella `faction_bonus` (l'engine non ha database);
- i due mazzi, **una voce per ogni copia**, con `effectCode` e `subtype` ricavati dai tag (`EFFECT_*`, `DAMAGE`, `SHIELD`, …).

L'engine restituisce `CHALLENGER_WIN`, `OPPONENT_WIN` o `DRAW`, gli stessi valori dell'enum `BattleResult`.

## Storico

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/battle-invites?box=incoming` | inviti ricevuti in attesa |
| `GET` | `/api/v1/battle-invites?box=outgoing` | inviti inviati in attesa |
| `POST` | `/api/v1/battle-invites/{id}/accept` | accetta scegliendo il mazzo |
| `POST` | `/api/v1/battle-invites/{id}/reject` | rifiuta |
| `DELETE` | `/api/v1/battle-invites/{id}` | ritira (solo sfidante, solo `PENDING`) |
| `GET` | `/api/v1/battles` | partite giocate, paginate |
| `GET` | `/api/v1/battles/{id}` | dettaglio con log round per round |
| `POST` | `/api/v1/battles/{id}/retry` | rilancia una simulazione non completata |

Elemento dello storico:

```json
{
  "battleId": "0b7e…",
  "opponent": { "id": 12, "username": "sentinella_valle" },
  "outcome": "WIN",
  "endReason": "LIFE_POINTS_ZERO",
  "myFinalLifePoints": 9,
  "opponentFinalLifePoints": 0,
  "rounds": 37
}
```

`outcome` è calcolato dal punto di vista di chi chiede (`WIN`, `LOSS`, `DRAW`) partendo da `result`.

Solo i due giocatori possono vedere il dettaglio di una partita (altrimenti `404`).

Lo storico è ordinato per `battles.created_at`, dalla più recente.

## Test minimi

- Accettazione con mazzo non più valido → `422`, invito resta `PENDING`, nessuna partita creata.
- Invito già accettato → seconda accettazione `409` (e il vincolo `unique` su `invite_id` lo garantisce comunque).
- Engine non raggiungibile → partita con `result` nullo, nessun credito assegnato.
- Retry invia lo stesso seed e gli stessi mazzi (verificare con un `EngineClient` finto).
- `outcome` corretto per entrambi i giocatori.
