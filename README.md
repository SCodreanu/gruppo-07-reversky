# Progetto `Reversky`

## Corso WMD

- *Codreanu Stefan Ioan* (Team Leader) @SCodreanu
- *Galiano Lorenzo* @lorenzogaliano-gif
- *Dall'Aglio Luca* @locadallaglio-dot

## Corso DSG

- *Gastaldo Francesco* @francescogastaldo-its
- *Librè Elisa* @elisa-libre
- *Maffeo Alessia* @alessiamaffeo

## Corso SWD

- *Fatticcioni Tommaso* @tommasofatticcioni
- *Cistaro Alessandro* @acistar0
- *Consolato Antonino* @antoninoconsolato-its

---

## Documentazione di progetto

Tutti i documenti del gruppo (presi dal Drive) sono nella cartella [`docs/`](docs).

### Brief e organizzazione

| Documento | Contenuto |
|---|---|
| [Brief del laboratorio](<docs/infos/laboratorio-integrato-brief.pdf>) | richiesta del cliente: requisiti del gioco, del portale, tecnici ed economici |
| [Calendario](<docs/infos/calendario.pdf>) | date, ore e tipo delle lezioni del laboratorio |
| [Gruppi del laboratorio](<docs/infos/Gruppi Laboratorio Integrato.xlsx>) | composizione di tutti i gruppi |

### Progetto Reversky

| Documento | Contenuto |
|---|---|
| [Documento di lavoro (idee)](<docs/idee.docx>) | materiale per il primo incontro con il cliente (2 novembre): chi siamo, recap del brief, proposta tecnica, lore, carte, regolamento, roadmap |
| [Ricerca di mercato, insight e strategia social](<docs/Reversky — Ricerca di mercato, insight e strategia social.pdf>) | analisi di mercato, target e strategia sui social |
| [Report del questionario](<docs/Report_questionario_Reversky.pdf>) | risultati del questionario agli utenti (ottobre 2026) e cosa implicano per il concept |
| [Competitor horror](<docs/Competitor horror di Reversky — tre riferimenti consolidati e tre titoli recenti.docx>) | analisi di sei giochi concorrenti: tre riferimenti consolidati e tre titoli recenti |

### Grafica

| Cartella | Contenuto |
|---|---|
| [`docs/assets/logo`](<docs/assets/logo>) | logo di Reversky nelle varie versioni (PNG e SVG) |
| [`docs/assets/fazioni`](<docs/assets/fazioni>) | simboli delle quattro fazioni (Theory of the Wound, of the Invitation, of the Archive, of the Dream) |
| [`docs/assets/carte`](<docs/assets/carte>) | esempi di carte, una per rarità, più una carta speciale |

Esempi di carte (illustrazioni precedenti al catalogo di 120 carte: nel catalogo Kitsune è Legendary, mentre Ashen Kitsune e The Last Gift non ci sono):

| Rarità | Carta |
|---|---|
| Common | [Ash Raven](<docs/assets/carte/Common-Corvo_di_Cenere.png>) |
| Uncommon | [Living Waiting Room](<docs/assets/carte/Uncommon-Sala_d’Attesa_Vivente.png>) |
| Rare | [SCP-106 (Old Man)](<docs/assets/carte/Rare-SCP-106.png>) |
| Mythic | [Kitsune](<docs/assets/carte/Mythic-Kitsune.png>) |
| Legendary | [Wendigo](<docs/assets/carte/Legendary-Wendingo.png>) |
| Mutant | [Ashen Kitsune](<docs/assets/carte/Mutante-Ashen_Kitsune.png>) |
| Speciale | [The Last Gift](<docs/assets/carte/CartaSpecialeNatale.png>) |

---

## Come avviare il backend

Il backend è composto da quattro servizi, avviati insieme con Docker Compose:

| Servizio | Cosa fa | Indirizzo |
|---|---|---|
| `db` | database PostgreSQL 18 | `localhost:5432` |
| `mailpit` | finto server email: cattura le email dell'API senza spedirle | `http://localhost:8025` |
| `engine` | motore di gioco (`reversky-engine`) | solo interno, `http://engine:8081` |
| `api` | backend REST (`reversky-api`) | `http://localhost:8080` |

### Requisiti

Servono **Docker Desktop** (sempre) e **Java 25 — Eclipse Temurin** (solo per avviare i moduli senza Docker).
Come installarli e verificarli è spiegato passo per passo in [INSTALLAZIONE.md](INSTALLAZIONE.md).

### Avvio rapido

Dalla cartella principale del progetto:

```bash
# 1. (facoltativo) crea il file con le tue impostazioni
cp .env.example .env          # su Windows: copy .env.example .env

# 2. compila e avvia tutto
docker compose up --build
```

La prima volta ci vuole qualche minuto (scarica le immagini e le dipendenze Gradle). L'API è pronta quando nei log compare `Started ReverskyApiApplication`.

