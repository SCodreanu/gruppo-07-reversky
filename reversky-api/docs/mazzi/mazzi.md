# Mazzi

## Cosa fa

Ogni utente può creare, salvare, modificare ed eliminare **più mazzi**. Il server controlla che ogni mazzo rispetti il regolamento e che l'utente possieda davvero tutte le carte.

Tabelle: `decks` (nome univoco per utente), `deck_cards` (quantità per carta, sempre > 0).

## Vincoli

### Copie della stessa carta (dal database)

```
copie massime = min(rarities.deck_limit, catalog_cards.copy_limit)
```

- `rarities.deck_limit` è il limite della rarità (proposta del regolamento: Common 3, Uncommon 2, Rare 2, Mythic 1, Legendary 1, Mutant 1).
- `catalog_cards.copy_limit` permette di abbassarlo per una carta specifica (es. Spell Burn: 1). Se è nullo vale solo `deck_limit`.
- Una variante (`catalog_cards.variant_of_id`) conta insieme alla carta originale: le copie si sommano per `COALESCE(variant_of_id, id)` e vale il limite della carta originale.

### Composizione (dal rule set attivo)

Questi numeri stanno nel **rule set attivo** (tabella `rule_sets`) e si modificano dal backoffice: nessun valore è nel codice o in `application.properties`.

La composizione viene dalla scheda di progettazione del catalogo (mazzo da 30 su un catalogo di 120 carte). I numeri sono esatti: ogni categoria deve avere **esattamente** quel numero di carte. Possono ancora cambiare dopo i test di bilanciamento (API-64), sempre e solo dal backoffice.

| Regola | Valore |
|---|---|
| Carte totali (`deck_size`) | esattamente **30** |
| Creature (`deck_creatures`) | **20** |
| Spell (`deck_spells`) | **6** |
| Hidden (`deck_hidden`) | **2** |
| Oggetti (`deck_items`) | **1** |
| Terreni (`deck_terrains`) | **1** |
| Hidden con sottotipo `SURVIVAL` (`max_hidden_survival`) | massimo 2 in totale |
| Hidden con sottotipo `DAMAGE` (`max_hidden_damage`) | massimo 1 in totale |

**[DA CONFERMARE]** I `deck_limit` qui sopra e i `max_per_deck` qui sotto: si decidono con il bilanciamento (API-64).
I massimi delle Hidden per sottotipo vengono dal mazzo da 40 e restano (Q-24, deciso il 10 novembre): con 2 Hidden nel mazzo l'unico vincolo reale è "al massimo una Hidden `DAMAGE`".

### Bilanciamento (richiesto dal brief)

Serve un vincolo che impedisca di riempire il mazzo solo con le carte più forti. Sta in `rarities.max_per_deck` (backoffice; vuoto = nessun limite):

| Regola | Valore |
|---|---|
| Legendary totali | massimo 2 |
| Mythic totali | massimo 2 **[DA CONFERMARE]** |
| Mutant totali | massimo 1 **[DA CONFERMARE]** |
| Rare totali | massimo 5 **[DA CONFERMARE]** |

Il Terreno e l'Oggetto sono carte Rare e occupano ciascuno uno dei posti del massimo delle Rare.

Non c'è nessun vincolo tra la fazione del Terreno o dell'Oggetto e le fazioni delle creature del mazzo.

La distribuzione "circa 10 Common, 7 Uncommon…" del regolamento è solo indicativa: **non** si valida.

### Possesso e disponibilità

