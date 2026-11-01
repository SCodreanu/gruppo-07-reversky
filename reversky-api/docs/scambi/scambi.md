# Scambi

## Cosa fa

Due utenti si scambiano **sole carte**, anche in quantità diverse (es. 3 carte contro 1, 1 contro 5). Chi riceve una proposta può **accettarla**, **rifiutarla** o fare una **controproposta**. Lo scambio avviene solo quando chi deve rispondere accetta.

Requisito più importante: **nessuna carta deve essere persa o duplicata**, nemmeno con richieste contemporanee.

Tabelle: `trades`, `trade_offers`.

## Modello

```
trades
id (uuid) | requester_id | recipient_id | status
----------+--------------+--------------+-------------------
  a1b2... |      7       |      12      | PENDING

trade_offers   (una riga per ogni carta che un partecipante DÀ)
trade_id | user_id | card_id | quantity
---------+---------+---------+---------
 a1b2... |    7    |    4    |    3      ← l'utente 7 dà 3x Crawlers
 a1b2... |   12    |   51    |    1      ← l'utente 12 dà 1x Mothman
```

- `user_id` in `trade_offers` è **chi cede** quella carta, e deve essere uno dei due partecipanti.
- Coppia `(trade_id, user_id, card_id)` univoca.

## Stati e turni

| Stato | Significato | Chi deve rispondere |
|---|---|---|
| `PENDING` | proposta iniziale appena inviata | destinatario |
| `WAITING_RECIPIENT` | il richiedente ha fatto una controproposta | destinatario |
| `WAITING_REQUESTER` | il destinatario ha fatto una controproposta | richiedente |
| `ACCEPTED` | scambio eseguito (finale) | — |
| `REJECTED` | scambio chiuso senza scambio (finale) | — |

```
              controproposta (destinatario)
PENDING ──────────────────────────────────▶ WAITING_REQUESTER
   │                                          │        ▲
   │                controproposta (richiedente)       │ controproposta (destinatario)
   │                                          ▼        │
   │                                       WAITING_RECIPIENT
   │
   └── accept (chi ha il turno) ──▶ ACCEPTED
   └── reject (chiunque dei due) ─▶ REJECTED
```

Regole:
- **Accetta** e **controproponi** solo chi ha il turno.
- **Rifiuta** può farlo chiunque dei due finché lo scambio è aperto (così il richiedente può anche ritirare la proposta).
- Azione su uno scambio `ACCEPTED`/`REJECTED`, o fuori turno → `409 Conflict`.

## Proposta

`POST /api/v1/trades`

```json
{
  "recipientId": 12,
  "give":    [ { "cardId": 4,  "quantity": 3 } ],
  "receive": [ { "cardId": 51, "quantity": 1 } ]
}
```

L'API trasforma `give` in righe con `user_id = richiedente` e `receive` in righe con `user_id = destinatario`.

Controlli:
- niente scambi con se stessi (`chk_trades_no_self_trade`);
- almeno una carta per lato **[DA CONFERMARE]**;
- quantità > 0, nessuna carta ripetuta nello stesso lato;
- ognuno possiede **ora** le carte che dovrebbe dare.

Le carte **non vengono bloccate** durante l'attesa: l'utente può continuare a usarle. Per questo il possesso si ricontrolla all'accettazione.

## Controproposta

`POST /api/v1/trades/{id}/counter` con lo stesso formato (`give` / `receive` dal punto di vista di chi controproponde).

Nella stessa transazione:
1. controlla stato e turno;
2. **sostituisce** tutte le righe di `trade_offers` dello scambio con le nuove;
3. passa il turno: `WAITING_REQUESTER` se controproponde il destinatario, `WAITING_RECIPIENT` se controproponde il richiedente.

## Accettazione (la parte delicata)

`POST /api/v1/trades/{id}/accept` — tutto in **una transazione**:

1. Blocca la riga dello scambio (`PESSIMISTIC_WRITE`), controlla stato e turno.
2. Blocca le righe `collection_cards` di **entrambi** gli utenti, **in ordine di id utente crescente** (evita il deadlock se due scambi incrociati vengono accettati insieme).
3. Ricontrolla che ognuno possieda ancora le carte che dà. Se no → `422`, lo scambio resta aperto.
4. Sposta le carte tramite `CollectionService`:
   ```java
   for (TradeOffer offer : offers) {
   	Integer giver = offer.getUserId();
   	Integer taker = giver.equals(requesterId) ? recipientId : requesterId;
   	collectionService.removeCards(giver, Map.of(offer.getCardId(), offer.getQuantity()));
   	collectionService.addCards(taker, Map.of(offer.getCardId(), offer.getQuantity()));
   }
   ```
5. Imposta `ACCEPTED`.

Se un passo fallisce, la transazione fa rollback e **nessuna carta si è mossa**.

### Perché il blocco in ordine

Senza ordine: lo scambio A blocca Mario poi aspetta Luigi; lo scambio B blocca Luigi poi aspetta Mario → si bloccano a vicenda (deadlock).
Con l'ordine per id, entrambi provano a bloccare prima l'utente con id minore: uno dei due aspetta l'altro e poi procede.

### Conseguenze sui mazzi

Dopo uno scambio, i mazzi che usavano le carte cedute possono diventare non validi. Non si modificano automaticamente: risulteranno `valid: false` e l'utente dovrà sistemarli (vedi [mazzi](../mazzi/mazzi.md)).

## Endpoint

| Metodo | URL | Chi | Descrizione |
|---|---|---|---|
| `GET` | `/api/v1/trades?box=incoming` | io | scambi in cui tocca a me rispondere |
| `GET` | `/api/v1/trades?box=outgoing` | io | scambi in cui aspetto l'altro |
| `GET` | `/api/v1/trades/{id}` | partecipanti | dettaglio con le offerte attuali |
| `POST` | `/api/v1/trades` | chiunque | propone |
| `POST` | `/api/v1/trades/{id}/counter` | chi ha il turno | controproposta |
| `POST` | `/api/v1/trades/{id}/accept` | chi ha il turno | accetta |
| `POST` | `/api/v1/trades/{id}/reject` | partecipanti | rifiuta / ritira |

Un utente non partecipante che chiede uno scambio riceve `404`.

## Test minimi

- Scambio accettato: le carte passano correttamente e **il totale delle carte nel sistema non cambia**.
- Controproposta: le offerte vecchie spariscono, il turno passa all'altro.
- Accettazione fuori turno → `409`.
- Un partecipante cede le carte prima dell'accettazione → accettazione fallisce, niente cambia.
- Doppia accettazione dello stesso scambio → la seconda dà `409`.
- Test di concorrenza: due scambi incrociati accettati nello stesso momento → nessun deadlock, nessuna carta duplicata.
