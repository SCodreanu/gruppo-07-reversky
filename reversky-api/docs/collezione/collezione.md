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

## Le carte nei mazzi restano nella collezione

Mettere una carta in un mazzo **non** la toglie dalla collezione: il mazzo è solo un elenco che fa riferimento a carte possedute. Una carta può stare in più mazzi contemporaneamente.

Conseguenza: se l'utente scambia una carta usata in un mazzo, quel mazzo può diventare non valido (vedi [mazzi](../mazzi/mazzi.md)).

## Endpoint

`GET /api/v1/collection` — paginato, con filtri opzionali:

| Parametro | Esempio | Effetto |
|---|---|---|
| `factionId` | `3` | solo una fazione (anche come seconda fazione) |
| `rarityId` | `4` | solo una rarità |
| `type` | `CREATURE` | solo un tipo di carta |
| `tag` | `DREAM` | solo carte con quel tag |
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
    "tags": ["EFFECT_WENDIGO"],
    "effectDescription": "At the end of each round, the opposing creature loses 1 DEF...",
    "flavorText": "Hunger and frost that consume everything.",
    "imagePath": "/images/cards/wendigo.webp"
  },
  "quantity": 1,
  "usedInDecks": 2
}
```

Altri endpoint:
- `GET /api/v1/collection/summary` → carte uniche possedute sul totale del catalogo, per rarità (barra di completamento);
- `GET /api/v1/users/{id}/collection` → carte di un altro utente (solo carte e quantità), per preparare uno scambio.

## Test minimi

- `addCards` su carta già posseduta incrementa la quantità, su carta nuova crea la riga.
- `removeCards` con quantità insufficiente lancia eccezione e non modifica niente.
- `removeCards` che porta a 0 elimina la riga.
