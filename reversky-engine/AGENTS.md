# AGENTS.md — reversky-engine

Istruzioni per gli agenti AI (Claude Code, Codex, Copilot, ecc.) e per chiunque lavori su questo modulo.
Leggi questo file **prima** di modificare il codice.

## Cos'è questo modulo

`reversky-engine` è il **motore di gioco** di Reversky (Downfall, gruppo 07).
Fa una cosa sola: **dati due mazzi, le regole della partita, i bonus di fazione e un seed, simula una partita completa e restituisce il risultato round per round.**

- Non ha database e non sa nulla di utenti, crediti, scambi.
- Riceve i dati da `reversky-api` via HTTP e risponde in JSON.
- Il giocatore non prende decisioni durante la partita: tutto è automatico.

Il brief richiede che il motore sia **un componente Java isolato e coperto da test JUnit**. È la parte più testata del progetto.

## Stack

| Cosa | Versione / Libreria |
|---|---|
| Linguaggio | Java 25 |
| Framework | Spring Boot 4.1.2 (solo Web MVC + Validation, solo nello strato `web`) |
| Build | Gradle (wrapper incluso) |
| Test | JUnit 5, AssertJ |
| Utility | Lombok (solo nello strato `web`) |
| Documentazione REST | springdoc-openapi 3.2.0, Swagger UI su `/swagger-ui.html` (solo nello strato `web`) |
| Database | **nessuno** |

## Comandi

```bash
./gradlew bootRun    # avvia l'engine su http://localhost:8081 (Windows: gradlew.bat bootRun)
./gradlew test       # tutti i test (non serve Docker)
./gradlew build      # compilazione + test
./gradlew balanceTest  # simulazione di bilanciamento (lenta, esclusa da build): report in docs/bilanciamento
./gradlew effectTest   # valore di ogni effetto a partite appaiate (lento, escluso da build): report-effetti.md
```

Altri comandi (un solo test, report, curl, Docker) e problemi comuni: [docs/guida-sviluppo](docs/guida-sviluppo/guida-sviluppo.md).

## Struttura del codice

```
src/main/java/it/downfall/reversky/engine/
├── core/                 ← Java puro, ZERO dipendenze da Spring
│   ├── model/            carte, creature in gioco, stato dei giocatori
│   ├── rules/            RuleConfig, FactionMatrix (costruita dai dati ricevuti)
│   ├── simulation/       MatchSimulator, risoluzione dei round, fine partita
│   ├── effect/           interfaccia effetti, registro, implementazioni
│   └── log/              eventi del log di partita
└── web/                  ← Spring: controller REST, DTO, conversione DTO ↔ core
```

## Documentazione per argomento

| Argomento | File |
|---|---|
| Architettura e isolamento del core | [docs/architettura](docs/architettura/architettura.md) |
| Regole base: round, danni, fine partita | [docs/regole-base](docs/regole-base/regole-base.md) |
| Fazioni e bonus (da `faction_bonuses`) | [docs/fazioni](docs/fazioni/fazioni.md) |
| Tipi di carta (Creature, Spell, Hidden, Terreni, Oggetti) | [docs/carte](docs/carte/carte.md) |
| Sistema degli effetti speciali | [docs/effetti](docs/effetti/effetti.md) |
| Mescolamento, seed, riproducibilità | [docs/mescolamento](docs/mescolamento/mescolamento.md) |
| Log della partita | [docs/log-partita](docs/log-partita/log-partita.md) |
| Contratto REST con l'API | [docs/api-engine](docs/api-engine/api-engine.md) |
| Strategia di test | [docs/test](docs/test/test.md) |
| Catalogo di tutti gli effetti delle carte | [docs/effetti/catalogo-effetti](docs/effetti/catalogo-effetti.md) |
| Decisioni aperte e incoerenze da risolvere | [docs/decisioni-aperte](docs/decisioni-aperte/decisioni-aperte.md) |
| Roadmap e divisione del lavoro (issue `ENG-xx`) | [docs/roadmap](docs/roadmap/roadmap.md) |
| Avviare, chiamare e testare l'engine | [docs/guida-sviluppo](docs/guida-sviluppo/guida-sviluppo.md) |
| Richieste di esempio con il risultato atteso | [examples/](examples/README.md) |
| Demo per il SAL del 23 novembre | [docs/demo-sal](docs/demo-sal/demo-sal.md) |

Le regole di scrittura del codice sono in [CodingRequirements.md](CodingRequirements.md).

## Regole che non si discutono

