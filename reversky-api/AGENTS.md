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

Manca ancora la parte legata ai crediti: l'inventario alla registrazione e i crediti in `/users/me` (TODO nel codice), che arriveranno con `inventory/`.

Catalogo (`catalog/`):

- fazioni (migrazione `V3`): `GET /api/v1/factions` pubblico e `GET/POST/PUT /api/v1/admin/factions` (`admin/controller/AdminFactionController`);
- rarità (migrazione `V4`): `GET /api/v1/rarities` pubblico, ordinato per `sorting_order`, e `GET/POST/PUT /api/v1/admin/rarities` (`admin/controller/AdminRarityController`).

Le altre funzionalità non sono ancora iniziate: i documenti in `docs/` descrivono come **devono** essere costruite.

Lo schema confermato è in [schema.dbml](docs/database/schema.dbml); `V1`–`V4` sono allineate. Da qui in poi vale la regola 6: lo schema cambia solo con migrazioni nuove.
