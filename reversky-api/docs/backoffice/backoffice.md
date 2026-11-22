# Backoffice

## Cosa fa

Area riservata agli **amministratori** (`ADMIN`) per gestire catalogo, pacchetti e utenti senza toccare il database a mano.

Tutti gli endpoint sono sotto `/api/v1/admin/**`. Ogni modifica viene registrata in `audit_logs`.

- Il controllo del ruolo è in `SecurityConfig` (`/api/v1/admin/**` → `hasRole(ADMIN)`): i controller non ripetono `@PreAuthorize`.
- I controller stanno in `admin/controller/` e usano i service delle funzionalità (vedi [architettura](../architettura/architettura.md)).
- Già implementato, senza audit (task API-52, #122):
  - `GET/POST/PUT /admin/factions` (`AdminFactionController`), senza `faction_tags` (task API-13, #83);
  - `GET/POST/PUT /admin/rarities` (`AdminRarityController`): `POST` usa `RarityCreateRequest`, `PUT` usa `RarityUpdateRequest`, che non ha `sortingOrder` (se il client lo invia viene ignorato);
  - carte (task API-08, #78): `GET /admin/cards`, `PUT /admin/cards/{id}`, `POST /admin/cards/{id}/deactivate` e `/activate` (`AdminCardController`, `CatalogCardService`), con la rarità (task API-10, #80); le fazioni sono nell'entità ma `PUT` non le riceve ancora (task #81), e i tag mancano (task #82). `POST /admin/cards` e `POST /admin/cards/{id}/image` arrivano con API-14 (#84): fino ad allora le carte si inseriscono da migrazione. Dettagli in "Comportamento già implementato" qui sotto.

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
| `type` | `CREATURE`, `SPELL`, `HIDDEN`, `TERRAIN`, `ITEM` (Oggetto) |
| `subtype` | obbligatorio per Spell e Hidden, vuoto per le altre carte (vedi sotto) |
| `effectCode` | effetto della carta: obbligatorio per Spell, Hidden, Terreni e Oggetti, facoltativo per le creature (vedi sotto) |
| `variantOfId` | carta originale, solo per una versione evento (vedi sotto) |
| `rarityId` | riferimento a `rarities` |
| `factionId` | riferimento a `factions`: obbligatorio per Creature, Terreni e Oggetti, **vuoto** per Spell e Hidden, che sono condivise tra tutte le fazioni |
| `secondaryFactionId` | seconda fazione facoltativa (es. Mutant), diversa da `factionId`: la carta prende vantaggi e svantaggi di entrambe. Sempre vuota per Spell e Hidden |
| `atk`, `def` | obbligatori e ≥ 0 per le creature (DEF > 0); vuoti per le altre carte |
| `copyLimit` | ≥ 1, oppure vuoto se vale solo il limite della rarità; il limite reale nel mazzo è `min(deck_limit della rarità, copyLimit)` |
| `imagePath` | illustrazione |
| `effectDescription` | testo dell'effetto mostrato ai giocatori (facoltativo) |
| `flavorText` | testo di sapore della lore (facoltativo) |
| `tags` | etichette per i filtri dei pacchetti |

### Regole

1. **Le carte non si cancellano mai.** Gli utenti le possiedono e lo storico le usa: si **disattivano**. Una carta disattivata non esce più dai pacchetti e non si può usare nei mazzi (Q-07), ma resta nelle collezioni.
2. **Effetti.** L'effetto di una carta è `catalog_cards.effect_code` (al massimo uno). Il codice **deve esistere nell'engine**: gli effetti sono codice Java, il backoffice può solo assegnare un effetto già implementato. L'elenco dei codici validi si ottiene dall'engine (`GET /api/v1/effects`).
3. **Sottotipi.** Una Spell deve avere uno tra `DAMAGE`, `SHIELD`, `BURN`; una Hidden uno tra `SURVIVAL`, `DAMAGE`; le altre carte nessuno. Lo garantisce il vincolo `chk_catalog_cards_subtype_matches_type`.
4. **Rarità coerenti con i pacchetti.** Spell e Hidden devono essere Common o Uncommon, Terreni e Oggetti Rare (vedi [pacchetti](../pacchetti/pacchetti.md)); le creature accettano qualsiasi rarità. Le rarità si riconoscono dal `sorting_order` e **mai dal nome** (il nome si modifica dal backoffice, l'ordine no; anche l'engine le confronta solo con `rarityOrder`): Common = 1, Uncommon = 2, Rare = 3 (`Rarity.COMMON_ORDER`, `UNCOMMON_ORDER`, `RARE_ORDER`). Il seed delle rarità (#85) deve usare questi valori. La regola non ha un vincolo nel database: la controlla solo il service.
5. **Nessun massimo di ATK e DEF per rarità.** Le carte personalizzate possono superare qualsiasi soglia: la rarità non limita le statistiche, valgono solo ATK ≥ 0 e DEF ≥ 1 per le creature (decisione del 9 novembre, che sostituisce `max_atk` e `max_def`).
6. Modificare una carta **non** cambia le partite già giocate (vedi la "fotografia" in [sfide](../sfide/sfide.md)).
7. **Nomi.** Il nome è univoco tra le carte originali; una variante può riutilizzare quello della **propria** originale, ma non quello di un'altra originale.
8. **Varianti.** Una versione evento cambia solo la grafica: `variantOfId` deve indicare una carta originale (mai un'altra variante), e tipo, sottotipo, effetto (`effectCode` e `effectDescription`), rarità, fazioni, statistiche e `copyLimit` si copiano dall'originale. Nome, illustrazione e `flavorText` restano quelli della variante. Modificare l'originale aggiorna anche le sue varianti, nella stessa transazione.

### Comportamento già implementato (task API-08, #78)

Endpoint pubblici, senza token (`CardController`):

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/cards` | carte **attive**, varianti comprese, paginate; filtri facoltativi `type`, `name` (contiene, senza maiuscole) e `rarityId` (un id inesistente dà una pagina vuota, non `404`) |
| `GET` | `/cards/{id}` | una carta, **anche disattivata** (collezioni e storico la usano ancora); `404` se non esiste |

`GET /admin/cards` ha in più il filtro `active` (`true`, `false`, vuoto = tutte). Ogni carta restituita ha `rarity: { id, name }`, la stessa forma di [collezione](../collezione/collezione.md); per una variante è la rarità dell'originale. Le liste sono sempre ordinate per nome e poi per `id`: il parametro `sort` del client si ignora, quindi Swagger documenta solo `page` e `size`.

`PUT /admin/cards/{id}` (`CatalogCardUpdateRequest`):

- non ha `imagePath`: l'illustrazione si sostituisce con `POST /admin/cards/{id}/image` (API-14); se il client lo invia viene ignorato;
- `variantOfId` vuoto rende la carta originale, valorizzato la rende variante di quella carta;
- `rarityId` è obbligatorio (come `type`) anche per una variante, ma su una variante si ignora: non si controlla nemmeno che esista;
- su una variante i campi di gioco della richiesta, rarità compresa, si ignorano e si copiano dall'originale; modificare la rarità di un'originale la cambia anche alle sue varianti;
- tutti i controlli avvengono prima di modificare la carta, in questo ordine:

| Caso | Status | `code` |
|---|---|---|
| campi mancanti o fuori dai limiti del DTO (nome, tipo, `rarityId`, ATK < 0, DEF < 1, `copyLimit` < 1, `effectCode` vuoto) | `400` | `VALIDATION_FAILED` |
| carta inesistente | `404` | `CARD_NOT_FOUND` |
| `variantOfId` uguale alla carta stessa, inesistente, che indica una variante, oppure carta che ha già varianti | `422` | `CARD_VARIANT_INVALID` |
| solo per un'originale: `rarityId` inesistente | `422` | `CARD_RARITY_INVALID` |
| statistiche, sottotipo o effetto non coerenti con il tipo (regole 2 e 3 e la riga `atk`, `def` dei campi) | `422` | `CARD_RULES_VIOLATED` |
| rarità non ammessa per il tipo (regola 4) | `422` | `CARD_RARITY_INVALID` |
| il nuovo tipo richiede una fazione ma la carta non ne ha (riga `factionId` dei campi) | `422` | `CARD_RULES_VIOLATED` |
| nome già usato da un'altra carta originale (regola 7) | `409` | `CARD_NAME_TAKEN` |

Fazioni finché `PUT` non le riceve (task #81): la carta tiene quelle che ha. Una Spell o una Hidden non ne ha, quindi non può diventare Creatura, Terreno o Oggetto ("A CREATURE card must have a faction"); una carta che diventa Spell o Hidden perde le fazioni, e con lei le sue varianti.

`variantOfId` e `rarityId` inesistenti sono `422` e non `404`: la carta dell'URL esiste, è sbagliato un riferimento nel corpo. Le regole del tipo vengono prima di quelle della rarità: una Spell Rare con ATK riceve "must not have ATK or DEF", non un errore sulla rarità.

`POST /admin/cards/{id}/deactivate` e `/activate` rispondono `200` con la carta e sono idempotenti (disattivare una carta già disattivata non cambia niente). Non toccano le varianti: una versione evento si attiva e disattiva per conto suo.

Non ancora implementato: il controllo che `effectCode` esista nell'engine (regola 2), che richiede il client verso l'engine; per ora si controlla solo che non sia vuoto e che non superi 100 caratteri.

## Dati di riferimento

| Metodo | URL | Tabella |
|---|---|---|
| `GET/POST/PUT` | `/admin/rarities` | `rarities` (`deck_limit` ≥ 1, `max_per_deck`, `sorting_order` univoco, scelto alla creazione e poi non più modificabile) |
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

Controlli: `version` univoca; valori ≥ 1 per punti vita e round; creature + Spell + Hidden + Oggetti + Terreni = carte totali; crediti ≥ 0.
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
- Carta disattivata → non esce più dai pacchetti e rende non validi i mazzi che la contengono, ma resta in collezione.
- Attivazione di un pacchetto con uno slot base che nessuna carta può riempire → `422`.
- Bonus di rarità non più alta di tutti gli slot base → `422`.
- Disattivazione di un utente in coda classificata → ticket rimosso.
- Ogni azione crea una riga in `audit_logs` con valori prima/dopo.
- Modifica di un utente → nessun `password_hash` né token in `audit_logs`.
