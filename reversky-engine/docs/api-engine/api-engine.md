# Contratto REST dell'engine

L'unico cliente dell'engine è `reversky-api` (il frontend **non** lo chiama mai direttamente).
Se cambi questo contratto, va aggiornato anche l'`EngineClient` dell'API: avvisa il team.

Base URL in sviluppo: `http://localhost:8081`

Richieste pronte per ogni caso, con il risultato atteso: [`examples/`](../../examples/README.md). Come lanciarle: [guida allo sviluppo](../guida-sviluppo/guida-sviluppo.md#3-chiamare-lengine).

## `POST /api/v1/simulations`

Simula una partita completa.

### Richiesta

```json
{
  "seed": 8812734650912,
  "rules": { "rulesVersion": "1.0", "startingLifePoints": 30, "maxRounds": 75 },
  "factions": [
    { "id": 1, "name": "Theory of the Wound" },
    { "id": 2, "name": "Theory of the Invitation" },
    { "id": 3, "name": "Theory of the Archive" },
    { "id": 4, "name": "Theory of the Dream" }
  ],
  "factionBonuses": [
    { "originFactionId": 2, "targetFactionId": 3, "atkBonus": 2, "defBonus": 0 },
    { "originFactionId": 3, "targetFactionId": 1, "atkBonus": 2, "defBonus": 0 },
    { "originFactionId": 1, "targetFactionId": 4, "atkBonus": 2, "defBonus": 0 },
    { "originFactionId": 4, "targetFactionId": 2, "atkBonus": 2, "defBonus": 0 }
  ],
  "challenger": {
    "userId": 7,
    "deck": [
      {
        "cardId": 4,
        "name": "Crawlers",
        "type": "CREATURE",
        "rarity": "Common",
        "rarityOrder": 1,
        "factionId": 1,
        "secondaryFactionId": null,
        "atk": 4,
        "def": 5,
        "subtype": null,
        "effectCode": null
      },
      {
        "cardId": 70,
        "name": "Veil of the Anchored",
        "type": "SPELL",
        "rarity": "Uncommon",
        "rarityOrder": 2,
        "factionId": 3,
        "secondaryFactionId": null,
        "atk": null,
        "def": null,
        "subtype": "SHIELD",
        "effectCode": "VEIL_OF_ANCHORED"
      }
    ]
  },
  "opponent": { "userId": 12, "deck": [ ] }
}
```

| Campo | Obbligatorio | Note |
|---|---|---|
| `seed` | sì | `long`, generato dall'API |
| `rules` | sì | regole della partita, dal **rule set attivo** nel database dell'API (backoffice, tabella `rule_sets`). L'engine **non ha valori predefiniti** |
| `rules.rulesVersion` | sì | `rule_sets.version`, restituito uguale nel risultato |
| `rules.startingLifePoints` | sì | ≥ 1 |
| `rules.maxRounds` | sì | ≥ 1, limite di round **totali** della partita |
| `factions` | sì | dalla tabella `factions` |
| `factionBonuses` | sì (anche vuota) | dalla tabella `faction_bonuses` |
| `challenger`, `opponent` | sì | sfidante e avversario |
| `deck` | sì | una voce **per ogni copia**, almeno una creatura |
| `type` | sì | `CREATURE`, `SPELL`, `HIDDEN`, `TERRAIN`, `ITEM` (enum `CardType`) |
| `factionId` | Creature, Terreni e Oggetti | fazione della carta; `null` per Spell e Hidden, che sono condivise |
| `rarityOrder` | sì | `rarities.sorting_order`, serve agli effetti che confrontano le rarità |
| `secondaryFactionId` | no | seconda fazione della carta (`catalog_cards.secondary_faction_id`), diversa da `factionId`; `null` se ne ha una sola. Vedi [fazioni](../fazioni/fazioni.md#carte-con-due-fazioni) |
| `atk`, `def` | solo creature | come nel catalogo (nulli per le altre carte) |
| `subtype` | Spell e Hidden | `catalog_cards.subtype`: `DAMAGE`, `SHIELD`, `BURN`, `SURVIVAL` |
| `effectCode` | Spell, Hidden, Terreni e Oggetti | `catalog_cards.effect_code`; facoltativo per le creature |

### Risposta `200 OK`

```json
{
  "result": "CHALLENGER_WIN",
  "endReason": "LIFE_POINTS_ZERO",
  "totalRounds": 37,
  "challenger": { "userId": 7,  "finalLifePoints": 9, "creaturesDefeated": 14 },
  "opponent":   { "userId": 12, "finalLifePoints": 0, "creaturesDefeated": 11 },
  "seed": 8812734650912,
  "rulesVersion": "1.0",
  "log": [
    { "sequence": 1, "round": 0, "type": "MATCH_STARTED", "side": null,
      "data": { "startingLifePoints": 30 }, "message": "Match started" }
  ]
}
```

| Campo | Valori |
|---|---|
| `result` | `CHALLENGER_WIN`, `OPPONENT_WIN`, `DRAW` (uguali all'enum `BattleResult` del database) |
| `endReason` | `LIFE_POINTS_ZERO`, `BOTH_LIFE_POINTS_ZERO`, `NO_CREATURES_LEFT`, `BOTH_DECKS_EMPTY`, `ROUND_LIMIT` |
| `totalRounds` | round risolti; la preparazione (round 0) non conta (D-42) |
| `finalLifePoints` | valore vero, **può essere negativo** se l'ultimo danno porta sotto zero (D-43) |
| `creaturesDefeated` | creature **avversarie** morte mentre affrontavano quel giocatore, per qualunque causa (attacco, effetto, Spell, Hidden); in una morte simultanea contano per entrambi (D-42) |
| `log` | vedi [log-partita](../log-partita/log-partita.md) |

L'API salva `log` in `battles.rounds_log` e il resto in `battles.battle_stats`.

### Errori

| Status | Quando |
|---|---|
| `400` | JSON non valido, campo mancante, mazzo senza creature, statistiche negative, fazione sconosciuta, bonus di fazione non valido, `effectCode` mancante o sconosciuto |
| `500` | bug nel motore (non dovrebbe mai succedere: va segnalato con il seed per riprodurlo) |

Formato: `ProblemDetail`, come nell'API.

I controlli si fanno in quest'ordine e si restituisce **il primo** che fallisce, così lo stesso input dà sempre lo stesso errore:

| Ordine | `code` | Quando | Esempio |
|---|---|---|---|
| 1 | `VALIDATION_FAILED` | JSON non valido, campo obbligatorio mancante, `rules` mancante o con valori < 1, mazzo vuoto (Bean Validation nello strato `web`) | `95` |
| 2 | `UNKNOWN_FACTION` | un `factionId` o `secondaryFactionId` di una carta, o una fazione di un bonus, non è tra le `factions` | `92` |
| 3 | `INVALID_FACTION_BONUS` | fazione contro se stessa o `defBonus` negativo (`atkBonus` negativo è ammesso: è il malus) | `93` |
| 4 | `INVALID_CARD_FACTIONS` | `secondaryFactionId` uguale a `factionId`; Creatura, Terreno od Oggetto senza `factionId`; Spell o Hidden con una fazione | `94` |
| 5 | `NO_CREATURES_IN_DECK` | un mazzo senza creature | `91` |
| 6 | `INVALID_STATS` | creatura senza `atk`/`def`, con ATK < 0 o DEF ≤ 0 | — |
| 7 | `MISSING_SUBTYPE` | Spell o Hidden senza `subtype` | — |
| 8 | `MISSING_EFFECT` | Spell, Hidden, Terreno od Oggetto senza `effectCode` | — |
| 9 | `UNKNOWN_EFFECT` | `effectCode` che non esiste nell'`EffectRegistry` | `90` |

```json
{
  "title": "Input di simulazione non valido",
  "status": 400,
  "detail": "Effetto sconosciuto: 'HIDEBEHND' sulla carta 23",
  "code": "UNKNOWN_EFFECT"
}
```

## `GET /api/v1/effects`

Elenco dei codici effetto implementati. Il backoffice dell'API lo usa per validare `effect_code` delle carte.

```json
[
  { "code": "HIDEBEHIND", "appliesTo": "CREATURE" },
  { "code": "VEIL_OF_ANCHORED", "appliesTo": "SPELL" }
]
```

## Regole della partita: niente `GET /api/v1/rules`

L'engine non ha regole proprie: tutto ciò che non è una costante del regolamento (punti vita, limite di round, bonus di fazione)
arriva nella richiesta. Per mostrare le regole nel portale si usa l'API (`GET /api/v1/rules`, dal rule set attivo).

## Note

- Nessuna autenticazione in sviluppo: l'engine non deve essere esposto su internet, solo l'API può raggiungerlo. Se l'engine viene esposto fuori dalla rete privata si aggiunge una chiave condivisa nell'header `X-Engine-Key`, letta da una variabile d'ambiente (D-40).
- Ogni endpoint va documentato anche in Swagger (springdoc), come nell'API.
