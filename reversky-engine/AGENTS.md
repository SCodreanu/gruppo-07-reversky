# AGENTS.md — reversky-engine

Istruzioni per gli agenti AI (Claude Code, Codex, Copilot, ecc.) e per chiunque lavori su questo modulo.
Leggi questo file **prima** di modificare il codice.

## Cos'è questo modulo

`reversky-engine` è il **motore di gioco** di Reversky (Downfall, gruppo 07).
Fa una cosa sola: **dati due mazzi, i bonus di fazione e un seed, simula una partita completa e restituisce il risultato round per round.**

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
| Database | **nessuno** |

## Comandi

```bash
./gradlew bootRun    # avvia l'engine su http://localhost:8081 (Windows: gradlew.bat bootRun)
./gradlew test       # tutti i test (non serve Docker)
./gradlew build      # compilazione + test
```

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
| Fazioni e bonus (da `faction_bonus`) | [docs/fazioni](docs/fazioni/fazioni.md) |
| Tipi di carta (Creature, Spell, Hidden, Terreni) | [docs/carte](docs/carte/carte.md) |
| Sistema degli effetti speciali | [docs/effetti](docs/effetti/effetti.md) |
| Mescolamento, seed, riproducibilità | [docs/mescolamento](docs/mescolamento/mescolamento.md) |
| Log della partita | [docs/log-partita](docs/log-partita/log-partita.md) |
| Contratto REST con l'API | [docs/api-engine](docs/api-engine/api-engine.md) |
| Strategia di test | [docs/test](docs/test/test.md) |

Le regole di scrittura del codice sono in [CodingRequirements.md](CodingRequirements.md).

## Regole che non si discutono

1. **Il package `core` non importa niente di Spring** (né `org.springframework.*`, né Lombok). Deve poter girare in un semplice `main` o in un test JUnit.
2. **Determinismo:** stessi mazzi + stessi bonus di fazione + stesso seed = **stessa identica partita**. Vietati `Math.random()`, `new Random()` senza seed, orari di sistema, iterazione su `HashMap`/`HashSet` quando l'ordine conta.
3. **Un solo regolamento.** Abilità, Spell, Hidden e Terreni fanno parte del gioco standard: non esistono modalità alternative (decisione concordata con il cliente). Non introdurre flag o `if` per "disattivare" parti del regolamento.
4. **Ogni regola ha un test.** Ogni effetto speciale ha un test. Un bug corretto ha un test che lo riproduce.
5. **Nessun numero magico:** punti vita e limite di round stanno in `RuleConfig`; fazioni e bonus di fazione arrivano nella richiesta (sono dati del database dell'API).
6. **Il motore non valida i mazzi secondo il regolamento** (40 carte, copie…): lo fa l'API. L'engine controlla solo che i dati ricevuti siano coerenti.
7. I punti segnati **[DA CONFERMARE]** nei documenti non sono decisioni definitive: chiedi prima di cambiarli.

## Come lavorare

1. Leggi il documento dell'argomento e `CodingRequirements.md`.
2. Scrivi (o aggiorna) prima il test della regola, poi il codice.
3. Esegui `./gradlew test`.
4. Se cambi una regola, **aggiorna il documento in `docs/`** e il regolamento per i giocatori (lato DSG) nello stesso giro di lavoro.

## Stato attuale

Il progetto è lo scheletro generato da Spring Initializr: esiste solo `ReverskyEngineApplication`. I documenti in `docs/` descrivono come **deve** essere costruito il motore.
