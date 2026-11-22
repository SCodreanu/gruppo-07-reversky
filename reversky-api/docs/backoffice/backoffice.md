# Backoffice

## Cosa fa

Area riservata agli **amministratori** (`ADMIN`) per gestire catalogo, pacchetti e utenti senza toccare il database a mano.

Tutti gli endpoint sono sotto `/api/v1/admin/**`. Ogni modifica viene registrata in `audit_logs`.

- Il controllo del ruolo è in `SecurityConfig` (`/api/v1/admin/**` → `hasRole(ADMIN)`): i controller non ripetono `@PreAuthorize`.
- I controller stanno in `admin/controller/` e usano i service delle funzionalità (vedi [architettura](../architettura/architettura.md)).
- Già implementato, senza audit (task API-52, #122):
  - `GET/POST/PUT /admin/factions` (`AdminFactionController`), senza `faction_tags` (task API-13, #83);
  - `GET/POST/PUT /admin/rarities` (`AdminRarityController`): `POST` usa `RarityCreateRequest`, `PUT` usa `RarityUpdateRequest`, che non ha `sortingOrder` (se il client lo invia viene ignorato);
  - carte (task API-08, #78): `GET /admin/cards`, `PUT /admin/cards/{id}`, `POST /admin/cards/{id}/deactivate` e `/activate` (`AdminCardController`, `CatalogCardService`), con la rarità (task API-10, #80) e le fazioni (task API-11, #81); i tag mancano (task #82). `POST /admin/cards` e `POST /admin/cards/{id}/image` con l'illustrazione (API-14, #84). Dettagli in "Comportamento già implementato" e "Illustrazioni" qui sotto.
  - rule set: `GET/POST /admin/rule-sets`, `PUT /admin/rule-sets/{id}`, `POST /admin/rule-sets/{id}/activate` (`AdminRuleSetController`, `ruleset/service/RuleSetService`), e `GET /rules` pubblico (`RuleSetController`). `created_by_id` resta nullo finché il backoffice non registra l'autore. Dettagli in "Regole di gioco" qui sotto.
  - bonus di fazione: `GET/PUT /admin/faction-bonuses` (`AdminFactionBonusController`, `catalog/service/FactionBonusService`), inclusi anche in `GET /rules`. Dettagli in "Bonus di fazione: comportamento già implementato" qui sotto.

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
| `secondaryFactionId` | seconda fazione facoltativa (es. Mutant), diversa da `factionId`: la carta prende vantaggi e svantaggi di entrambe. Solo per le creature: sempre vuota per Spell, Hidden, Terreni e Oggetti (D-22, API-71) |
| `atk`, `def` | obbligatori e ≥ 0 per le creature (DEF > 0); vuoti per le altre carte |
| `copyLimit` | ≥ 1, oppure vuoto se vale solo il limite della rarità; il limite reale nel mazzo è `min(deck_limit della rarità, copyLimit)` |
| `imagePath` | illustrazione |
| `effectDescription` | testo dell'effetto mostrato ai giocatori (facoltativo) |
| `flavorText` | testo di sapore della lore (facoltativo) |
| `tags` | etichette per i filtri dei pacchetti |

### Regole

1. **Le carte non si cancellano mai.** Gli utenti le possiedono e lo storico le usa: si **disattivano**. Una carta disattivata non esce più dai pacchetti e non si può usare nei mazzi (Q-07), ma resta nelle collezioni.
2. **Effetti.** L'effetto di una carta è `catalog_cards.effect_code` (al massimo uno). Il codice **deve esistere nell'engine**: gli effetti sono codice Java, il backoffice può solo assegnare un effetto già implementato. L'elenco dei codici validi sta **nell'API**, nella tabella `effects` (V13, API-36, deciso il 21 novembre), e il backoffice lo legge con `GET /admin/effects`: l'engine si chiama solo per simulare, mai per sapere quali effetti esistono. Ogni codice vale per un solo tipo di carta (`appliesTo`): un codice di Terreno su una Spell è rifiutato come un codice inesistente. Un effetto nuovo arriva con una migrazione, insieme al suo codice nell'engine.
3. **Sottotipi.** Una Spell deve avere uno tra `DAMAGE`, `SHIELD`, `BURN`; una Hidden uno tra `SURVIVAL`, `DAMAGE`; le altre carte nessuno. Lo garantisce il vincolo `chk_catalog_cards_subtype_matches_type`.
4. **Rarità coerenti con i pacchetti.** Spell, Hidden, Terreni e Oggetti devono essere Rare (Spell e Hidden Rare dal 21 novembre, con il catalogo di 120 carte; prima erano Common o Uncommon), vedi [pacchetti](../pacchetti/pacchetti.md); le creature accettano qualsiasi rarità. Spell e Hidden non contano nel `max_per_deck` della Rare (vedi [mazzi](../mazzi/mazzi.md#bilanciamento-richiesto-dal-brief)). Le rarità si riconoscono dal `sorting_order` e **mai dal nome** (il nome si modifica dal backoffice, l'ordine no; anche l'engine le confronta solo con `rarityOrder`): Common = 1, Uncommon = 2, Rare = 3 (`Rarity.COMMON_ORDER`, `UNCOMMON_ORDER`, `RARE_ORDER`). Il seed delle rarità (`V9`, #85) usa questi valori. La regola non ha un vincolo nel database: la controlla solo il service.
5. **Nessun massimo di ATK e DEF per rarità.** Le carte personalizzate possono superare qualsiasi soglia: la rarità non limita le statistiche, valgono solo ATK ≥ 0 e DEF ≥ 1 per le creature (decisione del 9 novembre, che sostituisce `max_atk` e `max_def`).
6. Modificare una carta **non** cambia le partite già giocate (vedi la "fotografia" in [sfide](../sfide/sfide.md)).
7. **Nomi.** Il nome è univoco tra le carte originali; una variante può riutilizzare quello della **propria** originale, ma non quello di un'altra originale.
8. **Varianti.** Una versione evento cambia solo la grafica: `variantOfId` deve indicare una carta originale (mai un'altra variante), e tipo, sottotipo, effetto (`effectCode` e `effectDescription`), rarità, fazioni, statistiche e `copyLimit` si copiano dall'originale. Nome, illustrazione e `flavorText` restano quelli della variante. Modificare l'originale aggiorna anche le sue varianti, nella stessa transazione.

### Comportamento già implementato (task API-08, #78)

Endpoint pubblici, senza token (`CardController`):

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/cards` | carte **attive**, varianti comprese, paginate; filtri facoltativi `type`, `name` (contiene, senza maiuscole), `rarityId` e `factionId` (prima o seconda fazione; non restituisce mai Spell né Hidden). Un id inesistente dà una pagina vuota, non `404` |
| `GET` | `/cards/{id}` | una carta, **anche disattivata** (collezioni e storico la usano ancora); `404` se non esiste |

`GET /admin/cards` ha in più il filtro `active` (`true`, `false`, vuoto = tutte). Ogni carta restituita ha `rarity: { id, name }`, `faction: { id, name }` e `secondaryFaction: { id, name }`, la stessa forma di [collezione](../collezione/collezione.md); `faction` è `null` per Spell e Hidden, `secondaryFaction` è `null` se la carta ha una sola fazione; per una variante sono quelle dell'originale. Le liste sono sempre ordinate per nome e poi per `id`: il parametro `sort` del client si ignora, quindi Swagger documenta solo `page` e `size`.

`PUT /admin/cards/{id}` (`CatalogCardUpdateRequest`):

- non ha `imagePath`: l'illustrazione si sostituisce con `POST /admin/cards/{id}/image` (API-14); se il client lo invia viene ignorato;
- `variantOfId` vuoto rende la carta originale, valorizzato la rende variante di quella carta;
- `rarityId` è obbligatorio (come `type`) anche per una variante, ma su una variante si ignora: non si controlla nemmeno che esista;
- su una variante i campi di gioco della richiesta, rarità compresa, si ignorano e si copiano dall'originale; modificare la rarità di un'originale la cambia anche alle sue varianti;
- `factionId` e `secondaryFactionId` (task #81, deciso il 20 novembre) sostituiscono le fazioni della carta, come il resto del `PUT`: una Creatura, un Terreno o un Oggetto inviati senza `factionId` sono rifiutati. Su una variante si ignorano, senza controllare che esistano, e si copiano dall'originale; le fazioni nuove di un'originale passano alle sue varianti;
- tutti i controlli avvengono prima di modificare la carta, in questo ordine:

| Caso | Status | `code` |
|---|---|---|
| campi mancanti o fuori dai limiti del DTO (nome, tipo, `rarityId`, ATK < 0, DEF < 1, `copyLimit` < 1, `effectCode` vuoto) | `400` | `VALIDATION_FAILED` |
| carta inesistente | `404` | `CARD_NOT_FOUND` |
| `variantOfId` uguale alla carta stessa, inesistente, che indica una variante, oppure carta che ha già varianti | `422` | `CARD_VARIANT_INVALID` |
| solo per un'originale: `rarityId` inesistente | `422` | `CARD_RARITY_INVALID` |
| solo per un'originale: `factionId` o `secondaryFactionId` inesistente | `422` | `CARD_FACTION_INVALID` |
| statistiche, sottotipo o effetto non coerenti con il tipo (regole 2 e 3 e la riga `atk`, `def` dei campi) | `422` | `CARD_RULES_VIOLATED` |
| fazioni non coerenti con il tipo (righe `factionId` e `secondaryFactionId` dei campi): Creatura, Terreno o Oggetto senza fazione, Spell o Hidden con una fazione, Terreno o Oggetto con una seconda fazione (API-71), seconda fazione uguale alla prima | `422` | `CARD_RULES_VIOLATED` |
| rarità non ammessa per il tipo (regola 4) | `422` | `CARD_RARITY_INVALID` |
| nome già usato da un'altra carta originale (regola 7) | `409` | `CARD_NAME_TAKEN` |
| solo per un'originale con `effectCode`: il codice non è in `effects`, o c'è per un altro tipo di carta (regola 2) | `422` | `CARD_EFFECT_INVALID` |

Messaggi delle fazioni: "A CREATURE card must have a faction", "A SPELL card must not have a faction", "A TERRAIN card must not have a second faction", "The second faction must be different from the first", "The faction 7 does not exist". Una carta che diventa Spell o Hidden perde le fazioni (la richiesta non può averne), e con lei le sue varianti. Solo le creature hanno due fazioni anche nel database (`chk_catalog_cards_secondary_faction_creature_only`, `V14`, API-71): l'engine rifiuta un Terreno o un Oggetto con due fazioni (`INVALID_CARD_FACTIONS`). Come ogni regola del catalogo controllata anche dall'engine, vedi la tabella in [sfide](../sfide/sfide.md#controlli-dellengine-e-dellapi-api-71-146).

`variantOfId`, `rarityId` e le fazioni inesistenti sono `422` e non `404`: la carta dell'URL esiste, è sbagliato un riferimento nel corpo. Le regole del tipo vengono prima di quelle della rarità: una Spell Rare con ATK riceve "must not have ATK or DEF", non un errore sulla rarità.

`POST /admin/cards/{id}/deactivate` e `/activate` rispondono `200` con la carta e sono idempotenti (disattivare una carta già disattivata non cambia niente). Non toccano le varianti: una versione evento si attiva e disattiva per conto suo.

### Illustrazioni (comportamento già implementato, API-14, #84)

Decise il 22 novembre: le immagini stanno in una **cartella** e non nel database, e si servono dall'API.

| Metodo | URL | Corpo | Risposta |
|---|---|---|---|
| `POST` | `/admin/cards` | `multipart/form-data`: parte `card` (JSON, gli stessi campi e controlli di `PUT /admin/cards/{id}`) e parte `image` (il file, obbligatorio) | `201` con la carta |
| `POST` | `/admin/cards/{id}/image` | `multipart/form-data`: parte `image` | `200` con la carta |
| `GET` | `/images/<imagePath>` | pubblico, senza token | il file, con cache di un anno |

- **Creazione:** gli stessi controlli del `PUT` e nello stesso ordine (riferimenti, regole del tipo, rarità, nome, effetto), poi l'illustrazione: una carta non valida non scrive nessun file. Con `variantOfId` la carta nasce variante e copia i campi di gioco dall'originale (regola 8); nome, illustrazione e `flavorText` sono suoi. Una carta nuova è attiva. Per un'originale nuova il nome si controlla con `existsByNameAndVariantOfIsNull`: la query con `IdNot` e un id nullo non troverebbe mai un doppione.
- **File ammessi:** PNG, JPEG o WebP fino a **2 MB** (`reversky.storage.max-image-size`), riconosciuti dai primi byte del file e mai dal nome o dal `Content-Type` del client. Un file vuoto, più grande o di un altro tipo è `422 CARD_IMAGE_INVALID` ("The image is empty", "The image must not exceed 2 MB", "The image must be a PNG, JPEG or WebP file"): la carta non si crea, `image_path` non cambia. Senza la parte `image` o `card` la risposta è `400`. Oltre il limite di Spring sul multipart (3 MB per file, 4 MB per richiesta) la risposta è `413`.
- **Dove:** `common/storage/ImageStorage` scrive in `reversky.storage.images-dir` (`uploads` dentro `reversky-api` con `bootRun`, ignorata da Git; `/app/uploads` nel container, sul volume `images` di `compose.yaml`; `build/test-images` nei test). Il percorso salvato in `image_path` è relativo, es. `cards/<uuid>.png`: ogni caricamento ha un nome nuovo, così il browser non mostra la vecchia immagine dalla cache.
- **URL:** ogni carta restituita ha `imagePath` e `imageUrl` = `/images/` + `imagePath`; il frontend aggiunge l'indirizzo dell'API. Le 120 carte di `V11` hanno ancora i percorsi segnaposto senza file: il loro `imageUrl` risponde `404` finché non si carica l'illustrazione.
- **Transazioni:** il file system non partecipa alle transazioni. Il file nuovo si scrive dentro la transazione e si cancella se la transazione non arriva al commit; il file vecchio di una sostituzione si cancella solo dopo il commit (un segnaposto senza file non è un errore). Così `image_path` non indica mai un file che non esiste. Un percorso che uscirebbe dalla cartella (`../`) non si cancella.
- **Consegna (API-61):** le immagini non sono nel `pg_dump`: la cartella (o il volume `images`) va nell'archivio insieme all'export del database.
- Il pacchetto ha anche lui un'immagine (`packs.image_path`), ma resta un percorso scritto nella richiesta: `ImageStorage` si può riusare quando servirà il caricamento.

### Effetti (comportamento già implementato, task API-36, #106)

- `GET /admin/effects` (`AdminEffectController`, `EffectService`), solo ADMIN e in sola lettura: `[{ code, appliesTo }]` per tipo di carta (ordine dell'enum `card_type`: CREATURE, TERRAIN, SPELL, HIDDEN, ITEM) e poi per codice. Nessuna paginazione: sono 92 righe.
- `PUT /admin/cards/{id}` controlla l'effetto di un'originale con `effectCode` con `EffectService.validateForType`: è l'ultimo controllo, dopo il nome. Una creatura senza effetto non ha niente da controllare; una variante copia l'effetto dall'originale e quello della richiesta si ignora, anche se non esiste. Messaggi: "The effect X is not implemented by the game engine", "The effect X is for CREATURE cards, not SPELL".
- Lo stesso controllo lo fa il database con `fk_catalog_cards_effect` (vedi [database](../database/database.md#tabella-effects-v13-api-36)), quindi anche una carta inserita da migrazione non può avere un effetto sbagliato. Anche `POST /admin/cards` (API-14) usa `validateForType`, nello stesso punto dei controlli.

## Dati di riferimento

| Metodo | URL | Tabella |
|---|---|---|
| `GET/POST/PUT` | `/admin/rarities` | `rarities` (`deck_limit` ≥ 1, `max_per_deck`, `sorting_order` univoco, scelto alla creazione e poi non più modificabile) |
| `GET/POST/PUT` | `/admin/factions` | `factions` (`name`, `emblem_path`, `description` facoltativa), `faction_tags` |
| `GET/PUT` | `/admin/faction-bonuses` | `faction_bonuses` (`atkBonus` anche negativo per il malus, `defBonus` ≥ 0, mai una fazione contro se stessa) |
| `GET/POST/PUT` | `/admin/tags` | `tags` |

Modificare `faction_bonuses` cambia il bilanciamento di **tutte** le partite future: va fatto solo dopo aver verificato l'effetto con le simulazioni dell'engine.

### Bonus di fazione: comportamento già implementato

Le classi stanno in `catalog/` insieme a fazioni e rarità (`FactionBonusService`, `FactionBonusRepository`, entità `FactionBonus` con chiave `FactionBonusId`, DTO ed eccezioni `FactionBonus*`); il controller è `admin/controller/AdminFactionBonusController`. `FactionBonus` implementa `Persistable`, quindi il `PUT` reinserisce le righe con soli `INSERT`, senza una `SELECT` per riga.

- `GET /admin/faction-bonuses` restituisce tutte le righe con i nomi delle due fazioni (`originFactionId`, `originFactionName`, `targetFactionId`, `targetFactionName`, `atkBonus`, `defBonus`), ordinate per origine e poi per bersaglio.
- `PUT /admin/faction-bonuses` riceve **tutta** la matrice (una lista di `{ originFactionId, targetFactionId, atkBonus, defBonus }`) e la sostituisce in una sola transazione: le coppie che non compaiono vengono eliminate, `[]` svuota la matrice. Risponde `200` con la nuova matrice, nello stesso ordine del `GET`.
- `A → B` e `B → A` sono due righe diverse: il bonus di una fazione contro un'altra non implica quello inverso.
- Tutte le righe si controllano prima di toccare la tabella: se una è sbagliata la matrice resta quella di prima.
- Due `PUT` contemporanei si mettono in fila (blocco `SHARE ROW EXCLUSIVE` sulla tabella) e vince l'ultimo; le letture non aspettano e vedono la matrice precedente finché la sostituzione non è confermata.
- Le stesse regole le controlla l'engine nella validazione dell'input (`INVALID_FACTION_BONUS`), quindi una matrice salvata qui è sempre accettata dall'engine.
- La matrice non è versionata con il rule set: `GET /rules` restituisce sempre quella in vigore. Le partite giocate restano coerenti perché ne salvano una copia (vedi [sfide](../sfide/sfide.md)).
- Senza audit, come il resto del backoffice (task API-52).

| Caso | Status | `code` |
|---|---|---|
| riga `null`, campo mancante o `defBonus` negativo (`errors[].field` con l'indice della riga, es. `[1].defBonus`) | `400` | `VALIDATION_FAILED` |
| fazione abbinata a se stessa | `422` | `FACTION_BONUS_SELF_MATCHUP` |
| fazione inesistente | `422` | `FACTION_BONUS_FACTION_NOT_FOUND` |
| stessa coppia due volte nel corpo | `422` | `FACTION_BONUS_DUPLICATE` |

Chi chiama `FactionBonusService.replaceAll` senza passare dal controller riceve `FACTION_BONUS_RULES_VIOLATED` (`422`) per una riga `null`, un campo mancante o un `defBonus` negativo.

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

### Comportamento già implementato

- `POST` e `PUT` ricevono lo stesso corpo (`RuleSetRequest`), con tutti i campi obbligatori. Una nuova versione nasce **disattivata**.
- `GET /admin/rule-sets` restituisce `id`, `version`, `isActive` e `createdAt` di ogni versione, la più recente per prima (`id` decrescente); `POST`, `PUT` e `activate` restituiscono il rule set completo.
- Anche la versione attiva si può modificare con `PUT`, finché nessuna partita l'ha usata.
- `activate` risponde `200` con il rule set ed è idempotente (attivare la versione già attiva non cambia niente). Blocca la versione attiva, quindi due attivazioni contemporanee si mettono in fila e vince l'ultima; se non c'era nessuna versione attiva, la seconda riceve `409` e basta ripeterla.
- `GET /rules` (pubblico, senza token) restituisce i valori della versione attiva senza `id`, `isActive`, `createdAt` e `createdById`; `404` se nessuna versione è attiva (il seed `V9` di #85 attiva la `1.0`). Contiene anche `factionBonuses`, la matrice corrente dei bonus di fazione senza i nomi (`originFactionId`, `targetFactionId`, `atkBonus`, `defBonus`), ordinata per origine e bersaglio; lista vuota se non ci sono bonus.
- Il controllo "mai usata da una partita" legge `battles` con una query nativa, perché l'entità `Battle` non esiste ancora: con `battle/` passerà al suo service.

I controlli avvengono prima di modificare il rule set, in questo ordine:

| Caso | Status | `code` |
|---|---|---|
| campi mancanti o fuori dai limiti del DTO (versione vuota o oltre 20 caratteri, punti vita, round, carte totali e creature < 1, altre parti del mazzo, massimi delle Hidden e crediti < 0) | `400` | `VALIDATION_FAILED` |
| rule set inesistente (`PUT`, `activate`) | `404` | `RULE_SET_NOT_FOUND` |
| solo `PUT`: rule set già usato da una partita | `409` | `RULE_SET_IN_USE` |
| creature + Spell + Hidden + Oggetti + Terreni diverso da `deckSize` | `422` | `RULE_SET_RULES_VIOLATED` |
| versione già usata da un altro rule set | `409` | `RULE_SET_VERSION_TAKEN` |
| solo `activate`: un'altra attivazione nello stesso momento, senza una versione attiva da bloccare | `409` | `RULE_SET_ACTIVATION_CONFLICT` |

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

### Pacchetti: comportamento già implementato (API-18 #88, API-19 #89, API-20 #90)

`admin/controller/AdminPackController` e `pack/service/PackAdminService`. `POST` e `PUT /admin/packs` ricevono tutto in una richiesta (deciso il 21 novembre): `name`, `cost`, `purchaseLimit`, `imagePath`, `slots` `[{ rarityId, quantity }]` (almeno uno) e `bonuses` `[{ rarityId, probabilityUnits }]` (anche vuota). `PUT` sostituisce tutto, aggiornando le righe di slot e bonus esistenti invece di svuotare e reinserire. Le risposte riportano `active`, `cardCount` (somma degli slot), slot e bonus dalla rarità più comune, con `percentage` per ogni bonus. Senza audit e senza tag (API-21).

- Un pacchetto nasce disattivato; `POST /{id}/activate` controlla che ogni slot base trovi almeno una carta attiva e originale, anche con il ripiego (`pack/service/CardPool`); un `PUT` su un pacchetto attivo fa lo stesso controllo.
- `GET /api/v1/packs` (pubblico) elenca i pacchetti attivi con costo, limite, slot e solo i bonus di rarità che hanno carte.

| Caso | Status | `code` |
|---|---|---|
| campi mancanti o fuori dai limiti, slot vuoti, rarità ripetuta (`slotRaritiesUnique`, `bonusRaritiesUnique`), pacchetto gratuito senza limite (`freePackLimited`) | `400` | `VALIDATION_FAILED` |
| pacchetto inesistente | `404` | `PACK_NOT_FOUND` |
| nome già usato | `409` | `PACK_NAME_ALREADY_USED` |
| rarità inesistente in uno slot o in un bonus | `422` | `PACK_RARITY_INVALID` |
| bonus non più raro di tutti gli slot base | `422` | `PACK_RULES_VIOLATED` |
| slot base che non trova nessuna carta, neanche con il ripiego (attivazione o modifica di un pacchetto attivo) | `422` | `PACK_NOT_FILLABLE` |

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

### Utenti: comportamento già implementato (API-03, #73)

`admin/controller/AdminUserController` e `user/service/UserAdminService` (separato da `UserService` perché usa `AuthService`, che dipende già da `UserService`). L'admin che fa la richiesta si ricava sempre dal token.

- `GET /admin/users?q=&role=&enabled=&page=&size=`: paginato in ordine di `id`; `q` cerca nello username o nell'email senza distinguere maiuscole e minuscole, `role` (`USER`, `ADMIN`, maiuscole facoltative; un ruolo inesistente dà una pagina vuota), `enabled` (`true`, `false` o vuoto). Ogni riga: `id`, `username`, `email`, `role`, `enabled`, `emailVerified`, `credits`, `createdAt`. Ruoli e saldi si leggono con una query ciascuno per tutta la pagina.
- `POST /admin/users/{id}/disable` e `/enable`, `PUT /admin/users/{id}/role` con `{ "role": "ADMIN" }`: restituiscono l'utente con la stessa forma della lista. Sono idempotenti: disattivare un account già disattivato o assegnare lo stesso ruolo non cambia niente (la disattivazione cancella comunque i refresh token).
- La disattivazione cancella i refresh token (`AuthService.revokeAllSessions`); gli access token già emessi restano validi fino alla scadenza, al massimo 15 minuti, come il cambio di ruolo. Il ticket in `matchmaking_queue` si cancellerà con API-47 (TODO in `UserAdminService.disable`).
- Senza audit finché non arriva API-52.

| Caso | Status | `code` |
|---|---|---|
| ruolo mancante o diverso da `USER` e `ADMIN` | `400` | `VALIDATION_FAILED` |
| utente inesistente | `404` | `USER_NOT_FOUND` |
| un admin disattiva se stesso o si toglie il ruolo `ADMIN` | `422` | `ADMIN_SELF_ACTION` |

### Correzione dei crediti: comportamento già implementato (API-17, #87)

`POST /admin/users/{id}/credits` (`admin/controller/AdminUserController`, `InventoryService.adjustCredits`) riceve `{ "amount": -50, "reason": "..." }` e restituisce il nuovo saldo `{ "credits": 300 }`. `amount` positivo aggiunge, negativo toglie; il saldo non scende mai sotto zero.

| Caso | Status | `code` |
|---|---|---|
| `amount` mancante, `reason` vuoto o oltre 255 caratteri | `400` | `VALIDATION_FAILED` |
| `amount` uguale a 0 (errore sul campo `amountNonZero`) | `400` | `VALIDATION_FAILED` |
| utente inesistente | `404` | `INVENTORY_NOT_FOUND` |
| la correzione porterebbe il saldo sotto zero | `422` | `INSUFFICIENT_CREDITS` |

Finché non c'è l'audit (API-52, #122) il motivo non sta in `audit_logs`: si scrive solo nel log dell'applicazione (TODO in `InventoryService.adjustCredits`).

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
