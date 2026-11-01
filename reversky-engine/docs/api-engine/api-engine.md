# Contratto REST dell'engine

L'unico cliente dell'engine è `reversky-api` (il frontend **non** lo chiama mai direttamente).
Se cambi questo contratto, va aggiornato anche l'`EngineClient` dell'API: avvisa il team.

Base URL in sviluppo: `http://localhost:8081`

## `POST /api/v1/simulations`

Simula una partita completa.

### Richiesta

```json
{
  "seed": 8812734650912,
  "factions": [
    { "id": 1, "name": "Backrooms" },
    { "id": 2, "name": "Monti Appalachi" },
    { "id": 3, "name": "SCP" },
    { "id": 4, "name": "Yokai" }
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
        "atk": 4,
        "def": 5,
        "subtype": null,
        "effectCode": null
      },
      {
        "cardId": 70,
        "name": "Velo degli Ancorati",
        "type": "SPELL",
        "rarity": "Uncommon",
        "rarityOrder": 2,
        "factionId": 3,
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
| `factions` | sì | dalla tabella `factions` |
| `factionBonuses` | sì (anche vuota) | dalla tabella `faction_bonus` |
| `challenger`, `opponent` | sì | sfidante e avversario |
| `deck` | sì | una voce **per ogni copia**, almeno una creatura |
| `type` | sì | `CREATURE`, `SPELL`, `HIDDEN`, `TERRAIN` (enum `CardType`) |
| `rarityOrder` | sì | `rarities.sorting_order`, serve agli effetti che confrontano le rarità |
| `atk`, `def` | solo creature | come nel catalogo (nulli per le altre carte) |
| `subtype` | Spell e Hidden | ricavato dai tag: `DAMAGE`, `SHIELD`, `BURN`, `SURVIVAL` |
| `effectCode` | no | ricavato dal tag `EFFECT_<CODICE>` (senza il prefisso) |

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
      "data": { "startingLifePoints": 30 }, "message": "Partita iniziata" }
  ]
}
```

| Campo | Valori |
|---|---|
| `result` | `CHALLENGER_WIN`, `OPPONENT_WIN`, `DRAW` (uguali all'enum `BattleResult` del database) |
| `endReason` | `LIFE_POINTS_ZERO`, `BOTH_LIFE_POINTS_ZERO`, `NO_CREATURES_LEFT`, `BOTH_DECKS_EMPTY`, `ROUND_LIMIT` |
| `log` | vedi [log-partita](../log-partita/log-partita.md) |

L'API salva `log` in `battles.rounds_log` e il resto in `battles.battle_stats`.

### Errori

| Status | Quando |
|---|---|
| `400` | JSON non valido, campo mancante, mazzo senza creature, statistiche negative, fazione sconosciuta, bonus di fazione non valido, `effectCode` sconosciuto |
| `500` | bug nel motore (non dovrebbe mai succedere: va segnalato con il seed per riprodurlo) |

Formato: `ProblemDetail`, come nell'API.

```json
{
  "title": "Input di simulazione non valido",
  "status": 400,
  "detail": "Effetto sconosciuto: 'HIDEBEHND' sulla carta 23",
  "code": "UNKNOWN_EFFECT"
}
```

## `GET /api/v1/effects`

Elenco dei codici effetto implementati. Il backoffice dell'API lo usa per validare i tag `EFFECT_*` delle carte.

```json
[
  { "code": "HIDEBEHIND", "appliesTo": "CREATURE" },
  { "code": "VEIL_OF_ANCHORED", "appliesTo": "SPELL" }
]
```

## `GET /api/v1/rules`

Valori attuali di `RuleConfig`. Utile per mostrare le regole aggiornate nel portale senza duplicarle.

```json
{
  "rulesVersion": "1.0",
  "startingLifePoints": 30,
  "maxRounds": 100
}
```

## Note

- Nessuna autenticazione in sviluppo: l'engine non deve essere esposto su internet, solo l'API può raggiungerlo. In produzione si aggiunge una chiave condivisa nell'header **[DA CONFERMARE]**.
- Ogni endpoint va documentato anche in Swagger (springdoc), come nell'API.
