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
  a1b2... |      7       |      12      | WAITING_RECIPIENT

trade_offers   (una riga per ogni carta che un partecipante DÀ)
trade_id | user_id | card_id | quantity
---------+---------+---------+---------
 a1b2... |    7    |    4    |    3      ← l'utente 7 dà 3x Crawlers
 a1b2... |   12    |   51    |    1      ← l'utente 12 dà 1x Mothman
```

- `user_id` in `trade_offers` è **chi cede** quella carta, e deve essere uno dei due partecipanti.
- Chiave primaria `(trade_id, user_id, card_id)`.

## Stati e turni

| Stato | Significato | Chi deve rispondere |
|---|---|---|
| `WAITING_RECIPIENT` | il richiedente ha fatto la proposta iniziale o una controproposta | destinatario |
| `WAITING_REQUESTER` | il destinatario ha fatto una controproposta | richiedente |
| `ACCEPTED` | scambio eseguito (finale) | — |
| `REJECTED` | scambio chiuso senza scambio (finale) | — |

```
                     controproposta (destinatario)
WAITING_RECIPIENT ──────────────────────────────────▶ WAITING_REQUESTER
                  ◀──────────────────────────────────
                     controproposta (richiedente)

Da entrambi gli stati:
   ├── accept (chi ha il turno) ──▶ ACCEPTED
   └── reject (chiunque dei due) ─▶ REJECTED
```

Regole:
- **Accetta** e **controproponi** solo chi ha il turno.
- **Rifiuta** può farlo chiunque dei due finché lo scambio è aperto (così il richiedente può anche ritirare la proposta).
- Gli scambi **non scadono**: restano aperti finché uno dei due non li chiude.
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
- blocca la riga `inventories` di chi propone;
- chi propone ha abbastanza copie **disponibili** delle carte che dà, altrimenti `422`;
- il destinatario possiede **ora** le carte richieste (si guarda solo `quantity`, per non rivelare le sue copie riservate).

## Copie riservate

Le copie offerte **restano in `collection_cards`** ma non si possono usare altrove finché lo scambio è aperto:

```
disponibili = copie possedute − copie nelle mie offerte aperte
```

Esempio: con 3 copie, offrendone 2 ne resta 1 disponibile. Un altro scambio può offrirne al massimo 1, e un mazzo può usarne al massimo 1.

Sono riservate solo le carte di **chi ha fatto l'offerta in corso**: altrimenti chiunque potrebbe congelare la collezione di un altro utente mandandogli proposte. Lo dice lo stato:

| `status` | Le carte riservate sono di |
|---|---|
| `WAITING_RECIPIENT` | richiedente |
| `WAITING_REQUESTER` | destinatario |

Quando lo scambio si chiude, le sue righe smettono di contare e le copie tornano disponibili: non c'è niente da restituire. Con una controproposta la riserva passa da sola all'altro utente.

```java
/** Copie di una carta riservate da un utente nelle sue offerte aperte. */
@Query("""
		select coalesce(sum(o.quantity), 0) from TradeOffer o join o.trade t
		where o.userId = :userId and o.cardId = :cardId
		  and ((t.status = WAITING_RECIPIENT and t.requesterId = :userId)
		    or (t.status = WAITING_REQUESTER and t.recipientId = :userId))
		""")
int countReserved(Integer userId, Integer cardId);
```

## Controproposta

`POST /api/v1/trades/{id}/counter` con lo stesso formato (`give` / `receive` dal punto di vista di chi controproponde).

Nella stessa transazione:
1. controlla stato e turno, blocca la riga `inventories` di chi controproponde e controlla le sue copie disponibili, senza contare quelle riservate da questo scambio;
2. **sostituisce** tutte le righe di `trade_offers` dello scambio con le nuove;
3. passa il turno: `WAITING_REQUESTER` se controproponde il destinatario, `WAITING_RECIPIENT` se controproponde il richiedente.

## Accettazione (la parte delicata)

`POST /api/v1/trades/{id}/accept` — tutto in **una transazione**:

1. Blocca la riga dello scambio (`PESSIMISTIC_WRITE`), controlla stato e turno. Se l'altro partecipante è disattivato (`is_enabled = false`) → `422`, lo scambio resta aperto.
2. Blocca la riga `inventories` di **entrambi** gli utenti, **in ordine di id utente crescente** (evita il deadlock se due scambi incrociati vengono accettati insieme). È lo stesso blocco dell'apertura di un pacchetto, quindi vale anche per le carte che l'utente non possiede ancora.
3. Controlla che chi accetta abbia abbastanza copie **disponibili** delle carte che dà. Se no → `422`, lo scambio resta aperto. Le carte di chi ha fatto l'offerta sono già riservate.
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

Si possono offrire anche carte usate in un mazzo. Finché le copie sono riservate, quel mazzo risulta `valid: false`; se lo scambio si chiude senza accordo torna valido da solo. Dopo uno scambio accettato, i mazzi che usavano le carte cedute restano non validi: non si modificano automaticamente e l'utente dovrà sistemarli (vedi [mazzi](../mazzi/mazzi.md)).

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
- Con 3 copie, offrirne 2 in uno scambio e altre 2 in un secondo → il secondo riceve `422`.
- Due proposte contemporanee sulle stesse copie → una sola riesce.
- Ritiro o rifiuto di uno scambio → le copie tornano subito disponibili.
- Controproposta → le copie riservate passano all'altro utente.
- Controproposta che conserva una carta dell'offerta precedente → salvataggio riuscito (si aggiornano le righe esistenti: svuotando e reinserendo, Hibernate esegue gli insert prima dei delete e viola la chiave primaria).
- Chi accetta non ha più copie disponibili → `422`, niente cambia.
- Accettazione con l'altro partecipante disattivato → `422`, niente cambia.
- Scambio accettato mentre chi riceve apre un pacchetto con la stessa carta → nessuna copia persa.
- Doppia accettazione dello stesso scambio → la seconda dà `409`.
- Test di concorrenza: due scambi incrociati accettati nello stesso momento → nessun deadlock, nessuna carta duplicata.
