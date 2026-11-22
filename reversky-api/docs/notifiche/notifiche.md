# Notifiche

## Cosa fa

Gli utenti ricevono una notifica per gli eventi che li riguardano: richieste di amicizia, inviti e partite, abbinamenti in classificata, scambi. Ogni notifica si **salva** nella tabella `notifications`, così il client la ritrova anche se non era collegato quando è nata, e si **invia in tempo reale** sul canale Server-Sent Events (SSE), senza polling (decisione del 9 novembre, chiude Q-16).

Package `notification/`. Tabella `notifications` (baseline `V8`, #141): `id`, `user_id`, `type` (testo), `payload` (`jsonb`), `read_at` (nullo = non letta), `created_at`.

## Tipi e payload

Il tipo è testo e non un enum del database: un tipo nuovo si aggiunge in `NotificationType` senza migrazione. Il nome del tipo è anche il nome dell'evento SSE.

Il `payload` contiene **solo** gli id delle entità coinvolte e `userId` e `username` dell'altro utente, mai altri dati personali (deciso il 22 novembre): il client può mostrare il messaggio senza un'altra chiamata.

| Tipo | A chi | Payload | Task |
|---|---|---|---|
| `FRIEND_REQUEST_RECEIVED` | destinatario della richiesta | `userId`, `username` (chi la manda) | API-67 |
| `FRIEND_REQUEST_ACCEPTED` | chi ha mandato la richiesta | `userId`, `username` (chi accetta) | API-67 |
| `BATTLE_INVITE_RECEIVED` | avversario invitato | `inviteId`, `userId`, `username` (sfidante) | API-68 |
| `BATTLE_INVITE_ACCEPTED` | sfidante | `inviteId`, `battleId`, `userId`, `username` (avversario) | API-68 |
| `MATCH_FOUND` | i due giocatori abbinati in classificata | `battleId`, `userId`, `username` (l'altro) | API-68 |
| `BATTLE_COMPLETED` | i due giocatori, quando la partita ha un risultato (anche dopo un retry) | `battleId`, `mode`, `outcome` (di chi riceve), `userId`, `username` (l'altro) | API-68 |
| `TRADE_PROPOSED` | chi riceve la proposta | `tradeId`, `userId`, `username` (chi propone) | API-69 |
| `TRADE_COUNTERED` | chi deve rispondere alla controproposta | `tradeId`, `userId`, `username` | API-69 |
| `TRADE_ACCEPTED` | l'altra parte | `tradeId`, `userId`, `username` | API-69 |
| `TRADE_REJECTED` | l'altra parte | `tradeId`, `userId`, `username` | API-69 |

Rifiutare o ritirare un'amicizia o un invito non crea notifiche. Una partita che resta senza risultato (engine non raggiungibile) non invia `BATTLE_COMPLETED`.

## Regole

1. `NotificationService.notify(userId, type, payload)` lavora **dentro la transazione del chiamante** (`Propagation.MANDATORY`): se la transazione viene annullata la notifica non resta. Senza una transazione aperta è un errore di programmazione.
2. L'evento SSE parte **solo dopo il commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`, API-66): una transazione annullata non invia niente.
3. Un utente vede e segna come lette solo le proprie notifiche: quella di un altro è `404 NOTIFICATION_NOT_FOUND`, come una che non esiste.
4. Le notifiche lette più vecchie di `reversky.notifications.retention` (30 giorni) si eliminano ogni notte; quelle non lette restano sempre.

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/notifications` | le mie notifiche, paginate, dalla più recente; `?unread=true` solo le non lette |
| `GET` | `/api/v1/notifications/unread-count` | `{ "count": 3 }` |
| `POST` | `/api/v1/notifications/{id}/read` | segna letta una mia notifica, `204` (anche se era già letta) |
| `POST` | `/api/v1/notifications/read-all` | segna lette tutte le mie notifiche, `204` |
| `GET` | `/api/v1/notifications/stream` | canale SSE (`text/event-stream`, API-66) |

Una notifica:

```json
{
  "id": 42,
  "type": "TRADE_PROPOSED",
  "payload": { "tradeId": "0b7e…", "userId": 7, "username": "bob" },
  "read": false,
  "createdAt": "2026-11-23T10:00:00Z"
}
```

## Canale SSE (API-66)

Decisioni del 22 novembre:

- **Autenticazione con l'header `Authorization: Bearer`**, come ogni altra chiamata: l'`EventSource` nativo del browser non può mandarlo, quindi il frontend apre il canale con un client basato su `fetch` (es. `@microsoft/fetch-event-source`). Niente token nell'URL.
- Il canale si chiude quando scade l'access token che l'ha aperto (15 minuti): il client si ricollega con un token nuovo e `Last-Event-ID`, e riceve le notifiche perse.
- Ogni evento ha `id` = id della notifica, `event` = tipo, `data` = la stessa notifica JSON della lista; ogni 30 secondi un commento `: ping` tiene aperta la connessione dietro ai proxy.

```
id: 42
event: TRADE_PROPOSED
data: {"id":42,"type":"TRADE_PROPOSED","payload":{...},"read":false,"createdAt":"..."}

: ping
```

## Comportamento già implementato

### Notifiche: entità e consultazione (API-65 #135)

- `notification/entity/Notification` (`type` come `NotificationType` salvato in testo, `payload` `jsonb` come mappa) e `NotificationRepository`, sempre in ordine di id decrescente (indice `idx_notifications_user_id_id`).
- `NotificationService.notify` con `Propagation.MANDATORY`: salva la notifica nella transazione del chiamante; senza transazione `IllegalTransactionStateException`. I collegamenti con amicizie, sfide e scambi arrivano con API-67…69.
- `NotificationController`: `GET /notifications` (paginato come le altre liste, il `sort` del client si ignora), `GET /notifications/unread-count`, `POST /notifications/{id}/read` (idempotente, tiene la prima ora di lettura) e `POST /notifications/read-all`.
- `notification/job/NotificationCleanupJob` ogni notte alle 3:30 elimina le notifiche lette create prima di adesso meno `reversky.notifications.retention` (`NotificationProperties`, 30 giorni in `application.properties`: configurazione tecnica, non una regola di gioco).

| Caso | Status | `code` |
|---|---|---|
| notifica inesistente o di un altro utente | `404` | `NOTIFICATION_NOT_FOUND` |

### Canale SSE (API-66 #136)

- `GET /api/v1/notifications/stream` (`NotificationController`, `text/event-stream`) apre una connessione con `NotificationStreamService.open`. Le connessioni stanno in memoria, per utente (più schede = più connessioni): con più istanze dell'API ognuna invierebbe solo alle sue, e per la consegna gira un'istanza sola.
- `notify` pubblica un `NotificationCreatedEvent`; `NotificationStreamService` lo riceve con `@TransactionalEventListener(AFTER_COMMIT)` e lo invia a tutte le connessioni del destinatario: una transazione annullata non invia niente.
- La connessione ha come timeout il tempo che resta all'access token della richiesta (`jwt.getExpiresAt()`), mai 0, che per Spring vorrebbe dire nessun timeout.
- `Last-Event-ID` (un numero, altrimenti `400`) rimanda le notifiche create dopo quell'id, al massimo le 100 più recenti, dalla più vecchia. La connessione si registra prima di rimandarle: una notifica nata in quel momento può arrivare due volte, ma non si perde; il client ignora un id già visto.
- `NotificationHeartbeatJob` manda `: ping` ogni 30 secondi su ogni connessione; un invio che fallisce (`IOException` o connessione già chiusa) toglie la connessione. Timeout, errore e chiusura la tolgono allo stesso modo.
- Disattivare un account (`UserAdminService.disable`) pubblica `UserDisabledEvent`: dopo il commit si chiudono tutte le sue connessioni. Aprire il canale legge `is_enabled` (una query per connessione): un account disattivato riceve `403 AUTH_ACCOUNT_DISABLED` anche con un access token ancora valido (deciso il 22 novembre; gli altri endpoint restano come prima).
- Un errore prima di aprire il canale risponde in `ProblemDetail` JSON come gli altri, anche con `Accept: text/event-stream` (provato con il `403` in `NotificationStreamIT`).

| Caso | Status | `code` |
|---|---|---|
| account disattivato | `403` | `AUTH_ACCOUNT_DISABLED` |
| `Last-Event-ID` non numerico | `400` | |

## Test minimi

- Un utente vede e segna come lette solo le proprie notifiche; quella di un altro → `404` (`NotificationControllerIT`).
- Una notifica creata in una transazione annullata non resta (`NotificationServiceIT`).
- La pulizia elimina solo le lette e vecchie (`NotificationServiceIT`, `NotificationCleanupJobTest`).
- Un evento arriva solo al destinatario, su tutte le sue connessioni; una transazione annullata non invia eventi; `Last-Event-ID` rimanda le perse; un account disattivato perde le connessioni e non ne apre di nuove (`NotificationStreamIT`, `NotificationStreamServiceTest`, API-66).
- Ogni evento di amicizie, sfide e scambi crea una notifica e la invia (API-67…69).
