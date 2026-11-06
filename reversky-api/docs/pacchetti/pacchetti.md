# Pacchetti e rarità

## Cosa fa

L'utente **compra** pacchetti con i crediti e li tiene nell'inventario; quando vuole li **apre**. Ogni pacchetto ha un numero fisso di carte per rarità, con contenuto casuale e possibilità di carte bonus più rare.

Tabelle: `packs`, `pack_inventory`, `pack_openings`, `pack_rarity_rules`, `pack_bonus_rules`, `pack_tags`, `rarities`.

## Rarità

Righe della tabella `rarities`, ordinate da `sorting_order`:
`Common` → `Uncommon` → `Rare` → `Mythic` → `Legendary` → `Mutant`.

## Composizione di un pacchetto

### Slot base — `pack_rarity_rules`

Quante carte di ogni rarità contiene il pacchetto. Il totale delle carte è la **somma delle quantità**.

```
pack_rarity_rules (Pacchetto Base)
rarity    | quantity
----------+---------
Common    |    3
Uncommon  |    2
Rare      |    2          → 7 carte
```

### Carte bonus — `pack_bonus_rules`

Probabilità di ottenere una carta più rara, espressa in **unità su 10.000** (1.000 = 10%, 100 = 1%).

```
pack_bonus_rules (Pacchetto Base)
rarity    | probability_units | significato
----------+-------------------+------------------
Mythic    |       1000        | 10%  (~1 ogni 10)
Legendary |        500        | 5%   (~1 ogni 20)
Mutant    |        250        | 2,5% (~1 ogni 40)
```

- Ogni rarità bonus viene tirata **in modo indipendente** (in un pacchetto possono uscire sia una Mythic sia una Legendary).
- Ogni bonus riuscito **sostituisce** uno slot base: il numero di carte non cambia.
- I bonus sostituiscono gli slot partendo dalla rarità base più alta (le Rare), come previsto dal regolamento: un bonus riuscito = uno slot sostituito.
- I bonus riusciti si applicano partendo dal **più raro**. Se sono più degli slot, quelli in più si perdono: una Mutant non cede mai il posto a una Mythic.
- Un bonus di una rarità senza carte valide **non scatta**: lo slot resta della sua rarità base, così le probabilità delle altre rarità non cambiano.
- La rarità di un bonus deve essere più alta di tutte le rarità degli slot base del pacchetto (lo controlla il backoffice).

> Le frequenze sono **probabilità**, non garanzie: si può trovare una Mythic due volte di fila o non trovarla per 20 pacchetti.

### Filtro delle carte — `pack_tags`

- Se il pacchetto non ha righe in `pack_tags`: può uscire **qualsiasi** carta attiva, tranne le varianti.
- Altrimenti esce solo una carta che ha **almeno uno** dei tag del pacchetto, nei tag propri (`card_tags`) o in quelli di una delle sue fazioni (`faction_tags`, anche della seconda fazione).
- Una variante (`catalog_cards.variant_of_id`) esce solo se ha uno dei tag del pacchetto nei **tag propri**: non bastano quelli delle fazioni. Così una versione evento esce solo dai pacchetti del suo evento.

Esempio: un pacchetto evento con tag `DREAM` contiene solo carte della Theory of the Dream (anche come seconda fazione).

### Spell, Hidden e Terreni

Sono carte del catalogo come le altre, con una loro rarità: escono negli slot della loro rarità. Per rispettare il regolamento:
- Spell e Hidden hanno rarità **Common** o **Uncommon** → compaiono al posto di una Common/Uncommon;
- i Terreni hanno rarità **Rare** → compaiono solo al posto di una Rare, mai al posto di Mythic o Legendary.

## Acquisto

`POST /api/v1/packs/{id}/purchase` con `{ "quantity": 1 }` (1–10)

