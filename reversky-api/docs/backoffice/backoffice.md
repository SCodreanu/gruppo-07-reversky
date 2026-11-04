# Backoffice

## Cosa fa

Area riservata agli **amministratori** (`ADMIN`) per gestire catalogo, pacchetti e utenti senza toccare il database a mano.

Tutti gli endpoint sono sotto `/api/v1/admin/**`. Ogni modifica viene registrata in `audit_logs`.

## Carte — `catalog_cards`

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/admin/cards` | tutte le carte, anche disattivate, con filtri |
| `POST` | `/admin/cards` | crea una carta |
| `PUT` | `/admin/cards/{id}` | modifica una carta |
| `POST` | `/admin/cards/{id}/deactivate` | la toglie dai pacchetti (`is_active = false`) |
| `POST` | `/admin/cards/{id}/activate` | la rimette nei pacchetti |
| `PUT` | `/admin/cards/{id}/tags` | imposta i tag della carta |
| `POST` | `/admin/cards/{id}/image` | carica l'illustrazione (`image_path`) |

Campi:

| Campo | Note |
|---|---|
| `name` | nome della carta |
| `type` | `CREATURE`, `SPELL`, `HIDDEN`, `TERRAIN` |
| `rarityId` | riferimento a `rarities` |
| `factionId` | riferimento a `factions` (obbligatorio per tutte le carte) |
| `secondaryFactionId` | seconda fazione facoltativa (es. Mutant), diversa da `factionId`: la carta prende vantaggi e svantaggi di entrambe |
| `atk`, `def` | obbligatori e ≥ 0 per le creature (DEF > 0); vuoti per le altre carte |
| `copyLimit` | ≥ 1; il limite reale nel mazzo è `min(deck_limit della rarità, copyLimit)` |
| `imagePath` | illustrazione |
| `effectDescription` | testo dell'effetto mostrato ai giocatori (facoltativo) |
| `flavorText` | testo di sapore della lore (facoltativo) |
| `tags` | effetto, sottotipo, filtri (vedi sotto) |

### Regole

1. **Le carte non si cancellano mai.** Gli utenti le possiedono e lo storico le usa: si **disattivano**. Una carta disattivata non esce più dai pacchetti ma resta nelle collezioni.
2. **Effetti tramite tag.** L'effetto di una carta è un tag `EFFECT_<CODICE>` (al massimo uno). Il codice **deve esistere nell'engine**: gli effetti sono codice Java, il backoffice può solo assegnare un effetto già implementato. L'elenco dei codici validi si ottiene dall'engine (`GET /api/v1/effects`).
3. **Sottotipi tramite tag.** Una Spell deve avere uno tra `DAMAGE`, `SHIELD`, `BURN`; una Hidden uno tra `SURVIVAL`, `DAMAGE`.
4. **Rarità coerenti con i pacchetti.** Spell e Hidden devono essere Common o Uncommon, i Terreni Rare (vedi [pacchetti](../pacchetti/pacchetti.md)).
5. **Statistiche massime per rarità:** `rarities.max_atk` e `rarities.max_def`, separati (proposta al cliente: Common 10, Uncommon 12, Rare 14, Mythic 16, Legendary 20; Mutant da definire = nessun controllo).
6. Modificare una carta **non** cambia le partite già giocate (vedi la "fotografia" in [sfide](../sfide/sfide.md)).

## Dati di riferimento

| Metodo | URL | Tabella |
|---|---|---|
| `GET/POST/PUT` | `/admin/rarities` | `rarities` (`deck_limit` ≥ 1, `max_per_deck`, `max_atk`, `max_def`, `sorting_order` univoco) |
| `GET/POST/PUT` | `/admin/factions` | `factions`, `faction_tags` |
| `GET/PUT` | `/admin/faction-bonus` | `faction_bonus` (`atkBonus` anche negativo per il malus, `defBonus` ≥ 0, mai una fazione contro se stessa) |
| `GET/POST/PUT` | `/admin/tags` | `tags` |

Modificare `faction_bonus` cambia il bilanciamento di **tutte** le partite future: va fatto solo dopo aver verificato l'effetto con le simulazioni dell'engine.

## Regole di gioco — `rule_sets`

Regola del progetto: **tutto ciò che non è una costante del regolamento si modifica da qui**, non dal codice né da `application.properties`.
Il rule set contiene punti vita iniziali e limite di round (inviati all'engine a ogni partita), composizione del mazzo e crediti di benvenuto, vittoria e pareggio.
Bonus di fazione e limiti per rarità stanno nelle loro tabelle (sopra).

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/admin/rule-sets` | elenco delle versioni, con quella attiva |
| `POST` | `/admin/rule-sets` | crea una nuova versione (di solito copiando quella attiva) |
| `PUT` | `/admin/rule-sets/{id}` | modifica una versione **mai usata** da una partita (`409` altrimenti) |
| `POST` | `/admin/rule-sets/{id}/activate` | la rende attiva e disattiva la precedente, in una sola transazione |