1. **Il package `core` non importa niente di Spring** (né `org.springframework.*`, né Lombok). Deve poter girare in un semplice `main` o in un test JUnit.
2. **Determinismo:** stessi mazzi + stessi bonus di fazione + stesso seed = **stessa identica partita**. Vietati `Math.random()`, `new Random()` senza seed, orari di sistema, iterazione su `HashMap`/`HashSet` quando l'ordine conta.
3. **Un solo regolamento.** Abilità, Spell, Hidden e Terreni fanno parte del gioco standard: non esistono modalità alternative (decisione concordata con il cliente). Non introdurre flag o `if` per "disattivare" parti del regolamento.
4. **Ogni regola ha un test.** Ogni effetto speciale ha un test. Un bug corretto ha un test che lo riproduce.
5. **Nessun numero magico e nessun valore predefinito:** punti vita, limite di round, fazioni e bonus di fazione arrivano **nella richiesta**: sono dati del database dell'API, modificabili dal backoffice. Nel codice restano solo le regole che non sono numeri da bilanciare.
6. **Il motore non valida i mazzi secondo il regolamento** (numero di carte, copie…): lo fa l'API. L'engine controlla solo che i dati ricevuti siano coerenti.
7. I punti segnati **[DA CONFERMARE]** nei documenti non sono decisioni definitive: chiedi prima di cambiarli. L'elenco completo, con le proposte, è in [docs/decisioni-aperte](docs/decisioni-aperte/decisioni-aperte.md).

## Come lavorare

1. Leggi il documento dell'argomento e `CodingRequirements.md`.
2. Scrivi (o aggiorna) prima il test della regola, poi il codice.
3. Esegui `./gradlew test`.
4. Se cambi una regola, **aggiorna il documento in `docs/`** e il regolamento per i giocatori (lato DSG) nello stesso giro di lavoro.

## Stato attuale

Esistono il modello del core, la validazione dell'input, i bonus di fazione, lo stato della partita, il log, la risoluzione dei round, il controllo delle morti dopo ogni fase (D-08), le condizioni di fine partita e il `MatchSimulator`, esposto da `POST /api/v1/simulations` nello strato `web`; `GET /api/v1/effects` elenca gli effetti registrati.
Esiste il registro degli effetti (`EffectRegistry`), con le interfacce degli effetti di creature, Spell, Hidden, Terreni e Oggetti: una carta con un codice non registrato dà `400 UNKNOWN_EFFECT` (una creatura può non avere codice).
Sono implementate le Spell `P1`, le Hidden `P1`, i Terreni con il bonus base e gli effetti secondari (`ENG-27`) e i quattro Oggetti (`DrawService`, modificatori in attesa, zona Hidden, `TerrainService`, `ItemService`); i danni che non sono attacchi passano da `Penalties`, dove il Terreno in gioco li può ridurre. Le creature hanno i momenti del round (`CreatureEffect`, `CreatureEffectService`), con le ferite (`onWounded`) e le soglie della DEF (`onDefenseCheck`) controllate dopo ogni fase, e tutti gli effetti `P1`: Theory of the Invitation e Theory of the Archive (`ENG-23`, `ENG-25`), Theory of the Wound, Theory of the Dream e le due Mutant (`ENG-26`). Degli effetti `P2` ci sono la guarigione (`CreatureInPlay.heal`), il momento `onKill`, gli effetti collegati e The Bloodless Hound, Wendigo e SCP-109 (`ENG-34`), il sigillo e la rimozione dei bonus con L'Addetto alle Uscite, Deathmoths, SCP-3999 e Static Interference (`ENG-35`), la rimozione del Terreno con The Null Witness e The Hollow Wyrm (`ENG-36`), la sopravvivenza al danno letale e le Hidden con condizione con The Undying Blossom, Remnants of Reality e Unseen Exit (`ENG-37`), gli effetti annullati, divorati, rubati e copiati e il filtro sul bersaglio con Il Predicatore sotto la Pioggia, SCP-2521, Baku, Kitsune e Il Custode delle Porte Incrociate (`ENG-38`), il mazzo e le Hidden dell'avversario con La Regina dei Sentieri Perduti e L'Operatore della Stanza 0 (`ENG-39`). Con la Spell SCP-500 e la protezione dal primo danno (`ENG-40`) tutti gli effetti del catalogo sono registrati. Ogni giocatore ha una sola Hidden attiva alla volta, le altre aspettano in fila (`ENG-41`, D-15). Il task `balanceTest` (`ENG-28`) gioca 160.000 partite tra mazzi casuali del catalogo finale e scrive il report di bilanciamento in `docs/bilanciamento/` ([docs/test](docs/test/test.md#test-di-bilanciamento-eng-28)). Con `ENG-42` (in corso) la fase 1 ha ritarato ATK e DEF delle creature con la griglia `pv30` e la fase 2 ha misurato ogni effetto con `effectTest` (partite appaiate) e corretto due Terreni e sette creature: è una proposta per i DSG, insieme al bonus +1 ATK contro +1 DEF ([docs/test](docs/test/test.md#ribilanciamento-delle-statistiche-eng-42)). I documenti in `docs/` descrivono anche le parti che mancano.

L'ordine di lavoro è nella [roadmap](docs/roadmap/roadmap.md); le richieste di prova con il risultato calcolato a mano sono in [examples/](examples/README.md).
