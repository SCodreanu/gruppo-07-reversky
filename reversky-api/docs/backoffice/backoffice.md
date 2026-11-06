# Backoffice

## Cosa fa

Area riservata agli **amministratori** (`ADMIN`) per gestire catalogo, pacchetti e utenti senza toccare il database a mano.

Tutti gli endpoint sono sotto `/api/v1/admin/**`. Ogni modifica viene registrata in `audit_logs`.

## Carte — `catalog_cards`

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/admin/cards` | tutte le carte, anche disattivate, con filtri |
| `POST` | `/admin/cards` | crea una carta (l'illustrazione è obbligatoria e arriva nella stessa richiesta) |
| `PUT` | `/admin/cards/{id}` | modifica una carta |
| `POST` | `/admin/cards/{id}/deactivate` | la toglie dai pacchetti (`is_active = false`) |
| `POST` | `/admin/cards/{id}/activate` | la rimette nei pacchetti |
| `PUT` | `/admin/cards/{id}/tags` | imposta i tag della carta |
| `POST` | `/admin/cards/{id}/image` | sostituisce l'illustrazione (`image_path`) |

Campi:

| Campo | Note |
|---|---|
| `name` | nome della carta |
| `type` | `CREATURE`, `SPELL`, `HIDDEN`, `TERRAIN` |
| `subtype` | obbligatorio per Spell e Hidden, vuoto per le altre carte (vedi sotto) |
| `effectCode` | effetto della carta: obbligatorio per Spell, Hidden e Terreni, facoltativo per le creature (vedi sotto) |
| `variantOfId` | carta originale, solo per una versione evento (vedi sotto) |
| `rarityId` | riferimento a `rarities` |
| `factionId` | riferimento a `factions` (obbligatorio per tutte le carte) |
| `secondaryFactionId` | seconda fazione facoltativa (es. Mutant), diversa da `factionId`: la carta prende vantaggi e svantaggi di entrambe |
| `atk`, `def` | obbligatori e ≥ 0 per le creature (DEF > 0); vuoti per le altre carte |
| `copyLimit` | ≥ 1, oppure vuoto se vale solo il limite della rarità; il limite reale nel mazzo è `min(deck_limit della rarità, copyLimit)` |
| `imagePath` | illustrazione |
| `effectDescription` | testo dell'effetto mostrato ai giocatori (facoltativo) |
| `flavorText` | testo di sapore della lore (facoltativo) |
| `tags` | etichette per i filtri dei pacchetti |

### Regole

1. **Le carte non si cancellano mai.** Gli utenti le possiedono e lo storico le usa: si **disattivano**. Una carta disattivata non esce più dai pacchetti ma resta nelle collezioni.
2. **Effetti.** L'effetto di una carta è `catalog_cards.effect_code` (al massimo uno). Il codice **deve esistere nell'engine**: gli effetti sono codice Java, il backoffice può solo assegnare un effetto già implementato. L'elenco dei codici validi si ottiene dall'engine (`GET /api/v1/effects`).
3. **Sottotipi.** Una Spell deve avere uno tra `DAMAGE`, `SHIELD`, `BURN`; una Hidden uno tra `SURVIVAL`, `DAMAGE`; le altre carte nessuno. Lo garantisce il vincolo `chk_catalog_cards_subtype_matches_type`.
4. **Rarità coerenti con i pacchetti.** Spell e Hidden devono essere Common o Uncommon, i Terreni Rare (vedi [pacchetti](../pacchetti/pacchetti.md)).
5. **Statistiche massime per rarità:** `rarities.max_atk` e `rarities.max_def`, separati (proposta al cliente: Common 10, Uncommon 12, Rare 14, Mythic 16, Legendary 20; Mutant da definire = nessun controllo).
6. Modificare una carta **non** cambia le partite già giocate (vedi la "fotografia" in [sfide](../sfide/sfide.md)).
7. **Nomi.** Il nome è univoco tra le carte originali; una variante può riutilizzare quello dell'originale.
8. **Varianti.** Una versione evento cambia solo la grafica: `variantOfId` deve indicare una carta originale (mai un'altra variante), e tipo, sottotipo, effetto, rarità, fazioni, statistiche e `copyLimit` si copiano dall'originale. Modificare l'originale aggiorna anche le sue varianti, nella stessa transazione.

## Dati di riferimento

| Metodo | URL | Tabella |
|---|---|---|
| `GET/POST/PUT` | `/admin/rarities` | `rarities` (`deck_limit` ≥ 1, `max_per_deck`, `max_atk`, `max_def`, `sorting_order` univoco, scelto alla creazione e poi non più modificabile) |
| `GET/POST/PUT` | `/admin/factions` | `factions` (`name`, `emblem_path`, `description` facoltativa), `faction_tags` |
| `GET/PUT` | `/admin/faction-bonuses` | `faction_bonuses` (`atkBonus` anche negativo per il malus, `defBonus` ≥ 0, mai una fazione contro se stessa) |
| `GET/POST/PUT` | `/admin/tags` | `tags` |

Modificare `faction_bonuses` cambia il bilanciamento di **tutte** le partite future: va fatto solo dopo aver verificato l'effetto con le simulazioni dell'engine.

`sorting_order` non si modifica perché arriva all'engine come `rarityOrder` e alcuni effetti lo confrontano con soglie scritte nel codice (es. SCP-3000: `rarityOrder ≥ 3`). Una nuova rarità può usare solo un numero libero.

## Regole di gioco — `rule_sets`

Regola del progetto: **tutto ciò che non è una costante del regolamento si modifica da qui**, non dal codice né da `application.properties`.
Il rule set contiene punti vita iniziali e limite di round (inviati all'engine a ogni partita), composizione del mazzo e crediti di benvenuto, vittoria e pareggio (questi ultimi due solo nelle partite classificate).
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
| `PUT` | `/admin/packs/{id}` | modifica nome, costo, limite di acquisto, immagine, slot, bonus e tag |
| `POST` | `/admin/packs/{id}/deactivate` | lo toglie dal negozio |
| `POST` | `/admin/packs/{id}/activate` | lo rimette nel negozio |

Controlli:
- `cost` ≥ 0, `name` univoco;
- `purchaseLimit` ≥ 1, oppure vuoto se non c'è limite; obbligatorio se `cost` è 0 (omaggi, vedi [pacchetti](../pacchetti/pacchetti.md));
- almeno una riga in `pack_rarity_rules`, quantità > 0;
- `probability_units` dei bonus tra 0 e 10.000;
- la rarità di un bonus deve essere più alta di tutte le rarità degli slot base del pacchetto, altrimenti `422`;
- un pacchetto attivo deve poter riempire tutti i suoi slot base, anche con il ripiego descritto in [pacchetti](../pacchetti/pacchetti.md): altrimenti creazione, modifica e attivazione rispondono `422`;
- i pacchetti già comprati e non aperti (`pack_inventory`) restano apribili anche se il pacchetto viene disattivato.

## Utenti

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/admin/users` | ricerca utenti |
| `POST` | `/admin/users/{id}/disable` | disattiva l'account (`is_enabled = false`), cancella i suoi refresh token e il suo ticket in `matchmaking_queue` |
| `POST` | `/admin/users/{id}/enable` | riattiva |
| `PUT` | `/admin/users/{id}/role` | cambia ruolo |
| `POST` | `/admin/users/{id}/credits` | correzione manuale dei crediti, con motivo obbligatorio: riga `ADMIN_ADJUSTMENT` in `credit_transactions`, motivo in `new_values` di `audit_logs` |
| `GET` | `/admin/stats` | numeri chiave: utenti, pacchetti aperti (da `pack_openings`), partite giocate (KPI per il marketing) |

