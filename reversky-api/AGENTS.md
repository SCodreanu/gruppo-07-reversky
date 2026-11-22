# AGENTS.md — reversky-api

Istruzioni per gli agenti AI (Claude Code, Codex, Copilot, ecc.) e per chiunque lavori su questo modulo.
Leggi questo file **prima** di modificare il codice.

## Cos'è questo modulo

`reversky-api` è il backend REST di **Reversky**, il gioco di carte collezionabili digitale di Downfall (gruppo 07).
Gestisce tutto ciò che **non** è la simulazione della partita, ed è l'unico modulo che parla con il database:

- registrazione, login e utenti;
- inventario: crediti virtuali e pacchetti da aprire;
- catalogo carte, pacchetti e loro apertura;
- collezione dell'utente;
- mazzi e loro validazione;
- amicizie;
- scambi tra utenti (con controproposte);
- sfide e storico partite;
- backoffice (catalogo, pacchetti, utenti) con registro di audit.

La partita **non** viene calcolata qui: viene delegata al modulo `reversky-engine`, chiamato via HTTP da un service dedicato (vedi [docs/sfide](docs/sfide/sfide.md)).

## Stack

| Cosa | Versione / Libreria |
|---|---|
| Linguaggio | Java 25 |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL 18 |
| Migrazioni | Flyway |
| Build | Gradle (wrapper incluso) |
| Test | JUnit 5, Spring Boot Test, Testcontainers |
| Documentazione API | Swagger / OpenAPI (springdoc) |
| Utility | Lombok |

## Comandi

```bash
# Avvio in locale (avvia anche il container PostgreSQL tramite compose.yaml, serve Docker)
./gradlew bootRun          # su Windows: gradlew.bat bootRun

# Test (serve Docker per Testcontainers)
./gradlew test

# Build completa
./gradlew build
```

Swagger UI, una volta configurato springdoc: `http://localhost:8080/swagger-ui.html`.

## Struttura del codice

Organizzazione **per funzionalità** (package-by-feature), e dentro ogni funzionalità un sotto-package per livello:

```
src/main/java/it/downfall/reversky/api/
├── auth/          registrazione, login, token, reset password, verifica email
├── user/          utenti e ruoli
├── inventory/     crediti e pacchetti da aprire
├── catalog/       carte, rarità, fazioni, bonus di fazione, tag
├── ruleset/       regole di gioco versionate (rule set attivo, GET /rules)
├── pack/          pacchetti: acquisto e apertura
├── collection/    carte possedute
├── deck/          mazzi e validazione
├── friendship/    amicizie
├── trade/         scambi e controproposte
├── battle/        inviti, partite, storico, client verso l'engine
├── admin/         endpoint di backoffice e audit
└── common/        errori, sicurezza, configurazione condivisa
```

