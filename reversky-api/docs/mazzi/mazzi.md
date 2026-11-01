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

### Composizione (da configurazione)

Questi numeri non sono nello schema: stanno in `application.properties` (`reversky.deck.*`).

| Regola | Valore |
|---|---|
| Carte totali | esattamente **40** |
| Creature | **26** |
| Spell + Hidden | **13** |
| Terreni | **1** |
| Hidden con tag `SURVIVAL` | massimo 2 in totale |
| Hidden con tag `DAMAGE` | massimo 1 in totale |

### Bilanciamento (richiesto dal brief)

Serve un vincolo che impedisca di riempire il mazzo solo con le carte più forti. Anche questi in configurazione:

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
| `DECK_SIZE` | Il mazzo ha 38 carte, ne servono 40 |
| `DECK_CREATURE_COUNT` | Servono 26 creature, ce ne sono 24 |
| `DECK_SPECIAL_COUNT` | Servono 13 Spell/Hidden, ce ne sono 14 |
| `DECK_TERRAIN_COUNT` | Serve esattamente 1 Terreno |
| `DECK_COPY_LIMIT` | Troppe copie di 'Hounds' (2/1) |
| `DECK_BALANCE_LIMIT` | Troppe Legendary (3/2) |
| `DECK_NOT_OWNED` | Possiedi 1 copia di 'Kappa', nel mazzo ce ne sono 2 |

## Quando si valida

1. **Al salvataggio** (`POST` / `PUT`): se ci sono errori → `422`, il mazzo non si salva.
2. **Alla lettura**: il campo `valid` viene ricalcolato, perché nel frattempo l'utente potrebbe aver scambiato una carta.
3. **Prima di una sfida**: alla creazione dell'invito e all'accettazione. Un mazzo non valido non può giocare.

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/decks` | i miei mazzi (con `valid` e numero carte) |
| `POST` | `/api/v1/decks` | crea |
| `GET` | `/api/v1/decks/{id}` | dettaglio con carte |
| `PUT` | `/api/v1/decks/{id}` | sostituisce nome e contenuto |
| `DELETE` | `/api/v1/decks/{id}` | elimina (non se usato in un invito `PENDING` → `409`) |
| `POST` | `/api/v1/decks/validate` | valida senza salvare (per il deck builder) |
| `GET` | `/api/v1/decks/rules` | limiti attuali (configurazione + `deck_limit` delle rarità), così il frontend non li duplica |

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

## Test minimi

Un test per **ogni** regola, sia nel caso valido sia al limite:
- 40 carte → ok; 39 o 41 → errore;
- copie = `min(deck_limit, copy_limit)` → ok; una in più → errore;
- carta con `copy_limit` più basso della rarità → vale il `copy_limit`;
- 2 Legendary diverse → ok; 3 → errore;
- carta non posseduta → errore;
- mazzo con più errori → li restituisce tutti.