Nella stessa transazione:
1. il pacchetto deve essere `is_active`;
2. blocca la riga dell'inventario;
3. se il pacchetto ha un `purchase_limit`, già comprati + `quantity` non deve superarlo, altrimenti `422`;
4. `InventoryService.spendCredits(userId, cost × quantity, PACK_PURCHASE)`;
5. `pack_inventory.quantity += quantity` (crea la riga se non c'è).

### Limite di acquisto

`packs.purchase_limit` è il massimo di pacchetti che un utente può comprare in totale; se è nullo non c'è limite. I pacchetti già comprati si contano senza colonne in più:

```
già comprati = righe in pack_openings (utente, pacchetto) + pack_inventory.quantity
```

Aprire un pacchetto non cambia il conteggio, perché passa da `pack_inventory` a `pack_openings` nella stessa transazione.

### Pacchetti gratuiti

Un pacchetto con `cost = 0` è un omaggio: l'utente lo ritira dal negozio come un normale acquisto. Il `purchase_limit` è obbligatorio (`chk_packs_free_requires_limit`), altrimenti si potrebbero ritirare carte senza fine.

Per regalare una carta precisa basta un pacchetto con un solo slot e un tag che ha soltanto quella carta.

## Apertura

`POST /api/v1/inventory/packs/{packId}/open` — tutto in **una transazione**:

1. Blocca l'inventario; serve una riga in `pack_inventory`, altrimenti `422`. Poi `quantity − 1`; a 0 la riga si elimina.
2. Crea gli slot base da `pack_rarity_rules`.
3. Tira i bonus da `pack_bonus_rules`, saltando le rarità senza carte valide; ogni bonus riuscito, dal più raro, sostituisce uno slot partendo dalle Rare.
4. Per ogni slot sceglie a caso una carta **attiva**, della rarità dello slot, che passa il filtro dei tag (se non ce n'è, vedi "Rarità senza carte").
5. Aggiunge le carte alla collezione (`CollectionService.addCards`).
6. Salva l'apertura in `pack_openings`, con le carte ricevute.
7. Restituisce le carte nell'ordine degli slot (il frontend ci fa l'animazione).

```java
private boolean rollBonus(PackBonusRule rule) {
	// 0 ≤ roll < 10.000: con 1.000 unità il bonus esce nel 10% dei casi
	return random.nextInt(10_000) < rule.getProbabilityUnits();
}
```

### Rarità senza carte

Se per la rarità di uno slot base non c'è nessuna carta valida (es. un pacchetto evento senza carte Common con il suo tag), lo slot ripiega su un'altra rarità, secondo `sorting_order`:

1. scende alla rarità inferiore più vicina che ha una carta valida;
2. se sotto non c'è niente (es. uno slot Common in un pacchetto evento che ha solo carte Rare), sale alla rarità superiore più vicina.

Il ripiego non sceglie mai un Terreno: i Terreni escono solo dagli slot Rare. Il numero di carte del pacchetto non cambia.

Il backoffice rifiuta un pacchetto attivo se uno dei suoi slot base non troverebbe nessuna carta, neanche con il ripiego (vedi [backoffice](../backoffice/backoffice.md)). I bonus non ripiegano mai: se la loro rarità non ha carte, non scattano.

**Unica eccezione:** se le carte valide vengono disattivate dopo e uno slot resta senza carte, l'apertura risponde `422`. La transazione viene annullata, quindi il pacchetto resta nell'inventario e l'utente non perde niente.

## Casualità

- Si usa un bean `RandomGenerator` iniettato nel service (in produzione basato su `SecureRandom`).
- Nei test si inietta un generatore con seed fisso, così il risultato è prevedibile.
- **Il client non decide niente**: non invia seed, rarità o carte.

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/packs` | pacchetti acquistabili, con costo, limite di acquisto, quanti ne può ancora comprare l'utente, contenuto e probabilità (trasparenza verso l'utente); non elenca i bonus di rarità senza carte valide, perché non possono uscire |
| `POST` | `/api/v1/packs/{id}/purchase` | compra uno o più pacchetti |
| `GET` | `/api/v1/inventory` | crediti e pacchetti da aprire |
| `POST` | `/api/v1/inventory/packs/{packId}/open` | apre un pacchetto |

Risposta dell'apertura:

```json
{
  "packId": 1,
  "remainingPacks": 2,
  "cards": [
    { "id": 4,  "name": "Crawlers", "rarity": "Common", "isNew": true },
    { "id": 51, "name": "Mothman",  "rarity": "Mythic", "isNew": true }
  ]
}
```

`isNew` indica se l'utente non possedeva ancora quella carta.

## Test minimi

- Acquisto con crediti insufficienti: eccezione, saldo e pacchetti invariati.
- Acquisto oltre `purchase_limit` → `422`, saldo e pacchetti invariati; il conteggio include i pacchetti già aperti.
- Acquisto di un pacchetto con `cost = 0` → riga `PACK_PURCHASE` con `amount` 0, saldo invariato.
- Apertura senza pacchetti nell'inventario → `422`, niente cambia.
- Apertura dell'ultimo pacchetto → la riga di `pack_inventory` viene eliminata.
- Il numero di carte è sempre la somma degli slot base, anche con i bonus.
- Ogni apertura crea una riga in `pack_openings` con le stesse carte aggiunte alla collezione.
- Con 100.000 aperture simulate le frequenze dei bonus si avvicinano a quelle configurate (test statistico con tolleranza).
- Un pacchetto con `pack_tags` restituisce solo carte con quei tag (propri o della fazione).
- Una variante esce solo da un pacchetto con uno dei suoi tag propri, mai da uno senza `pack_tags`.
- Un Terreno non compare mai in uno slot Common/Uncommon.
- Un bonus di una rarità senza carte valide non scatta: lo slot resta della sua rarità base e `GET /packs` non elenca quel bonus.
- Con più bonus riusciti che slot vincono i più rari, e il numero di carte non cambia.
- Uno slot base senza carte della sua rarità scende alla rarità inferiore; se sotto non c'è niente, sale. Il numero di carte non cambia e il ripiego non sceglie mai un Terreno.
- Uno slot che non trova nessuna carta → `422`: pacchetto ancora nell'inventario, collezione invariata, nessuna riga in `pack_openings`.
