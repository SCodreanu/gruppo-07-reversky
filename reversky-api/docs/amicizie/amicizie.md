# Amicizie

## Cosa fa

Gli utenti possono mandarsi richieste di amicizia. La lista amici serve a trovare facilmente con chi sfidarsi o scambiare carte e a costruire la community.

Le **sfide amichevoli** (inviti, vedi [sfide](../sfide/sfide.md)) sono possibili **solo tra amici**. Le partite classificate e gli scambi non richiedono l'amicizia.

Tabella: `friendships`, chiave primaria `(requester_id, recipient_id)`.

## Stati

| Stato | Significato |
|---|---|
| `PENDING` | richiesta inviata, in attesa |
| `ACCEPTED` | sono amici |

```
PENDING ──accept (destinatario)──▶ ACCEPTED ──rimozione──▶ riga eliminata
   └────reject (destinatario)──▶ riga eliminata
```

## Regole

1. Niente amicizia con se stessi (`chk_friendships_no_self_friendship`).
2. **Richiesta inversa:** se B manda una richiesta ad A mentre esiste già A → B `PENDING`, non si crea una seconda riga: la richiesta di A viene **accettata**.
3. Se esiste già un'amicizia `ACCEPTED` in una delle due direzioni → `409`.
4. Rifiutare elimina la riga: si può riprovare con una nuova richiesta, in entrambe le direzioni.
5. **Una riga per coppia.** Tra due utenti esiste al massimo una riga, in qualunque direzione (`uq_friendships_pair`). Se due richieste opposte partono nello stesso momento, la seconda riceve `409`: ripetendola si applica la regola 2.
6. Per sapere se A e B sono amici si controllano **entrambe** le direzioni.
7. Rimuovere un amico elimina la riga e anche gli inviti di sfida `PENDING` tra i due, in entrambe le direzioni.
8. L'amicizia si controlla sia quando si crea un invito di sfida sia quando lo si accetta.

```java
/** Cerca la relazione tra due utenti, in qualunque direzione sia stata creata. */
@Query("""
		select f from Friendship f
		where (f.id.requesterId = :a and f.id.recipientId = :b)
		   or (f.id.requesterId = :b and f.id.recipientId = :a)
		""")
Optional<Friendship> findBetween(Integer a, Integer b);
```

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/friends` | i miei amici |
| `GET` | `/api/v1/friends/requests?box=incoming` | richieste ricevute in attesa |
| `GET` | `/api/v1/friends/requests?box=outgoing` | richieste inviate in attesa |
| `POST` | `/api/v1/friends/requests` | invia una richiesta (`{ "username": "..." }`) |
| `POST` | `/api/v1/friends/requests/{requesterId}/accept` | accetta |
| `POST` | `/api/v1/friends/requests/{requesterId}/reject` | rifiuta |
| `DELETE` | `/api/v1/friends/{userId}` | rimuove un amico o ritira una richiesta inviata |

## Test minimi

- Richiesta inversa con una già `PENDING` → diventa `ACCEPTED`, una sola riga.
- Richiesta a se stessi → `400`.
- Rifiuto → riga eliminata; una nuova richiesta, in entrambe le direzioni, crea una riga `PENDING`.
- Due richieste opposte contemporanee → una sola riga, la seconda riceve `409`.
- Solo il destinatario può accettare o rifiutare.
- Rimozione di un amico con un invito di sfida `PENDING` → invito eliminato.

Sono in `FriendshipServiceIT` (regole, concorrenza, inviti di sfida) e `FriendshipControllerIT` (status, forma delle risposte, `401`).

## Comportamento già implementato

Package `friendship/`: `FriendshipController`, `FriendshipService`, `FriendshipRepository`, entità `Friendship` con chiave `FriendshipId`. Tutti gli endpoint richiedono il token (`anyRequest().authenticated()` in `SecurityConfig`).

- `POST /friends/requests` cerca l'utente per `username` **esatto** (maiuscole e minuscole contano, come il vincolo `uq_users_username`). Il corpo segue le stesse regole della registrazione: da 3 a 30 caratteri tra lettere, cifre e `_`.
- `POST /friends/requests` risponde sempre `200` con la richiesta: `status` è `PENDING` per una richiesta nuova, `ACCEPTED` quando accetta una richiesta inversa (regola 2). In quel caso `requesterId` resta chi aveva inviato la prima richiesta: la riga è sempre una.
- Le risposte delle richieste contengono entrambi i lati: `requesterId`, `requesterUsername`, `recipientId`, `recipientUsername`, `status`, `createdAt`. `GET /friends` restituisce solo l'altro utente (`userId`, `username`).
- `GET /friends/requests` senza `box` vale `box=incoming`. Le due liste sono ordinate dalla richiesta più recente; le amicizie `ACCEPTED` non compaiono. `GET /friends` non ha ancora un ordine fisso (TODO nel codice).
- `accept` e `reject` cercano la riga `(requesterId, utente autenticato)`: se l'utente non è il destinatario, o la richiesta è già accettata, la riga non c'è e la risposta è `404`.
- `DELETE /friends/{userId}` elimina la riga in qualunque stato e direzione: rimuove un amico, ritira una richiesta inviata e, per il destinatario, equivale a un rifiuto. Nella stessa transazione elimina gli inviti di sfida `PENDING` tra i due, in entrambe le direzioni (regola 7), con una query nativa su `battle_invites` finché non esiste l'entità `BattleInvite`; gli inviti `ACCEPTED` restano.
- Concorrenza: `Friendship` implementa `Persistable`, quindi il salvataggio di una richiesta nuova è sempre un `INSERT`. Due richieste uguali nello stesso momento si fermano sulla chiave primaria, due opposte su `uq_friendships_pair`: in entrambi i casi la seconda riceve `409` e resta una sola riga (regola 5).
- La regola 8 (amicizia controllata negli inviti di sfida) spetta alla task delle sfide.

| Caso | Status | `code` |
|---|---|---|
| `username` mancante o non valido | `400` | `VALIDATION_FAILED` |
| richiesta a se stessi, o `DELETE` con il proprio id | `400` | `FRIENDSHIP_SELF_REQUEST` |
| `box` diverso da `incoming` e `outgoing` | `400` | `INVALID_FRIENDSHIP_BOX` |
| nessun utente con quello `username` | `404` | `USER_NOT_FOUND` |
| `accept`/`reject` senza una richiesta `PENDING` da quell'utente all'utente autenticato | `404` | `FRIENDSHIP_REQUEST_NOT_FOUND` |
| `DELETE` senza alcuna riga tra i due | `404` | `FRIENDSHIP_NOT_FOUND` |
| già amici, richiesta già inviata, o richiesta opposta partita nello stesso momento | `409` | `FRIENDSHIP_ALREADY_EXISTS` |