Dentro ogni funzionalità: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/` e, se servono task schedulati, `job/`. Si crea solo il sotto-package che serve; `common/` resta diviso per argomento. I test usano gli stessi package delle classi che testano. Dettagli ed esempio in [docs/architettura](docs/architettura/architettura.md).

## Documentazione per funzionalità

Prima di toccare una funzionalità, leggi il documento corrispondente:

| Argomento | File |
|---|---|
| Architettura e flusso delle richieste | [docs/architettura](docs/architettura/architettura.md) |
| Convenzioni REST, errori, Swagger | [docs/api-rest](docs/api-rest/api-rest.md) |
| Database, schema, migrazioni Flyway | [docs/database](docs/database/database.md) |
| Registrazione, login, token, ruoli | [docs/autenticazione](docs/autenticazione/autenticazione.md) |
| Crediti e inventario | [docs/crediti](docs/crediti/crediti.md) |
| Pacchetti e rarità | [docs/pacchetti](docs/pacchetti/pacchetti.md) |
| Collezione | [docs/collezione](docs/collezione/collezione.md) |
| Mazzi e vincoli | [docs/mazzi](docs/mazzi/mazzi.md) |
| Amicizie | [docs/amicizie](docs/amicizie/amicizie.md) |
| Scambi | [docs/scambi](docs/scambi/scambi.md) |
| Sfide e storico | [docs/sfide](docs/sfide/sfide.md) |
| Backoffice e audit | [docs/backoffice](docs/backoffice/backoffice.md) |
| Strategia di test | [docs/test](docs/test/test.md) |
| Stato del lavoro e piano fino alla consegna | [docs/roadmap](docs/roadmap/roadmap.md) |

Le regole di scrittura del codice sono in [CodingRequirements.md](CodingRequirements.md).

## Regole che non si discutono

1. **Mai calcolare la partita qui.** Le regole di combattimento vivono solo in `reversky-engine`.
2. **Lo schema del database è quello deciso dal team** (vedi [docs/database](docs/database/database.md)): non aggiungere tabelle o colonne senza chiedere.
3. **Ogni operazione che sposta crediti o carte è una transazione** (`@Transactional`) e non deve mai creare né perdere carte o crediti.
4. **La casualità sta solo sul server.** Il client non invia mai seed, probabilità o risultati.
5. **Il server ricontrolla sempre tutto**: possesso delle carte, vincoli del mazzo, saldo, turno negli scambi. La validazione del frontend è solo una comodità.
6. **Lo schema cambia solo tramite migrazioni Flyway nuove.** Non modificare mai una migrazione già committata.
7. **Niente acquisto di crediti con denaro reale**: è fuori dal perimetro della fase 1.
8. **Valori del regolamento** (punti vita, limite di round, prezzi, ricompense, limiti del mazzo…) stanno nel database e si modificano dal **backoffice** (`rule_sets`, `rarities`, `faction_bonuses`, `packs`), mai nel codice né in `application.properties`. Nei documenti sono segnati con **[DA CONFERMARE]**.

## Come lavorare

1. Leggi il documento della funzionalità e `CodingRequirements.md`.
2. Fai modifiche piccole e coerenti con il codice esistente.
3. Aggiungi o aggiorna i test (vedi [docs/test](docs/test/test.md)).
4. Esegui `./gradlew test` prima di dichiarare il lavoro finito.
5. Se cambi un comportamento, **aggiorna il documento in `docs/`** nello stesso commit.
6. Se un requisito è ambiguo o segnato [DA CONFERMARE], chiedi invece di inventare.

## Stato attuale

L'autenticazione è completa e testata (`auth/`, `user/`, `common/`, migrazioni `V1` e `V2`):

- registrazione, login, rinnovo con rotazione, logout, reset della password, verifica e reinvio della verifica email;
- `GET /api/v1/users/me`;
- pulizia giornaliera dei token scaduti (`TokenCleanupJob`);
- errori in formato ProblemDetail, Swagger con il pulsante Authorize;
- test di integrazione su PostgreSQL vero (`AuthServiceIT`, `AuthControllerIT`, `TokenCleanupJobIT`, `UserControllerIT`).

Gestione degli utenti dal backoffice (API-03): `GET /api/v1/admin/users` con ricerca, filtri `role` ed `enabled`, ruolo e crediti; `POST /api/v1/admin/users/{id}/disable` (cancella i refresh token) e `/enable`, `PUT /api/v1/admin/users/{id}/role`; un admin non può disattivarsi né togliersi il ruolo (`422 ADMIN_SELF_ACTION`). Dettagli in [docs/backoffice](docs/backoffice/backoffice.md#utenti-comportamento-già-implementato-api-03-73).

Inventario e crediti (`inventory/`, API-16 e API-17):

- la registrazione crea l'inventario con i crediti di benvenuto del rule set attivo e la riga `SIGNUP`; `GET /api/v1/users/me` restituisce anche `credits`;
- `InventoryService` è l'unico che modifica il saldo (`addCredits`, `spendCredits`, `lockInventories` in ordine di `user_id`), sempre con la riga di `credit_transactions`; `GET /api/v1/inventory` con il saldo;
- `POST /api/v1/admin/users/{id}/credits` per la correzione dal backoffice, con motivo obbligatorio (per ora solo nel log, l'audit arriva con API-52);
- `V10__create_missing_inventories.sql` crea l'inventario per gli utenti registrati prima. Dettagli in [docs/crediti](docs/crediti/crediti.md#comportamento-già-implementato-api-16-86-api-17-87).

Catalogo (`catalog/`):

- fazioni: `GET /api/v1/factions` pubblico e `GET/POST/PUT /api/v1/admin/factions` (`admin/controller/AdminFactionController`); i tag delle fazioni (API-13) con `PUT /api/v1/admin/factions/{id}/tags`, che sostituisce tutto l'insieme (tag inesistente `422 FACTION_TAG_INVALID`), e nelle sole risposte del backoffice (`AdminFactionResponse`); `V15` crea e collega `WOUND`, `INVITATION`, `ARCHIVE`, `DREAM`;
- rarità: `GET /api/v1/rarities` pubblico, ordinato per `sorting_order`, e `GET/POST/PUT /api/v1/admin/rarities` (`admin/controller/AdminRarityController`). Le rarità non hanno massimi di ATK e DEF (tolti con la baseline);
- carte, senza tag: `GET /api/v1/cards` pubblico e paginato con filtri `type`, `name` e `rarityId`, `GET /api/v1/cards/{id}`, e `GET/PUT /api/v1/admin/cards`, `POST /api/v1/admin/cards/{id}/deactivate` e `/activate` (`admin/controller/AdminCardController`). Le varianti ricevono le modifiche dell'originale. `POST /api/v1/admin/cards` (multipart: la carta in JSON e l'illustrazione obbligatoria, anche per creare una variante) e `POST /api/v1/admin/cards/{id}/image` (API-14): le immagini (PNG, JPEG o WebP fino a 2 MB, riconosciute dal contenuto) le salva `common/storage/ImageStorage` in una cartella (`reversky.storage.images-dir`, un volume nel container) e sono pubbliche su `GET /images/**`; le carte hanno `imageUrl`. Dettagli in [docs/backoffice](docs/backoffice/backoffice.md#illustrazioni-comportamento-già-implementato-api-14-84).
- effetti delle carte (API-36): la tabella `effects` di `V13__create_effects.sql` (92 codici del registro dell'engine, uno per tipo di carta, con la chiave esterna `fk_catalog_cards_effect`) e `GET /api/v1/admin/effects` (`admin/controller/AdminEffectController`); `PUT /admin/cards` rifiuta un effetto inesistente o di un altro tipo con `422 CARD_EFFECT_INVALID`. L'engine si chiama solo per simulare.
- rarità delle carte (task #80): `rarity_id` obbligatorio; Spell, Hidden, Terreni e Oggetti solo Rare (Spell e Hidden dal 21 novembre), riconosciute da `sorting_order` e mai dal nome.
- fazioni delle carte (task #81): `PUT /admin/cards` riceve `factionId` (obbligatoria per Creature, Terreni e Oggetti, vietata per Spell e Hidden) e `secondaryFactionId` (facoltativa, diversa dalla prima, solo per le creature: `V14` e API-71); una fazione inesistente è `422 CARD_FACTION_INVALID`. Le carte restituiscono `faction` e `secondaryFaction` come `{ id, name }`, e `GET /cards` e `GET /admin/cards` hanno il filtro `factionId` (prima o seconda fazione).
- tag (API-07): `GET/POST /api/v1/admin/tags` e `PUT /api/v1/admin/tags/{id}` (`admin/controller/AdminTagController`, `catalog/service/TagService`), in ordine di nome; il nome si controlla così com'è (`^[A-Z0-9_]+$`, al massimo 100 caratteri, altrimenti `400`) ed è univoco (`409 TAG_NAME_TAKEN`). I collegamenti a carte, fazioni e pacchetti non ci sono ancora (API-12, API-13, API-21). Dettagli in [docs/backoffice](docs/backoffice/backoffice.md#tag-comportamento-già-implementato-api-07-77).
- bonus di fazione: `GET/PUT /api/v1/admin/faction-bonuses` (`admin/controller/AdminFactionBonusController`, `catalog/service/FactionBonusService`): il `PUT` sostituisce tutta la matrice in una transazione, con due `PUT` contemporanei messi in fila. Senza audit. La matrice corrente compare anche in `GET /rules`. Dettagli ed errori in [docs/backoffice](docs/backoffice/backoffice.md#bonus-di-fazione-comportamento-già-implementato).

Regole di gioco (`ruleset/`):

- `GET /api/v1/rules` pubblico (rule set attivo, con la matrice corrente dei bonus di fazione) e `GET/POST /api/v1/admin/rule-sets`, `PUT /api/v1/admin/rule-sets/{id}`, `POST /api/v1/admin/rule-sets/{id}/activate` (`admin/controller/AdminRuleSetController`). Una versione già usata da una partita non si modifica; l'attivazione disattiva la precedente nella stessa transazione. Senza audit e con `created_by_id` nullo. Il rule set `1.0` attivo, le sei rarità e le quattro fazioni (id 1–4 come nell'engine) sono inseriti da `V9__insert_reference_data.sql` (#85); la matrice dei bonus di fazione è vuota finché i DSG non decidono D-04.

Amicizie (`friendship/`):

- `GET /api/v1/friends`, `GET /api/v1/friends/requests?box=incoming|outgoing`, `POST /api/v1/friends/requests`, `POST /api/v1/friends/requests/{requesterId}/accept` e `/reject`, `DELETE /api/v1/friends/{userId}` (`FriendshipController`). La richiesta inversa accetta quella esistente; rimuovere un amico elimina anche gli inviti di sfida `PENDING` tra i due (`battle/service/PendingInviteService`). Dettagli ed errori in [docs/amicizie](docs/amicizie/amicizie.md#comportamento-già-implementato).

Collezione (`collection/`):

- `CollectionService.addCards` e `removeCards`, gli unici che scrivono su `collection_cards`, dentro la transazione del chiamante; `GET /api/v1/collection` (filtri come `GET /cards`, `sort` su name, rarity, type o quantity, con copie riservate e mazzi che usano la carta), `GET /api/v1/collection/summary` e `GET /api/v1/users/{id}/collection` con le copie disponibili (`CollectionController`). Copie riservate e mazzi si leggono con SQL nativo in `CollectionCardRepository`: `TradeService` e `DeckService` usano `CollectionService`, quindi il contrario creerebbe un ciclo di bean. `getOwnedCopies` (copie possedute, senza riserve) serve agli scambi. Dettagli in [docs/collezione](docs/collezione/collezione.md#comportamento-già-implementato-task-api-23-93).

Sfide amichevoli (`battle/`, API-35, 37–44):

- `EngineClient` chiama `POST /api/v1/simulations` dell'engine (`reversky.engine.base-url`, default `http://localhost:8081`); engine non disponibile = `503 ENGINE_UNAVAILABLE`, dati rifiutati dall'engine (`4xx`) = `500 ENGINE_INPUT_REJECTED` (API-71); il JSON è controllato da `EngineContractTest` con le richieste e le fotografie delle risposte di `reversky-engine/examples/` (ENG-24);
- inviti tra amici con un mazzo valido (`/api/v1/battle-invites`, `box=incoming|outgoing`, `reject`, ritiro con `DELETE`); `accept` prepara la partita (seed dal server, fotografia dei mazzi, regole in `battle_stats`), fa simulare l'engine fuori transazione e salva l'esito; `GET /api/v1/battles` (storico senza log), `GET /api/v1/battles/{id}` (dettaglio dal punto di vista di chi guarda, con log) e `POST /api/v1/battles/{id}/retry` con la stessa identica partita. Classificate e crediti delle partite non ci sono ancora. Dettagli in [docs/sfide](docs/sfide/sfide.md#comportamento-già-implementato).
- rating Elo (API-46): `Elo.eloDelta` e `Elo.score` (funzioni pure), `PlayerRating` con `recordGame` per la futura partita classificata, `RankingService.createIfMissing` per l'ingresso in coda e `GET /api/v1/rankings/me` (`RankingController`), che senza una riga restituisce 1000 e contatori a 0 senza scrivere. Nessuna classifica (Q-10).
- coda classificata (API-47): `POST/GET/DELETE /api/v1/matchmaking/queue` (`MatchmakingController`, `MatchmakingQueueService`). Si entra con un mio mazzo valido (`getPlayableDeck`), al primo ingresso si crea il rating, un secondo ingresso è `409 ALREADY_IN_QUEUE`; il ticket `{ status, deckId, rating, queuedAt, window }` dà la finestra di ricerca di adesso (`MatchmakingTicket.searchWindow`), senza ticket `GET` è `404 QUEUE_TICKET_NOT_FOUND`, `DELETE` è sempre `204`. `QueueTicketService` toglie il ticket quando il mazzo cambia o l'account viene disattivato. L'abbinamento non c'è ancora (API-49).

Pacchetti (`pack/`, API-18, 19, 20, 22, 24, 63):

- backoffice `GET/POST/PUT /api/v1/admin/packs`, `/activate`, `/deactivate` con slot e bonus nella stessa richiesta; un pacchetto si attiva solo se ogni slot trova carte (anche con il ripiego);
- `GET /api/v1/packs` pubblico (con il token anche `remainingPurchases`), `POST /api/v1/packs/{id}/purchase`, `POST /api/v1/inventory/packs/{packId}/open`; `GET /inventory` elenca i pacchetti da aprire;
- estrazione in `PackDrawer`/`CardPool` senza database, casualità dal bean `RandomGenerator`; `pack_tags` ignorati finché non c'è API-21;
- `V12__insert_base_pack.sql` mette in vendita il *Base Pack*. Dettagli in [docs/pacchetti](docs/pacchetti/pacchetti.md#comportamento-già-implementato) e [docs/backoffice](docs/backoffice/backoffice.md#pacchetti-comportamento-già-implementato-api-18-88-api-19-89-api-20-90).

Catalogo iniziale e dati di prova (API-55, API-25):

- `V11__insert_initial_catalog.sql` carica le 120 carte di `catalogo-finale.csv` (ENG-42); Spell e Hidden sono Rare e non contano nel `max_per_deck`. Immagini segnaposto e testi in italiano finché i DSG non danno quelli definitivi.
- Con il profilo `dev` (attivo da solo con `./gradlew bootRun`) `db/dev/R__dev_seed.sql` aggiunge `admin`, `alice` e `bob` (password di sviluppo `Reversky2026`) con collezioni complete, un mazzo valido ciascuno e l'amicizia tra alice e bob. Dettagli in [docs/database](docs/database/database.md#dati-di-prova-solo-sviluppo).

Mazzi (`deck/`, API-26–29):

- `GET/POST /api/v1/decks`, `GET/PUT/DELETE /api/v1/decks/{id}`, `POST /api/v1/decks/validate`, `GET /api/v1/decks/rules` (`DeckController`); il mazzo di un altro è `404`, nome univoco per utente (`409`), un mazzo in un invito `PENDING` non si elimina (`409`), la modifica toglie il ticket in coda classificata.
- `DeckValidator` senza database restituisce tutte le violazioni `DECK_*` (composizione del rule set attivo, Hidden per sottotipo, copie con varianti sommate, `max_per_deck`, possesso sulle copie disponibili, carte disattivate); un mazzo non valido è `422 DECK_INVALID` con `violations` e non si salva; `valid` si ricalcola a ogni lettura. Gli inviti si controllano con `PendingInviteService`, il ticket in coda classificata si toglie con `QueueTicketService` (API-47). Dettagli in [docs/mazzi](docs/mazzi/mazzi.md#comportamento-già-implementato).

Scambi (`trade/`, API-31–34):

- `GET /api/v1/trades?box=incoming|outgoing` (solo aperti, paginata, ultima mossa prima), `GET /api/v1/trades/{id}` e `POST /api/v1/trades/{id}/reject` (`TradeController`), sempre dal punto di vista di chi guarda (`give`, `receive`, `other`, `myTurn`); un non partecipante riceve `404`.
- `POST /api/v1/trades` e `POST /api/v1/trades/{id}/counter`: le copie offerte restano in collezione ma sono riservate (solo per chi ha fatto l'offerta in corso) finché lo scambio è aperto; chi propone blocca la sua riga `inventories` prima di contare le copie disponibili. Le offerte si aggiornano al loro posto (`Trade.replaceOffers`).
- `POST /api/v1/trades/{id}/accept` in una sola transazione: blocca lo scambio, poi le righe `inventories` di entrambi in ordine di `user_id`, controlla le copie disponibili di chi accetta e sposta le carte con `CollectionService`; l'altro disattivato è `422 TRADE_USER_DISABLED`. Dettagli in [docs/scambi](docs/scambi/scambi.md#comportamento-già-implementato).

Le altre funzionalità non sono ancora iniziate: i documenti in `docs/` descrivono come **devono** essere costruite. Cosa manca e in che ordine è nella [roadmap](docs/roadmap/roadmap.md).

Lo schema confermato è in [schema.dbml](docs/database/schema.dbml). La **baseline** `V1`–`V8` crea tutte le tabelle in una volta (task API-70, #141) e sostituisce le vecchie migrazioni scritte una per task: da qui lo schema è bloccato, gli altri task scrivono solo Java e un cambiamento di struttura richiede una migrazione nuova decisa insieme (vedi [database](docs/database/database.md#schema-bloccato-baseline)). Chi aveva già un database locale deve cancellarne il volume.
