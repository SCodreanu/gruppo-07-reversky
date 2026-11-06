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

**[DA CONFERMARE]** Tutti i valori di questa tabella e i `deck_limit` qui sopra: i limiti del mazzo sono ancora in discussione, perché il cliente vuole più carte.

| Regola | Valore |
|---|---|
| Carte totali (`deck_size`) | esattamente **40** |
| Creature (`deck_creatures`) | **26** |
| Spell + Hidden (`deck_specials`) | **13** |
| Terreni (`deck_terrains`) | **1** |
| Hidden con sottotipo `SURVIVAL` (`max_hidden_survival`) | massimo 2 in totale |
| Hidden con sottotipo `DAMAGE` (`max_hidden_damage`) | massimo 1 in totale |

### Bilanciamento (richiesto dal brief)

Serve un vincolo che impedisca di riempire il mazzo solo con le carte più forti. Sta in `rarities.max_per_deck` (backoffice; vuoto = nessun limite):

| Regola | Valore |
|---|---|
| Legendary totali | massimo 2 |
| Mythic totali | massimo 2 **[DA CONFERMARE]** |
| Mutant totali | massimo 1 **[DA CONFERMARE]** |
| Rare totali | massimo 5 **[DA CONFERMARE]** |

Il Terreno è una carta Rare e occupa uno dei posti del massimo delle Rare.

La distribuzione "circa 10 Common, 7 Uncommon…" del regolamento è solo indicativa: **non** si valida.

### Possesso e disponibilità

- Per ogni carta: `quantità nel mazzo ≤ copie disponibili`, cioè le copie in `collection_cards` meno quelle riservate da uno scambio aperto (vedi [scambi](../scambi/scambi.md#copie-riservate)).
- Le carte con `is_active = false` restano utilizzabili da chi le possiede **[DA CONFERMARE]**.

## Come si valida

`DeckValidator` è una classe a sé, senza accesso al database: riceve il contenuto del mazzo (con rarità, tipo e sottotipo di ogni carta), i limiti e la collezione, e restituisce **tutti** gli errori trovati (non si ferma al primo). Così il frontend può mostrarli tutti insieme.

```java
public record DeckViolation(String code, String message, Integer cardId) { }

public List<DeckViolation> validate(Map<CatalogCard, Integer> deckCards, Map<Integer, Integer> availableByCardId) {
	List<DeckViolation> violations = new ArrayList<>();
	checkTotalSize(deckCards, violations);
	checkCompositionByType(deckCards, violations);
	checkCopyLimits(deckCards, violations);
	checkBalanceLimits(deckCards, violations);
	checkOwnership(deckCards, availableByCardId, violations);
	return violations;
}
```

Codici di errore (usati dal frontend):

| Codice | Esempio di messaggio |
|---|---|
| `DECK_SIZE` | The deck has 38 cards, 40 are required |
| `DECK_CREATURE_COUNT` | 26 creatures are required, there are 24 |
| `DECK_SPECIAL_COUNT` | 13 Spell/Hidden cards are required, there are 14 |
| `DECK_TERRAIN_COUNT` | Exactly 1 Terrain is required |
| `DECK_COPY_LIMIT` | Too many copies of 'Hounds' (2/1) |
| `DECK_BALANCE_LIMIT` | Too many Legendary cards (3/2) |
| `DECK_NOT_OWNED` | You have 1 available copy of 'Kappa', the deck contains 2 |

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
- il Terreno conta nel massimo delle Rare;
- carta non posseduta → errore;
- 3 copie, 2 riservate da uno scambio: 1 nel mazzo → ok; 2 → errore; chiuso lo scambio, 2 → ok;
- mazzo con più errori → li restituisce tutti;
- eliminazione di un mazzo usato in un invito `PENDING` → `409`;
- eliminazione di un mazzo usato in un invito già accettato → ok, `challenger_deck_id` diventa `NULL`;
- modifica o eliminazione di un mazzo in coda classificata → ticket rimosso;
- eliminazione di un mazzo → le sue righe di `deck_cards` vengono eliminate;
- modifica di un mazzo che conserva alcune carte → salvataggio riuscito (si aggiornano le righe esistenti: svuotando e reinserendo, Hibernate esegue gli insert prima dei delete e viola la chiave primaria).
