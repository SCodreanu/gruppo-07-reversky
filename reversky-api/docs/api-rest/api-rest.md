# Convenzioni REST

Regole comuni a tutti gli endpoint. Il frontend (WMD) si basa su queste convenzioni: non cambiarle senza avvisare.

## URL

- Prefisso: `/api/v1`.
- Risorse al **plurale**, in inglese, minuscolo, parole separate da `-`.
- Azioni che non sono CRUD: `POST` su un sotto-percorso con un verbo.

```
GET    /api/v1/decks              lista dei miei mazzi
POST   /api/v1/decks              crea un mazzo
GET    /api/v1/decks/{id}         dettaglio
PUT    /api/v1/decks/{id}         modifica
DELETE /api/v1/decks/{id}         elimina
POST   /api/v1/trades/{id}/accept azione su uno scambio
```

## Metodi e codici di stato

| Caso | Status |
|---|---|
| Lettura riuscita | `200 OK` |
| Creazione riuscita | `201 Created` |
| Azione senza contenuto da restituire (es. delete) | `204 No Content` |
| Dati della richiesta non validi (campi mancanti, formati) | `400 Bad Request` |
| Non autenticato / token non valido | `401 Unauthorized` |
| Autenticato ma senza permesso | `403 Forbidden` |
| Risorsa inesistente **o non visibile all'utente** | `404 Not Found` |
| Stato non compatibile (scambio già chiuso o fuori turno, invito già accettato) | `409 Conflict` |
| Regola di gioco violata (mazzo non valido, crediti insufficienti) | `422 Unprocessable Content` |
| Engine non raggiungibile | `503 Service Unavailable` |

## Formato degli errori

Usiamo `ProblemDetail` di Spring (standard RFC 9457). Tutti gli errori hanno questa forma:

```json
{
  "type": "about:blank",
  "title": "Invalid deck",
  "status": 422,
  "detail": "The deck contains 3 copies of 'Hounds', maximum allowed: 1",
  "instance": "/api/v1/decks",
  "code": "DECK_INVALID",
  "errors": [
    { "field": "cards", "message": "Too many copies of 'Hounds' (3/1)" }
  ]
}
```

- `code`: codice stabile in inglese che il frontend può usare per tradurre o gestire il caso.
- `title` e `detail`: testo leggibile in inglese (il prodotto è English first).
- `errors`: presente solo per errori di validazione con più problemi.

## Paginazione

Le liste che possono crescere (collezione, storico partite, scambi, audit) sono paginate:

```
GET /api/v1/battles?page=0&size=20
```

Risposta:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

## Formati dei dati

- JSON con campi in `camelCase`.
- Date in ISO-8601 UTC: `"2026-11-01T16:30:00Z"`.
- Enum come stringhe maiuscole: `"LEGENDARY"`, `"PENDING"`.
- Id numerici (`Integer`) oppure UUID come stringa, secondo la tabella (vedi [database](../database/database.md)).

## Autenticazione

Header su ogni richiesta protetta:

```
Authorization: Bearer <token>
```

Dettagli in [autenticazione](../autenticazione/autenticazione.md).

## Swagger / OpenAPI

Il brief richiede la documentazione Swagger. Si usa **springdoc-openapi** (da aggiungere al `build.gradle`, versione compatibile con Spring Boot 4).

Ogni controller deve avere:

```java
@Tag(name = "Decks", description = "Deck creation and management")
@RestController
@RequestMapping("/api/v1/decks")
public class DeckController {

	@Operation(summary = "Create a new deck",
			description = "Validates the deck against the game rules and saves it.")
	@ApiResponse(responseCode = "201", description = "Deck created")
	@ApiResponse(responseCode = "422", description = "The deck does not meet the constraints")
	@PostMapping
	public ResponseEntity<DeckResponse> create(@Valid @RequestBody CreateDeckRequest request) { ... }
}
```

## Elenco degli endpoint principali

| Area | Endpoint |
|---|---|
| Auth | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/password/forgot`, `/auth/password/reset`, `/auth/email/verify`, `/auth/email/resend` |
| Utente | `GET /users/me`, `GET /users/{id}/collection` |
| Inventario | `GET /inventory`, `POST /inventory/packs/{packId}/open` |
| Carte | `GET /cards`, `GET /cards/{id}`, `GET /rarities`, `GET /factions` |
| Regole | `GET /rules` (rule set attivo e bonus di fazione, per la pagina del regolamento) |
| Pacchetti | `GET /packs`, `POST /packs/{id}/purchase` |
| Collezione | `GET /collection`, `GET /collection/summary` |
| Mazzi | `GET/POST /decks`, `GET/PUT/DELETE /decks/{id}`, `POST /decks/validate`, `GET /decks/rules` |
| Amicizie | `GET /friends`, `GET/POST /friends/requests`, `POST /friends/requests/{requesterId}/accept`, `/reject`, `DELETE /friends/{userId}` |
| Scambi | `GET/POST /trades`, `GET /trades/{id}`, `POST /trades/{id}/counter`, `/accept`, `/reject` |
| Sfide | `GET/POST /battle-invites`, `POST /battle-invites/{id}/accept`, `/reject`, `DELETE /battle-invites/{id}`, `GET /battles`, `GET /battles/{id}`, `POST /battles/{id}/retry` |
| Classificate | `GET/POST/DELETE /matchmaking/queue`, `GET /rankings`, `GET /rankings/me` |
| Backoffice | `/admin/cards/**`, `/admin/rarities`, `/admin/factions`, `/admin/faction-bonus`, `/admin/rule-sets/**`, `/admin/tags`, `/admin/packs/**`, `/admin/users/**`, `/admin/audit-logs`, `/admin/stats` |

(tutti con prefisso `/api/v1`)
