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
- `catalog_cards.copy_limit` permette di abbassarlo per una carta specifica (es. Spell Burn: 1).

### Composizione (dal rule set attivo)

Questi numeri stanno nel **rule set attivo** (tabella `rule_sets`) e si modificano dal backoffice: nessun valore è nel codice o in `application.properties`.

| Regola | Valore |
|---|---|
| Carte totali (`deck_size`) | esattamente **40** |
| Creature (`deck_creatures`) | **26** |
| Spell + Hidden (`deck_specials`) | **13** |
| Terreni (`deck_terrains`) | **1** |
| Hidden con tag `SURVIVAL` (`max_hidden_survival`) | massimo 2 in totale |
| Hidden con tag `DAMAGE` (`max_hidden_damage`) | massimo 1 in totale |

### Bilanciamento (richiesto dal brief)

Serve un vincolo che impedisca di riempire il mazzo solo con le carte più forti. Sta in `rarities.max_per_deck` (backoffice; vuoto = nessun limite):

| Regola | Valore |
|---|---|
| Legendary totali | massimo 2 |
| Mythic totali | massimo 2 **[DA CONFERMARE]** |
| Mutant totali | massimo 1 **[DA CONFERMARE]** |
| Rare totali | massimo 5 **[DA CONFERMARE]** |

La distribuzione "circa 10 Common, 7 Uncommon…" del regolamento è solo indicativa: **non** si valida.

### Possesso e disponibilità

- Per ogni carta: `quantità nel mazzo ≤ quantità in collection_cards`.
- Le carte con `is_active = false` restano utilizzabili da chi le possiede **[DA CONFERMARE]**.

## Come si valida

`DeckValidator` è una classe a sé, senza accesso al database: riceve il contenuto del mazzo (con rarità, tipo e tag di ogni carta), i limiti e la collezione, e restituisce **tutti** gli errori trovati (non si ferma al primo). Così il frontend può mostrarli tutti insieme.

```java
public record DeckViolation(String code, String message, Integer cardId) { }

public List<DeckViolation> validate(Map<CatalogCard, Integer> deckCards, Map<Integer, Integer> ownedByCardId) {
	List<DeckViolation> violations = new ArrayList<>();
	checkTotalSize(deckCards, violations);
	checkCompositionByType(deckCards, violations);
	checkCopyLimits(deckCards, violations);
	checkBalanceLimits(deckCards, violations);
	checkOwnership(deckCards, ownedByCardId, violations);
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
| `DECK_NOT_OWNED` | You own 1 copy of 'Kappa', the deck contains 2 |

## Quando si valida

1. **Al salvataggio** (`POST` / `PUT`): se ci sono errori → `422`, il mazzo non si salva.
2. **Alla lettura**: il campo `valid` viene ricalcolato, perché nel frattempo l'utente potrebbe aver scambiato una carta.
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
- Nome: 1–255 caratteri, univoco per utente (vincolo `(user_id, name)`); duplicato → `409`.
- Gli inviti già chiusi (`ACCEPTED`, `REJECTED`, `EXPIRED`) non bloccano l'eliminazione: il loro `challenger_deck_id` diventa `NULL` (`ON DELETE SET NULL`). Le partite conservano la propria fotografia del mazzo.

## Test minimi

Un test per **ogni** regola, sia nel caso valido sia al limite:
- 40 carte → ok; 39 o 41 → errore;
- copie = `min(deck_limit, copy_limit)` → ok; una in più → errore;
- carta con `copy_limit` più basso della rarità → vale il `copy_limit`;
- 2 Legendary diverse → ok; 3 → errore;
- carta non posseduta → errore;
- mazzo con più errori → li restituisce tutti;
- eliminazione di un mazzo usato in un invito `PENDING` → `409`;
- eliminazione di un mazzo usato in un invito già chiuso → ok, `challenger_deck_id` diventa `NULL`;
- modifica o eliminazione di un mazzo in coda classificata → ticket rimosso.
