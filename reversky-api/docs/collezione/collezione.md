# Collezione

## Cosa fa

Mostra all'utente le carte che possiede e quante copie ne ha. È la "fonte di verità" usata da mazzi, scambi e sfide per sapere cosa un utente possiede davvero.

## Modello

Tabella `collection_cards`, chiave primaria `(user_id, card_id)`:

```
collection_cards
user_id | card_id | quantity
--------+---------+---------
   7    |   4     |    3      ← 3 copie di "Crawlers"
   7    |   51    |    1      ← 1 copia di "Mothman"
```

- `quantity` è sempre **> 0** (`chk_collection_cards_quantity_positive`): quando arriva a 0 la riga si **elimina**.
- Entità JPA con `@EmbeddedId CollectionCardId(userId, cardId)`.

## Chi può modificarla

**Solo `CollectionService`**, con due metodi:

```java
/** Aggiunge copie di carte alla collezione (pacchetti, scambi). */
void addCards(Integer userId, Map<Integer, Integer> quantitiesByCardId);

/**
 * Rimuove copie di carte dalla collezione (scambi).
 * @throws NotEnoughCopiesException se l'utente non ha abbastanza copie
 */
void removeCards(Integer userId, Map<Integer, Integer> quantitiesByCardId);
```

Nessun altro service scrive su `collection_cards` direttamente. Così la regola "non si perdono e non si duplicano carte" sta in un posto solo.

Entrambi i metodi vanno chiamati **dentro** la transazione del chiamante (apertura pacchetto o scambio): non ne aprono una separata.

Il chiamante deve aver già bloccato la riga `inventories` di ogni utente coinvolto, in ordine di id crescente. È un solo blocco per utente: vale per tutte le sue carte e i suoi pacchetti, anche per una carta che non possiede ancora, e dura solo il tempo della transazione.

## Copie riservate

Le copie offerte in uno scambio aperto restano qui, ma non si possono offrire altrove né usare in un mazzo (vedi [scambi](../scambi/scambi.md#copie-riservate)).

## Le carte nei mazzi restano nella collezione

Mettere una carta in un mazzo **non** la toglie dalla collezione: il mazzo è solo un elenco che fa riferimento a carte possedute. Una carta può stare in più mazzi contemporaneamente.

Conseguenza: se l'utente offre o scambia una carta usata in un mazzo, quel mazzo può diventare non valido (vedi [mazzi](../mazzi/mazzi.md)).

## Endpoint

`GET /api/v1/collection` — paginato, con filtri opzionali:

| Parametro | Esempio | Effetto |
|---|---|---|
| `factionId` | `3` | solo una fazione (anche come seconda fazione) |
| `rarityId` | `4` | solo una rarità |
| `type` | `CREATURE` | solo un tipo di carta |
| `tag` | `DREAM` | solo carte con quel tag, proprio o di una delle loro fazioni (stessa regola dei pacchetti) |
| `name` | `wend` | ricerca per nome (contiene, senza maiuscole) |
| `sort` | `rarity,desc` | ordinamento |

Risposta (un elemento):

```json
{
  "card": {
    "id": 60,
    "name": "Wendigo",
    "type": "CREATURE",
    "faction": { "id": 2, "name": "Theory of the Invitation" },
    "secondaryFaction": null,
    "rarity": { "id": 5, "name": "Legendary" },
    "atk": 18,
    "def": 17,
    "subtype": null,
    "effectCode": "WENDIGO",
    "tags": [],
    "effectDescription": "At the end of each round, the opposing creature loses 1 DEF...",
    "flavorText": "Hunger and frost that consume everything.",
    "imagePath": "/images/cards/wendigo.webp"
  },
  "quantity": 1,
  "reservedQuantity": 0,
  "usedInDecks": 2
}
```

Altri endpoint:
- `GET /api/v1/collection/summary` → carte uniche possedute sul totale del catalogo, per rarità (barra di completamento). Le varianti **non** contano nel totale: conta solo la carta originale, e possedere una variante vale come possedere la sua originale (Q-08, deciso l'11 novembre);
- `GET /api/v1/users/{id}/collection` → carte di un altro utente (solo carte e quantità), per preparare uno scambio.

## Comportamento già implementato (task API-23, #93)

Package `collection/`: `CollectionCard` con `@EmbeddedId CollectionCardId(userId, cardId)`, `CollectionCardRepository`, `CollectionService`, `CollectionController`. Scelte del 20 novembre.

`addCards` e `removeCards` hanno `@Transactional(propagation = MANDATORY)`: senza la transazione del chiamante lanciano `IllegalTransactionStateException`. Una mappa vuota o una quantità minore di 1 è un errore di programmazione (`IllegalArgumentException`). `removeCards` controlla tutte le carte prima di cambiarne una e conta le copie **possedute**, non quelle disponibili: la disponibilità la controlla il chiamante (lo scambio), perché le copie che sposta sono riservate proprio da quello scambio. Copie insufficienti → `NotEnoughCopiesException`, `422 NOT_ENOUGH_COPIES`.

`GET /collection`:

- ogni carta posseduta, anche **disattivata** (`active: false`) o **variante** (con `variantOfId`), come voce a sé;
- `card` ha la stessa forma di `GET /cards/{id}` (`CatalogCardResponse`): oltre ai campi dell'esempio sopra ha `variantOfId`, `copyLimit` e `active`, e ancora niente `tags` (API-12);
- `reservedQuantity`: copie nelle mie offerte degli scambi in cui ho fatto io la proposta in corso (regola di [scambi](../scambi/scambi.md#copie-riservate)); `usedInDecks`: numero dei miei mazzi che contengono la carta. Finché non esistono le entità `Trade` (API-31) e `Deck` (API-26) si calcolano con SQL nativo su `trade_offers` e `deck_cards`;
- filtri `type`, `name`, `rarityId`, `factionId` (prima o seconda fazione) come `GET /cards`; un id inesistente dà una pagina vuota;
- `sort` ammette solo `name`, `rarity` (per `sorting_order`), `type` e `quantity`, con `asc` o `desc`; senza, per nome. A parità si ordina sempre per nome e poi per id della carta, così le pagine restano stabili. Un'altra proprietà → `400 INVALID_COLLECTION_SORT`.

`GET /collection/summary` restituisce `{ owned, total, rarities: [{ rarity: { id, name }, owned, total }] }`: `total` conta le carte **originali attive** del catalogo, `owned` quelle che possiedo direttamente o con una loro variante. Le carte disattivate non contano (così la barra arriva al 100%), le rarità senza originali attivi non compaiono, l'ordine è quello delle rarità.

`GET /users/{id}/collection` (qualsiasi utente autenticato, perché gli scambi non sono limitati agli amici): stessi filtri e ordinamento, ogni voce è `{ card, availableQuantity }`, con le copie possedute meno quelle riservate dalle sue offerte aperte, **anche 0** (il frontend le mostra disattivate). Utente inesistente o disabilitato → `404 USER_NOT_FOUND`.

| Caso | Status | `code` |
|---|---|---|
| nessun token | `401` | |
| `type` sconosciuto | `400` | `VALIDATION_FAILED` |
| `sort` su una proprietà non ammessa | `400` | `INVALID_COLLECTION_SORT` |
| utente di `/users/{id}/collection` inesistente o disabilitato | `404` | `USER_NOT_FOUND` |

## Test minimi

- `addCards` su carta già posseduta incrementa la quantità, su carta nuova crea la riga.
- `removeCards` con quantità insufficiente lancia eccezione e non modifica niente.
- `removeCards` che porta a 0 elimina la riga.