Senza file `.env` vengono usati i valori di sviluppo di `.env.example`. Il file `.env` **non va committato** (è già nel `.gitignore`).

### Script per Windows

Nella cartella [`scripts/`](scripts) ci sono tre script da lanciare con doppio clic o dal terminale (funzionano da qualsiasi cartella):

| Script | Cosa avvia | Serve Docker |
|---|---|---|
| [`start-backend.bat`](scripts/start-backend.bat) | tutto il backend con Docker Compose (database, engine, API); aggiungi `-d` per avviarlo in background | sì |
| [`start-api.bat`](scripts/start-api.bat) | solo l'API con Gradle, su `http://localhost:8080` (il database parte da solo) | sì |
| [`start-engine.bat`](scripts/start-engine.bat) | solo l'engine con Gradle, su `http://localhost:8081` | no |

Per sviluppare dall'IDE di solito si usano `start-engine.bat` e `start-api.bat` insieme, in due terminali. Non avviare `start-backend.bat` nello stesso momento: userebbe le stesse porte.

### Comandi utili

| Comando | A cosa serve |
|---|---|
| `docker compose up --build -d` | avvia in background |
| `docker compose logs -f api` | segue i log di un servizio (`api`, `engine`, `db`) |
| `docker compose ps` | mostra lo stato dei servizi |
| `docker compose down` | ferma tutto (i dati del database restano) |
| `docker compose down -v` | ferma tutto e **cancella il database** |
| `docker compose up --build api` | ricompila e riavvia solo l'API dopo una modifica |

### Provare l'API con Swagger

Con l'API avviata, la documentazione interattiva è su `http://localhost:8080/swagger-ui.html`. Per gli endpoint protetti:

1. chiama `POST /api/v1/auth/login` e copia `accessToken` dalla risposta;
2. premi **Authorize** in alto e incolla solo il token (senza `Bearer `);
3. gli endpoint con il lucchetto ora ricevono il token.

### Email in sviluppo

Le email dell'API (verifica dell'indirizzo, reset della password) non partono davvero: le riceve **Mailpit**, e si leggono su `http://localhost:8025`. I link nelle email puntano al frontend (`http://localhost:3000/verify-email?token=...` e `/reset-password?token=...`); senza frontend avviato, copia il `token` dal link e usalo in Swagger con `POST /api/v1/auth/email/verify` o `POST /api/v1/auth/password/reset`.

### Collegarsi al database

Da DBeaver, pgAdmin o IntelliJ:

| Campo | Valore |
|---|---|
| Host | `localhost` |
| Porta | `5432` |
| Database | `reversky` |
| Utente / Password | `reversky` / `reversky` |

(oppure i valori che hai messo nel `.env`)

### Avvio senza Docker (sviluppo)

Utile per il debug dall'IDE. Avvia ogni modulo dalla sua cartella:

```bash
# Engine → http://localhost:8081
cd reversky-engine
./gradlew bootRun             # su Windows: gradlew.bat bootRun

# API → http://localhost:8080 (in un altro terminale)
cd reversky-api
./gradlew bootRun
```

L'API avvia da sola PostgreSQL e Mailpit in Docker usando `reversky-api/compose.yaml`, quindi Docker Desktop deve essere acceso. Se è già in esecuzione il compose principale, fermalo prima (`docker compose down`) per non avere due database e due Mailpit sulle stesse porte.

### Test

```bash
cd reversky-engine && ./gradlew test     # test del motore, non serve Docker
cd reversky-api && ./gradlew test        # test dell'API, serve Docker (Testcontainers)
```

### Problemi comuni

| Problema | Soluzione |
|---|---|
| `port is already allocated` su 5432 o 8080 | un altro programma usa la porta: chiudilo oppure cambia `POSTGRES_PORT` / `API_PORT` nel `.env` |
| `./gradlew: Permission denied` | `chmod +x gradlew`, oppure su Windows usa `gradlew.bat` |
| L'API si avvia prima del database e fallisce | non dovrebbe succedere (aspetta il controllo di salute del db); riprova con `docker compose up` |
| Dati vecchi o migrazioni incoerenti | `docker compose down -v` e riavvia: il database viene ricreato da zero |

### Documentazione dei moduli

Ogni modulo ha la propria documentazione tecnica:

- `reversky-api/AGENTS.md` e `reversky-api/docs/`: funzionalità del backend, database, regole del codice;
- `reversky-engine/AGENTS.md` e `reversky-engine/docs/`: regole di gioco, effetti, contratto REST del motore.
  Per chi inizia sull'engine: [guida allo sviluppo](reversky-engine/docs/guida-sviluppo/guida-sviluppo.md), [roadmap](reversky-engine/docs/roadmap/roadmap.md), [richieste di esempio](reversky-engine/examples/README.md).
