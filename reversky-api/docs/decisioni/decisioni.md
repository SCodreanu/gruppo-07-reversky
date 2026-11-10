# Decisioni del backend

Registro di **tutte le scelte già prese** e di **tutte le domande ancora aperte** per chiudere i task di `reversky-api`.
Le decisioni del motore di gioco (D-xx) restano in [decisioni aperte dell'engine](../../../reversky-engine/docs/decisioni-aperte/decisioni-aperte.md): qui sono riassunte solo quelle che toccano l'API o vanno portate ai DSG.

Come si usa:

- Una domanda si chiude scrivendo la risposta nella sua riga, spostandola nella sezione **Decise** e aggiornando il documento di riferimento in `docs/`.
- Finché una domanda è aperta, il codice usa la **Proposta** e il valore resta segnato **[DA CONFERMARE]** nei documenti.
- **Blocca** indica le issue che non si possono chiudere senza la risposta.

## Da chiedere

### Ai DSG e al cliente (regolamento ed economia)

| # | Domanda | Proposta | Blocca | Riferimento |
|---|---|---|---|---|
| Q-05 | **Catalogo iniziale**: la distribuzione è decisa (120 carte, vedi sotto), mancano nomi, illustrazioni ed effetti di ogni carta | ATK e DEF li propone il bilanciamento (API-64); nomi ed effetti dai DSG | #125, #106 | D-24 |
| Q-23 | **Oggetti**: quale effetto passivo ha ciascuno dei 4 Oggetti (uno per fazione)? | come i Terreni: un bonus passivo che resta fino alla fine della partita; valori dal bilanciamento | ENG-33, #125 | [carte dell'engine](../../../reversky-engine/docs/carte/carte.md), D-47 |
| Q-06 | **Carte speciali** (The Last Gift, edizione di Natale): rarità `Special`? | rarità `Special` con `sorting_order` 7, fuori dalle bustine normali | #85, #125 | D-23 |
| Q-07 | Le carte **disattivate** restano utilizzabili nei mazzi di chi le possiede? | sì | #98 | [mazzi](../mazzi/mazzi.md) |
| Q-08 | Le **varianti** contano nel totale della barra di completamento della collezione? | no, conta solo la carta originale | #93 | [collezione](../collezione/collezione.md) |
| Q-09 | Uno scambio deve avere **almeno una carta per lato** (niente regali)? | sì | #103 | [scambi](../scambi/scambi.md) |
| Q-10 | **Classifica** (`GET /rankings` e posizione in `/rankings/me`): si fa? | probabilmente no; se sì, ordinata per `rating` decrescente e poi `user_id`, senza account disattivati | #116 | [sfide](../sfide/sfide.md) |

### Bilanciamento (SWD, task API-64)

Il bilanciamento lo fanno gli SWD (Tommaso con Claude), con le simulazioni dell'engine: i valori proposti qui sotto restano finché i test non dicono altro.

| # | Domanda | Proposta | Blocca | Riferimento |
|---|---|---|---|---|
| Q-02 | **Copie per rarità** (`deck_limit`) | Common 3, Uncommon 2, Rare 2, Mythic 1, Legendary 1, Mutant 1 | #85, #98, API-64 | [mazzi](../mazzi/mazzi.md) |
| Q-03 | **Massimo per rarità nel mazzo** (`max_per_deck`) | Legendary 2, Mythic 2, Mutant 1, Rare 5 (Terreno e Oggetto occupano un posto ciascuno delle Rare) | #85, #98, API-64 | [mazzi](../mazzi/mazzi.md) |

### Ai Digital Strategist (modello economico)

| # | Domanda | Proposta | Blocca | Riferimento |
|---|---|---|---|---|
| Q-11 | **Crediti di benvenuto** (`signup_bonus`) | 500 | #85, #87 | [crediti](../crediti/crediti.md) |
| Q-12 | **Ricompense** delle classificate: vittoria e pareggio | 30 e 10 (le amichevoli non danno crediti) | #85, #115 | [crediti](../crediti/crediti.md) |
| Q-13 | **Costo del pacchetto base** e sua composizione (slot e bonus) | nessuna: serve un valore per la migrazione del catalogo | #125 | [pacchetti](../pacchetti/pacchetti.md) |
| Q-14 | **Nuovi giocatori**: come ottengono un mazzo valido? Un mazzo da 30 carte richiede almeno 5 pacchetti da 7, e le classificate (unica fonte di crediti dopo la registrazione) richiedono un mazzo valido. Un `signup_bonus` troppo basso blocca il giocatore | `signup_bonus` sufficiente per un mazzo, oppure un pacchetto omaggio a quantità limitata (`cost = 0`), che però non garantisce una composizione valida | #85, #87 | [crediti](../crediti/crediti.md), [mazzi](../mazzi/mazzi.md) |
| Q-15 | Serve un **credito da evento** (proposta al cliente)? | se sì, nuovo valore `EVENT_REWARD` in `credit_reason`, con una migrazione nuova | #86 | [crediti](../crediti/crediti.md) |

### Al frontend (WMD)

| # | Domanda | Proposta | Blocca | Riferimento |
|---|---|---|---|---|
| Q-17 | **Ticket di matchmaking sparito**: va comunicato al frontend che deve confrontare il `createdAt` delle partite nello storico con il proprio `queuedAt` | così, senza endpoint dedicato | #117, #119 | [sfide](../sfide/sfide.md) |
| Q-18 | Link nelle email: il frontend gira su `http://localhost:3000` con `/verify-email?token=...` e `/reset-password?token=...`? | così | — | [autenticazione](../autenticazione/autenticazione.md) |

### Nel team SWD (tecniche)

| # | Domanda | Proposta | Blocca | Riferimento |
|---|---|---|---|---|
| Q-19 | **Inserimenti contemporanei duplicati** (due nomi uguali nello stesso istante): oggi passano il controllo nel service e la violazione del vincolo diventa `500` | un gestore di `DataIntegrityViolationException` in `GlobalExceptionHandler` che risponde `409`, in un task a parte | #100, #128 | [api-rest](../api-rest/api-rest.md) |
| Q-20 | **Username senza distinzione tra maiuscole e minuscole**: da fare nel codice o con un indice su `LOWER(username)`? Un controllo solo nel codice non ferma due registrazioni contemporanee | nel codice, più avanti (scelta non confermata in modo esplicito) | — | [autenticazione](../autenticazione/autenticazione.md) |
| Q-21 | **Autenticazione dell'engine** in produzione | header `X-Engine-Key` da variabile d'ambiente, solo se c'è un deploy pubblico | #105 | D-40 |
| Q-22 | `schema.dbml` non è mai stato verificato da uno strumento: va aperto e salvato di nuovo in dbdiagram.io | farlo con la baseline: le migrazioni reali lo verificano | #141, #131 | [database](../database/database.md) |

### Decisioni del motore portate ai DSG il 9 novembre

Priorità **Alta** (#41, chiusa). Dettagli nel [documento dell'engine](../../../reversky-engine/docs/decisioni-aperte/decisioni-aperte.md).

| ID | Tema | Esito | Stato |
|---|---|---|---|
| D-01 | Punti vita iniziali | proposta 30 | aperto |
| D-02 | Limite di round | 75 round totali: valore iniziale del rule set `1.0` (`max_rounds`) | deciso |
| D-03 | Danno al giocatore (incoerenza tra brief e bozza) | DEF negativa: 8 contro DEF 5 → 3 | deciso |
| D-05 | Condizioni di fine partita 3–5 | come in `regole-base` | deciso |
| D-08 | Morti fuori dal round | controllo delle morti dopo ogni fase | deciso |
| D-10 | Spell o Hidden pescate (incoerenza nella bozza) | `SHIELD` in attesa e sommate sulla prossima creatura; `DAMAGE` subito sulla creatura avversaria; poi scartate | deciso |
| D-15 | Attivazione delle Hidden | inizio del prossimo scontro, prima `DAMAGE` poi `SURVIVAL` | deciso |
| D-17 | A chi dà il bonus il Terreno | a tutte le creature della sua fazione in campo, anche avversarie | deciso |
| D-30 | Ordine degli effetti contemporanei | `NULLIFY` prima, poi sfidante prima dell'avversario | deciso |
| D-47 | Effetti degli Oggetti | proposta: bonus passivo per fazione | aperto |

Decise tra gli SWD: D-31 (numero massimo di attivazioni per effetto, dettagli con il nuovo catalogo), D-34 (modificatori in attesa), D-35 (origine dei modificatori, con `ITEM`), D-41 (springdoc 3.2.0 nell'engine).

## Decise

### Catalogo e mazzo (scheda di progettazione del 9 novembre)

| Decisione | Perché | Quando |
|---|---|---|
| Catalogo di **120 carte**: 92 Creature, 12 Spell, 8 Hidden, 4 Terreni, 4 Oggetti | scheda di progettazione dei DSG | 9 nov |
| Ogni fazione ha 25 carte: 23 Creature (Common 7, Uncommon 6, Rare 4, Mythic 3, Legendary 2, Mutant 1), 1 Terreno e 1 Oggetto | come sopra | 9 nov |
| Le 12 Spell e le 8 Hidden sono **condivise**: non hanno fazione (`faction_id` nullo, `chk_catalog_cards_faction_matches_type`) | come sopra | 9 nov |
| Mazzo da **30 carte**: 20 Creature, 6 Spell, 2 Hidden, 1 Oggetto, 1 Terreno (chiude Q-01); in `rule_sets` `deck_specials` diventa `deck_spells` e `deck_hidden`, e si aggiunge `deck_items` | come sopra; i valori possono cambiare dopo il bilanciamento, solo dal backoffice | 9 nov |
| Nuovo tipo di carta **Oggetto** (`ITEM`): come il Terreno resta in gioco fino alla fine della partita con un effetto passivo, ha una fazione, è Rare e occupa un posto delle Rare nel mazzo; non esce mai dal ripiego degli slot | scelta del team | 9 nov |
| Nessun vincolo tra la fazione del Terreno o dell'Oggetto e le creature del mazzo | la scheda non lo chiede | 9 nov |
| Bilanciamento (limiti di copie, massimi per rarità nel mazzo, statistiche delle carte) a cura degli SWD | scelta di Tommaso | 9 nov |
| **Niente massimi di ATK e DEF per rarità**: tolti `rarities.max_atk` e `max_def` (chiude Q-04) | le carte personalizzate possono superare qualsiasi soglia; il bilanciamento resta nelle statistiche di ogni carta | 9 nov |
| **Notifiche con SSE** (amicizie, inviti, partite, scambi), più una tabella `notifications` (già nella baseline, #141); le implementano Tommaso e Claude più avanti (task API-65 – API-69). Chiude Q-16 | il frontend riceve gli eventi senza polling | 9 nov |

### Progetto e infrastruttura

| Decisione | Perché | Quando |
|---|---|---|
| **PostgreSQL 18** invece di MySQL | concordato con il cliente e approvato | prima del 4 nov |
| Spring Boot 4.1.2, Java 25, Flyway, Lombok, springdoc 3.2.0 | stack comune ai due moduli | inizio progetto |
| Organizzazione **per funzionalità** con sotto-package per livello | ogni membro lavora su una funzionalità senza toccare le altre | inizio progetto |
| ~~Una migrazione per task~~ → **schema bloccato**: tutta la struttura in una baseline unica (`V1`–`V8`, #141) che sostituisce le vecchie `V1`–`V6`; gli altri task scrivono solo Java. Un cambiamento dopo la baseline va in una migrazione nuova, deciso insieme; `ddl-auto=validate` | nessun conflitto di numeri tra branch, nessun ordine forzato tra task dalle chiavi esterne, SQL scritto e verificato una volta sola. Nessuna migrazione era ancora su `main` | 9 nov |
| Il primo `ADMIN` e gli utenti di prova arrivano dalla migrazione dei dati di prova (#95), non da variabili d'ambiente | scelta più semplice per il team | 4 nov |
| I test usano PostgreSQL vero con Testcontainers e caricano tutta l'applicazione (niente `@WebMvcTest`) | passano davvero da sicurezza, errori e database | #71 |
| Lingua: documenti e commenti in italiano, nomi nel codice e testi tra virgolette in inglese; il gioco è in inglese (D-44) | decisione del team | 4 nov |

### Sicurezza e autenticazione

| Decisione | Perché | Quando |
|---|---|---|
| Access token JWT da 15 minuti e refresh token da 30 giorni con rotazione | nessuna sessione sul server | #72 |
| Password con BCrypt; token (refresh, reset, verifica) salvati solo come hash SHA-256 | un furto del database non espone credenziali | #72 |
| Un solo token di reset attivo per utente, garantito nel codice | il nuovo link annulla il precedente | 4 nov |
| Il ruolo si controlla in `SecurityConfig` (`/api/v1/admin/**` solo `ADMIN`), senza `@PreAuthorize` nei controller | una regola sola per tutto il backoffice | #76 |
| `GET` pubblici: `/cards/**`, `/packs`, `/factions`, `/rarities` | servono anche alle pagine SEO | #76, #75 |
| Il segreto JWT ha un valore di default solo per comodità del team: **va tolto prima del primo deploy** | senza `JWT_SECRET` l'applicazione non deve partire | #72 |
| Mai scrivere nei log password, token o dati personali | privacy | inizio progetto |

### Schema del database (revisione del 4 novembre, 32 tabelle)

| Decisione | Perché |
|---|---|
| `roles` senza `authority_level`; i ruoli `USER` e `ADMIN` si inseriscono in `V2` | il codice ne usa i nomi |
| `username` fino a 30 caratteri; date di accettazione di privacy e termini obbligatorie | richiesto dalla registrazione |
| Tutte le date sono `TIMESTAMPTZ` mappate su `Instant`; niente `SMALLINT` | un solo fuso (UTC) e un solo tipo intero |
| `factions` con `emblem_path` obbligatorio e `description` facoltativa; i dati iniziali in ordine Wound, Invitation, Archive, Dream | gli id coincidono con gli esempi dell'engine |
| `faction_bonuses`: `atk_bonus` può essere negativo (malus −1), `def_bonus` ≥ 0 | D-04 |
| `tags` solo per i filtri dei pacchetti, formato `^[A-Z0-9_]+$`; effetto e sottotipo sono colonne di `catalog_cards` | il database garantisce le regole di effetti e sottotipi (sostituisce i tag `EFFECT_`) |
| `catalog_cards`: statistiche legate al tipo, `copy_limit` nullo = solo il limite della rarità, `effect_code` obbligatorio per Spell, Hidden e Terreni, immagine obbligatoria, varianti evento con `variant_of_id` e nome univoco solo tra le originali | le creature per ora non hanno effetti |
| `rule_sets` versionati con uno solo attivo; le ricompense valgono solo nelle classificate | le amichevoli non danno crediti |
| `inventories` con chiave `user_id`; ogni movimento di crediti scrive `credit_transactions`, anche con importo 0 (pacchetto gratuito) | storico completo per i DSG |
| Omaggi = pacchetti con `cost = 0` e `purchase_limit` obbligatorio, niente tabelle dedicate | stesso flusso di un acquisto normale |
| `pack_openings` registra ogni apertura | alimenta `/admin/stats` |
| Slot base vuoto: si scende di rarità, poi si sale, mai su un Terreno; il backoffice rifiuta un pacchetto attivo che non può riempire gli slot | l'apertura non deve fallire per configurazione |
| Lacuna accettata: disattivare l'ultima carta idonea non viene controllato, e l'apertura risponde `422` lasciando il pacchetto nell'inventario | costo del controllo troppo alto ora |
| `friendships`: niente stato `REJECTED` (il rifiuto elimina la riga), una sola riga per coppia | una nuova richiesta è sempre possibile |
| `trades`: niente scadenza, niente stato `PENDING`; le copie offerte sono riservate per query, non spostate | chiunque dei due può chiudere lo scambio |
| `battle_invites`: niente scadenza, il rifiuto elimina la riga, al massimo un invito `PENDING` per direzione | — |
| `battles`: il salvataggio blocca la riga e si ferma se `result` è già valorizzato | due retry sovrapposti non pagano due volte Elo e crediti |
| Elo approvato dal cliente; `player_ratings` parte da 1000 e conta solo le classificate | — |
| `audit_logs` non contiene mai `password_hash` né token; chiavi composte scritte come `"2:3"` | le righe di audit non si cancellano |
| Lucchetti sempre sulle righe di `inventories` in ordine crescente di `user_id` | evita i deadlock negli scambi e nei pareggi |
| `deck_cards` e `trade_offers` si aggiornano riga per riga, mai svuotando e reinserendo | Hibernate esegue gli insert prima dei delete e viola la chiave |

### Regolamento e valori di gioco

| Decisione | Perché | Quando |
|---|---|---|
| Valori di gioco (punti vita, round, mazzo, crediti, limiti per rarità, bonus di fazione) **nel database**, modificabili dal backoffice, mai nel codice | D-45 | 4 nov |
| I numeri dentro gli effetti stanno nel codice dell'engine; il backoffice sceglie solo `effect_code` | D-46 | 4 nov |
| Spell, Hidden e Terreni fanno parte del regolamento standard | D-21, approvate dal cliente | 2 nov |
| Carte con due fazioni: seconda fazione facoltativa, si sommano vantaggi e svantaggi | D-22 | 4 nov |
| Il Terreno occupa uno dei posti delle Rare in `max_per_deck` | scelta del team | 4 nov |
| Le varianti contano insieme all'originale per il limite di copie | cambiano solo la grafica | 4 nov |

### Catalogo implementato

| Decisione | Perché | Task |
|---|---|---|
| I controller del backoffice stanno in `admin/controller/` e chiamano i service delle funzionalità, mai i repository | un solo posto per le regole | #76 |
| `faction_tags` rimandato a #83, la scrittura dell'audit a #122 | fuori dal perimetro dei task | #76, #75 |
| Rarità: due DTO, `RarityCreateRequest` con `sortingOrder` e `RarityUpdateRequest` senza | `sorting_order` arriva all'engine come `rarityOrder` e gli effetti lo confrontano con soglie nel codice | #75 |
| `sorting_order` univoco, scelto alla creazione e poi immutabile; doppia protezione con `@Column(updatable = false)` | come sopra | #75 |
| Un `sortingOrder` inviato nel `PUT` viene ignorato, non rifiutato (verificato da un test: Jackson ignora i campi sconosciuti) | client più semplici | #75 |
| `GET /rarities` ordinato per `sorting_order` | ordine naturale per il frontend | #75 |

## Prima della consegna

Cose decise ma non ancora fatte, da non dimenticare:

- [ ] Togliere il default del segreto JWT da `application.properties` e `compose.yaml` (#128).
- [ ] Gestore per `DataIntegrityViolationException` → `409`, se Q-19 viene approvata.
- [ ] Inventario alla registrazione e crediti in `/users/me` (TODO nel codice, #87).
- [ ] Comunicare al team le decisioni della revisione dello schema (effetti e sottotipi come colonne, niente stati `REJECTED`/`PENDING`/`EXPIRED`).
- [ ] Verificare `schema.dbml` in dbdiagram.io (Q-22) ed esportare il database con utenti di prova (#131).