Controlli: `version` univoca; valori ≥ 1 per punti vita e round; creature + speciali + terreni = carte totali; crediti ≥ 0.
Ogni modifica e ogni attivazione va in `audit_logs`.

Le partite già giocate non cambiano: ognuna salva il proprio `rule_set_id` e una copia delle regole e dei bonus di fazione usati.
Un mazzo valido con il vecchio rule set può diventare non valido con il nuovo: lo segnala il campo `valid` (vedi [mazzi](../mazzi/mazzi.md)).

## Pacchetti

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/admin/packs` | elenco pacchetti |
| `POST` | `/admin/packs` | crea un tipo di pacchetto (es. evento) |
| `PUT` | `/admin/packs/{id}` | modifica nome, costo, immagine, slot, bonus e tag |
| `POST` | `/admin/packs/{id}/deactivate` | lo toglie dal negozio |

Controlli:
- `cost` ≥ 0, `name` univoco;
- almeno una riga in `pack_rarity_rules`, quantità > 0;
- `probability_units` dei bonus tra 0 e 10.000;
- i pacchetti già comprati e non aperti (`pack_inventory`) restano apribili anche se il pacchetto viene disattivato.

## Utenti

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/admin/users` | ricerca utenti |
| `POST` | `/admin/users/{id}/disable` | disattiva l'account (`is_enabled = false`) e cancella i suoi refresh token |
| `POST` | `/admin/users/{id}/enable` | riattiva |
| `PUT` | `/admin/users/{id}/role` | cambia ruolo |
| `POST` | `/admin/users/{id}/credits` | correzione manuale dei crediti, con motivo obbligatorio |
| `GET` | `/admin/stats` | numeri chiave: utenti, pacchetti aperti, partite giocate (KPI per il marketing) |

Un admin non può disattivare se stesso né abbassarsi il ruolo.

## Audit — `audit_logs`

Ogni azione del backoffice salva una riga:

| Campo | Esempio |
|---|---|
| `admin_user_id` | 1 |
| `action` | `CARD_UPDATED`, `USER_DISABLED`, `CREDITS_ADJUSTED` |
| `entity_type` | `catalog_cards` |
| `entity_id` | `"51"` (stringa, così vale anche per gli UUID) |
| `old_values` / `new_values` | `jsonb` con i campi cambiati |

```json
{
  "action": "CARD_UPDATED",
  "entityType": "catalog_cards",
  "entityId": "51",
  "oldValues": { "atk": 12 },
  "newValues": { "atk": 11 }
}
```

- La riga di audit si scrive **nella stessa transazione** della modifica.
- `GET /admin/audit-logs` (paginato, filtri per admin, entità e periodo) per consultarlo.
- Le righe di audit non si modificano né si cancellano.

## Test minimi

- Utente `USER` su `/admin/**` → `403`.
- Carta con tag `EFFECT_*` inesistente nell'engine → `422`.
- Carta disattivata → non esce più dai pacchetti, ma resta in collezione.
- Ogni azione crea una riga in `audit_logs` con valori prima/dopo.
