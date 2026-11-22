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
