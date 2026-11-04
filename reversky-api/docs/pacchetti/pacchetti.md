# Pacchetti e rarità

## Cosa fa

L'utente **compra** pacchetti con i crediti e li tiene nell'inventario; quando vuole li **apre**. Ogni pacchetto ha un numero fisso di carte per rarità, con contenuto casuale e possibilità di carte bonus più rare.

Tabelle: `packs`, `pack_inventory`, `pack_rarity_rules`, `pack_bonus_rules`, `pack_tags`, `rarities`.

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

> Le frequenze sono **probabilità**, non garanzie: si può trovare una Mythic due volte di fila o non trovarla per 20 pacchetti.

### Filtro delle carte — `pack_tags`

- Se il pacchetto non ha righe in `pack_tags`: può uscire **qualsiasi** carta attiva.
- Altrimenti esce solo una carta che ha **almeno uno** dei tag del pacchetto, nei tag propri (`card_tags`) o in quelli di una delle sue fazioni (`faction_tags`, anche della seconda fazione).

Esempio: un pacchetto evento con tag `DREAM` contiene solo carte della Theory of the Dream (anche come seconda fazione).

### Spell, Hidden e Terreni

Sono carte del catalogo come le altre, con una loro rarità: escono negli slot della loro rarità. Per rispettare il regolamento:
- Spell e Hidden hanno rarità **Common** o **Uncommon** → compaiono al posto di una Common/Uncommon;
- i Terreni hanno rarità **Rare** → compaiono solo al posto di una Rare, mai al posto di Mythic o Legendary.

## Acquisto

`POST /api/v1/packs/{id}/purchase` con `{ "quantity": 1 }` (1–10)

Nella stessa transazione:
1. il pacchetto deve essere `is_active`;
2. `InventoryService.spendCredits(userId, cost × quantity)`;
3. `pack_inventory.quantity += quantity` (crea la riga se non c'è).

## Apertura

`POST /api/v1/inventory/packs/{packId}/open` — tutto in **una transazione**:

1. Blocca l'inventario; serve `pack_inventory.quantity ≥ 1`, altrimenti `422`. Poi `quantity − 1`.
2. Crea gli slot base da `pack_rarity_rules`.
3. Tira i bonus da `pack_bonus_rules`; ogni bonus riuscito sostituisce uno slot, partendo dalle Rare.
4. Per ogni slot sceglie a caso una carta **attiva**, della rarità dello slot, che passa il filtro dei tag.
5. Aggiunge le carte alla collezione (`CollectionService.addCards`).
6. Restituisce le carte nell'ordine degli slot (il frontend ci fa l'animazione).

```java
private boolean rollBonus(PackBonusRule rule) {
	// 0 ≤ roll < 10.000: con 1.000 unità il bonus esce nel 10% dei casi
	return random.nextInt(10_000) < rule.getProbabilityUnits();
}
```

### Rarità senza carte

Se per una rarità non c'è nessuna carta valida (es. ancora nessuna `Mutant` nel catalogo), si scende alla rarità inferiore disponibile secondo `sorting_order`. Mai errori, mai pacchetti con meno carte.

## Casualità

- Si usa un bean `RandomGenerator` iniettato nel service (in produzione basato su `SecureRandom`).
- Nei test si inietta un generatore con seed fisso, così il risultato è prevedibile.
- **Il client non decide niente**: non invia seed, rarità o carte.

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/packs` | pacchetti acquistabili, con costo, contenuto e probabilità (trasparenza verso l'utente) |
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
- Apertura senza pacchetti nell'inventario → `422`, niente cambia.
- Il numero di carte è sempre la somma degli slot base, anche con i bonus.
- Con 100.000 aperture simulate le frequenze dei bonus si avvicinano a quelle configurate (test statistico con tolleranza).
- Un pacchetto con `pack_tags` restituisce solo carte con quei tag (propri o della fazione).
- Un Terreno non compare mai in uno slot Common/Uncommon.