Un admin non può disattivare se stesso né abbassarsi il ruolo.

## Audit — `audit_logs`

Ogni azione del backoffice salva una riga:

| Campo | Esempio |
|---|---|
| `admin_user_id` | 1 |
| `action` | `CARD_UPDATED`, `USER_DISABLED`, `CREDITS_ADJUSTED` |
| `entity_type` | `catalog_cards` |
| `entity_id` | `"51"` (stringa, così vale anche per gli UUID); per le chiavi composte le parti separate da `:`, es. `"2:3"` per `faction_bonuses` (origine:bersaglio) |
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
- `old_values` e `new_values` non contengono **mai** dati sensibili (`password_hash`, token), perché queste righe non si possono cancellare.
- Il motivo di una correzione dei crediti sta in `new_values`, es. `{ "credits": 350, "reason": "..." }`.

## Test minimi

- Utente `USER` su `/admin/**` → `403`.
- Carta con `effectCode` inesistente nell'engine → `422`.
- Modifica di una carta originale → le sue varianti ricevono gli stessi valori.
- `variantOfId` che indica una variante → `422`.
- Carta disattivata → non esce più dai pacchetti, ma resta in collezione.
- Attivazione di un pacchetto con uno slot base che nessuna carta può riempire → `422`.
- Bonus di rarità non più alta di tutti gli slot base → `422`.
- Disattivazione di un utente in coda classificata → ticket rimosso.
- Ogni azione crea una riga in `audit_logs` con valori prima/dopo.
- Modifica di un utente → nessun `password_hash` né token in `audit_logs`.