- Per ogni carta: `quantità nel mazzo ≤ copie disponibili`, cioè le copie in `collection_cards` meno quelle riservate da uno scambio aperto (vedi [scambi](../scambi/scambi.md#copie-riservate)).
- Le carte con `is_active = false` (disattivate dal backoffice) **non si possono usare**: un mazzo che ne contiene una non è valido finché non la si toglie (Q-07, deciso l'11 novembre). Chi la possiede la conserva nella collezione.

## Come si valida

`DeckValidator` è una classe a sé, senza accesso al database: riceve il contenuto del mazzo (con rarità, tipo, sottotipo e stato attivo di ogni carta), i limiti e la collezione, e restituisce **tutti** gli errori trovati (non si ferma al primo). Così il frontend può mostrarli tutti insieme.

```java
public record DeckViolation(String code, String message, Integer cardId) { }

public List<DeckViolation> validate(Map<CatalogCard, Integer> deckCards, Map<Integer, Integer> availableByCardId) {
	List<DeckViolation> violations = new ArrayList<>();
	checkTotalSize(deckCards, violations);
	checkCompositionByType(deckCards, violations);
	checkCopyLimits(deckCards, violations);
	checkBalanceLimits(deckCards, violations);
	checkOwnership(deckCards, availableByCardId, violations);
	checkActiveCards(deckCards, violations);
	return violations;
}
```

Codici di errore (usati dal frontend):

| Codice | Esempio di messaggio |
|---|---|
| `DECK_CARD_NOT_FOUND` | The card 999 does not exist |
| `DECK_SIZE` | The deck has 28 cards, 30 are required |
| `DECK_CREATURE_COUNT` | The deck needs exactly 20 creatures, it has 19 |
| `DECK_SPELL_COUNT` | The deck needs exactly 6 Spells, it has 7 |
| `DECK_HIDDEN_COUNT` | The deck needs exactly 2 Hidden cards, it has 1 |
| `DECK_HIDDEN_SUBTYPE_LIMIT` | Too many SURVIVAL Hidden cards (3/2) |
| `DECK_ITEM_COUNT` | The deck needs exactly 1 Item, it has 0 |
| `DECK_TERRAIN_COUNT` | The deck needs exactly 1 Terrain, it has 2 |
| `DECK_COPY_LIMIT` | Too many copies of 'Hounds' (2/1) |
| `DECK_BALANCE_LIMIT` | Too many Legendary cards (3/2) |
| `DECK_NOT_OWNED` | You have 1 available copy of 'Kappa', the deck contains 2 |
| `DECK_INACTIVE_CARD` | 'Kappa' is no longer available and must be removed from the deck |

## Quando si valida

1. **Al salvataggio** (`POST` / `PUT`): se ci sono errori → `422`, il mazzo non si salva.
2. **Alla lettura**: il campo `valid` viene ricalcolato, perché nel frattempo l'utente potrebbe aver scambiato o offerto una carta.
3. **Prima di una sfida**: alla creazione dell'invito e all'accettazione; per le classificate, all'ingresso in coda e al momento dell'abbinamento. Un mazzo non valido non può giocare.

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/decks` | i miei mazzi (con `valid` e numero carte) |
| `POST` | `/api/v1/decks` | crea |
| `GET` | `/api/v1/decks/{id}` | dettaglio con carte |
| `PUT` | `/api/v1/decks/{id}` | sostituisce nome e contenuto (se il mazzo è in coda classificata, il ticket viene rimosso) |
| `DELETE` | `/api/v1/decks/{id}` | elimina (non se usato in un invito `PENDING` → `409`; se è in coda classificata, il ticket viene rimosso) |
| `POST` | `/api/v1/decks/validate` | valida senza salvare (per il deck builder) |
| `GET` | `/api/v1/decks/rules` | limiti attuali del mazzo (rule set attivo + `deck_limit` e `max_per_deck` delle rarità), così il frontend non li duplica |

Richiesta di creazione:

```json
{
  "name": "Pioggia di Archivio",
  "cards": [
    { "cardId": 4,  "quantity": 3 },
    { "cardId": 51, "quantity": 1 }
  ]
}
```

## Regole

- Un utente vede e modifica **solo i propri mazzi** (mazzo di un altro → `404`).
- Nome: 1–50 caratteri, univoco per utente (vincolo `(user_id, name)`); duplicato → `409`.
- Gli inviti già accettati non bloccano l'eliminazione: il loro `challenger_deck_id` diventa `NULL` (`ON DELETE SET NULL`). Le partite conservano la propria fotografia del mazzo.

## Test minimi

Un test per **ogni** regola, sia nel caso valido sia al limite:
- esattamente `deck_size` carte → ok; una in meno o una in più → errore;
- copie = `min(deck_limit, copy_limit)` → ok; una in più → errore;
- carta con `copy_limit` più basso della rarità → vale il `copy_limit`;
- carta con `copy_limit` nullo → vale `deck_limit`;
- carta originale e sua variante → le copie si sommano;
- 2 Legendary diverse → ok; 3 → errore;
- per ogni categoria (creature, Spell, Hidden, Oggetti, Terreni) il numero esatto → ok; una in meno o una in più → errore;
- il Terreno e l'Oggetto contano nel massimo delle Rare;
- carta non posseduta → errore;
- carta disattivata (`is_active = false`) → errore `DECK_INACTIVE_CARD`; riattivata → ok;
- 3 copie, 2 riservate da uno scambio: 1 nel mazzo → ok; 2 → errore; chiuso lo scambio, 2 → ok;
- mazzo con più errori → li restituisce tutti;
- eliminazione di un mazzo usato in un invito `PENDING` → `409`;
- eliminazione di un mazzo usato in un invito già accettato → ok, `challenger_deck_id` diventa `NULL`;
- modifica o eliminazione di un mazzo in coda classificata → ticket rimosso;
- eliminazione di un mazzo → le sue righe di `deck_cards` vengono eliminate;
- modifica di un mazzo che conserva alcune carte → salvataggio riuscito (si aggiornano le righe esistenti: svuotando e reinserendo, Hibernate esegue gli insert prima dei delete e viola la chiave primaria).

## Comportamento già implementato

### Mazzi: nome, proprietà ed eliminazione (API-26, #96)

- Package `deck/`: entità `Deck`, `DeckService`, `DeckController`. Ogni metodo riceve l'utente dal token e cerca il mazzo per id **e** utente, quindi il mazzo di un altro è `404 DECK_NOT_FOUND`.
- `GET /decks` in ordine di nome, poi di id. Il nome si salva senza spazi all'inizio e alla fine; un nome già usato da un altro mazzo dello stesso utente è `409 DECK_NAME_ALREADY_USED`, anche con due richieste contemporanee (il vincolo `uq_decks_user_id_name` viene tradotto nello stesso errore).
- `PUT /decks/{id}` toglie il ticket in coda classificata; `DELETE /decks/{id}` risponde `204`, e il ticket si cancella da solo (`ON DELETE CASCADE`). Un mazzo usato da un invito `PENDING` è `409 DECK_IN_USE`. Inviti e coda si leggono con SQL nativo in `DeckRepository` finché non esiste `battle/`.

### Contenuto del mazzo (API-27, #97)

- `POST` e `PUT` ricevono `cards` (obbligatoria, anche vuota a livello di formato): ogni carta compare una volta sola con `quantity` ≥ 1, altrimenti `400 VALIDATION_FAILED` (campo `cardIdsUnique` per una carta ripetuta, `cards[i].quantity` per una quantità sotto 1).
- `PUT` sostituisce tutto il contenuto con `Deck.replaceCards`, che aggiorna le righe esistenti, elimina quelle tolte (`orphanRemoval`) e aggiunge le nuove.
- Una carta che non esiste rende il mazzo non valido: `422 DECK_INVALID` con `violations` (codice `DECK_CARD_NOT_FOUND`, `cardId`), e niente viene salvato. `violations` è un campo in più del ProblemDetail, scritto da `GlobalExceptionHandler` tramite `BusinessException.getExtraProperties()`.
- `GET /decks` restituisce `id`, `name`, `cardCount` (somma delle copie); `GET /decks/{id}` e le risposte di `POST` e `PUT` anche `cards`, in ordine di id della carta, ognuna con tutti i dati del catalogo (`card`) e `quantity`.

### Validazione (API-28, #98)

- `deck/service/DeckValidator` (bean senza stato, nessun accesso al database) riceve `DeckRules` (composizione del rule set attivo) e le carte con rarità e originale già caricate, e restituisce **tutte** le violazioni in quest'ordine: carte totali, composizione per tipo, Hidden per sottotipo, copie (varianti sommate all'originale, limite `min(deck_limit, copy_limit)` dell'originale), bilanciamento per rarità (`max_per_deck`, Terreno e Oggetto contano tra le Rare), carte disattivate. Dentro ogni controllo, carte in ordine di id e rarità di `sorting_order`.
- Al salvataggio (`POST`, `PUT`) un mazzo con violazioni è `422 DECK_INVALID` con `violations` e non si salva; le carte inesistenti (`DECK_CARD_NOT_FOUND`) sono in testa alla lista.
- `valid` si ricalcola a ogni lettura: in `GET /decks` per ogni mazzo, in `GET /decks/{id}` insieme a `violations` (lista vuota se valido). Un mazzo salvato diventa non valido, ad esempio, se una sua carta viene disattivata o se il rule set attivo cambia.
- `POST /decks/validate` con `{ "cards": [...] }` risponde sempre `200 { valid, violations }` (stesse regole del salvataggio, niente salvato); una carta ripetuta è `400`.
- `GET /decks/rules`: composizione del rule set attivo (`deckSize`, `deckCreatures`, `deckSpells`, `deckHidden`, `deckItems`, `deckTerrains`, `maxHiddenSurvival`, `maxHiddenDamage`) e `rarities` con `deckLimit` e `maxPerDeck`, dalla più comune. Senza rule set attivo, `404 NO_ACTIVE_RULE_SET`.
- I test (`DeckValidatorTest`) coprono ogni regola nel caso valido e al limite; `support/TestDecks` crea contenuti validi per il rule set attivo con le carte già nella collezione.
