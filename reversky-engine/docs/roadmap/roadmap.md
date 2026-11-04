# Roadmap del motore di gioco

Piano di lavoro di `reversky-engine` per **due sviluppatori**, dal 4 novembre alla presentazione finale del **10 dicembre 2026**.
Segue le fasi e le milestone della **roadmap presentata al cliente** il 2 novembre (proposta al cliente, "Roadmap di sviluppo"),
che coincidono con il calendario del laboratorio.

| Chi | Ruolo | Possiede |
|---|---|---|
| **Dev A** — _______ | Regole e simulazione | `core.model`, `core.rules`, `core.simulation`; effetti della Theory of the Invitation e della Theory of the Archive |
| **Dev B** — _______ | Contratto, log e qualità | `web`, `core.log`, classi di test in `support/`, `examples/`; effetti della Theory of the Wound e della Theory of the Dream |
| Team leader SWD | Revisione e integrazione | approva le PR, `build.gradle`, `EngineClient` nell'API, decisioni con i DSG |

Ognuno lavora nei **propri package**: così le Pull Request non vanno in conflitto. I punti di contatto (modello e interfaccia degli effetti) si decidono insieme all'inizio (`ENG-02`, `ENG-22`).

## Fasi e milestone (dalla proposta al cliente)

| Fase | Date | Milestone | Cosa deve funzionare nell'engine |
|---|---|---|---|
| Analisi e progettazione | 19 ott – 2 nov | primo incontro | documenti in `docs/` ✔ |
| Architettura e prototipi | 3 – 16 nov | **Regole approvate** (16 nov) | [decisioni](../decisioni-aperte/decisioni-aperte.md) `Alta` chiuse; regole base con i test verdi nel `core` |
| Sviluppo funzionalità | 17 – 23 nov | **SAL** (lun 23 nov) | "prima sfida simulata dal motore": una partita via REST, riproducibile, esempi verdi |
| Integrazione e bilanciamento | 24 – 30 nov | **Versione completa** (30 nov) | carte speciali, effetti `P1`, engine usato dall'API, primo report di bilanciamento |
| Test e rifinitura | 1 – 9 dic | **Collaudo superato** (9 dic) | effetti `P2` decisi, casi limite, Javadoc, documenti; code freeze domenica 6 dic |
| Presentazione finale | gio 10 dic | **Consegna** | demo, documentazione, archivio ZIP |

Lezioni in classe: lunedì 9, 16, 23 (SAL) e 30 novembre. Martedì 8 dicembre è festivo (Immacolata).

## Priorità

| Livello | Cosa | Se manca il tempo |
|---|---|---|
| **P0** — obbligatorio dal brief | regole base, fazioni, mescolamento lato server, log round per round, REST, Swagger, test JUnit | non si taglia |
| **P1** — meccaniche aggiunte da Downfall | Spell, Hidden, Terreni (bonus base), effetti con testo chiaro | si taglia dal fondo della lista |
| **P2** — dopo le decisioni | effetti ambigui, effetti secondari dei Terreni, bilanciamento fine | si rimanda |

Un effetto non implementato **non rompe nulla**: non è in `GET /api/v1/effects`, quindi il backoffice non lo può assegnare a una carta.
Le meccaniche `P1` (Spell, Hidden, Terreni) sono state **approvate dal cliente** il 2 novembre (D-21).

## Architettura e prototipi — 4–16 novembre → Regole approvate

### Settimana 0 (4–8 novembre): preparazione

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-01` | Ambiente pronto: JDK 25, `gradlew test` verde, engine su 8081 ([guida, §1](../guida-sviluppo/guida-sviluppo.md#1-primo-giorno-controlli-da-fare)) | A + B | S | — |
| `ENG-02` | Modello condiviso: `CardDefinition` (con `secondaryFactionId`), `SimulationInput`, `PlayerInput`, `MatchResult`, `PlayerResult`, `MatchEvent` e gli enum `CardType`, `CardSubtype`, `Side`, `BattleResult`, `EndReason`, `EventType` (solo record ed enum, senza logica) | A + B in coppia | S | `ENG-01` |
| `ENG-03` | Leggere le [decisioni aperte](../decisioni-aperte/decisioni-aperte.md) e portare le `Alta` ai DSG il 9 novembre | lead + A | S | — |

`ENG-02` va fatto **insieme, in una sola PR**, prima di tutto il resto: tutte le altre issue usano questi tipi.

### Settimana 1 (9–16 novembre): regole base nel core

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-04` | `RuleConfig` (costruita dalla richiesta, senza valori predefiniti) e `FactionMatrix` con i test: malus −1 (D-04) e carte con due fazioni (D-22) (esempio di test nella [guida, §5](../guida-sviluppo/guida-sviluppo.md#5-scrivere-un-test-dal-regolamento-al-codice)) | A | S | `ENG-02` |
| `ENG-05` | `CreatureInPlay`, `PlayerState`, `MatchState`, `StatModifier` con origine (D-35) | A | M | `ENG-02` |
| `ENG-06` | `RoundResolver`: attacco simultaneo, danni, morti, danno al giocatore | A | M | `ENG-04`, `ENG-05` |
| `ENG-07` | `EndConditionChecker`: le 5 condizioni nell'ordine di [regole-base](../regole-base/regole-base.md#fine-della-partita) | A | M | `ENG-05` |
| `ENG-08` | `MatchLog` con i metodi per ogni tipo di evento e i messaggi in inglese (li leggono i giocatori) | B | S | `ENG-02` |
| `ENG-09` | Classi di supporto ai test: `TestRules` (rule set di prova), `TestCards`, `TestFactions` (`WOUND`, `INVITATION`, `ARCHIVE`, `DREAM`), `TestDecks`, `TestInputs`, `TestStates` | B | S | `ENG-02` |
| `ENG-10` | Validazione dell'input nel core: `InvalidSimulationInputException` con i codici di [api-engine](../api-engine/api-engine.md#errori) | B | S | `ENG-02` |

**Regole approvate (16 novembre):** decisioni `Alta` chiuse e tutti i punti "Regolamento base" e "Fazioni" della checklist di [test](../test/test.md) coperti da test verdi.

## Sviluppo funzionalità — 17–23 novembre → SAL

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-11` | `MatchSimulator`: mescolamento con il seed, pescata delle creature, ciclo dei round, test di determinismo | A | M | `ENG-06`, `ENG-07`, `ENG-08` |
| `ENG-12` | DTO, `SimulationMapper`, `SimulationController` con `POST /api/v1/simulations` | B | M | `ENG-02`, `ENG-10` |
| `ENG-13` | `GlobalExceptionHandler` con `ProblemDetail` e `code` | B | S | `ENG-12` |
| `ENG-14` | `GET /api/v1/effects` (per ora lista vuota) | B | S | `ENG-12` |
| `ENG-15` | Swagger con springdoc 3.2.0, la stessa versione dell'API (D-41, serve l'ok del lead) | B | S | `ENG-12` |
| `ENG-16` | `SimulationExamplesTest`: gli esempi 01–08, 10–12 e 90–95 di [`examples/`](../../examples/README.md) verdi | B | S | `ENG-11`, `ENG-12` |
| `ENG-17` | Prova con Docker: engine nel `compose.yaml`, chiamata dall'API o da curl | B | S | `ENG-12` |
| `ENG-18` | Demo per il SAL: esempio 08 via Swagger o curl, log mostrato round per round | A + B | S | `ENG-16` |
| `ENG-22` | Framework degli effetti: `CreatureEffect` con `onCombatStart` (D-31), `EffectContext`, `EffectRegistry`, priorità, controllo delle morti dopo ogni fase (D-08). B scrive registro e contesto, A chiama i momenti dal `RoundResolver`. **Si inizia dopo `ENG-11`, si chiude entro mercoledì 25** | A + B | M | `ENG-11` |

**SAL (23 novembre):** `gradlew build` verde, una partita si simula via REST con risultato riproducibile.
È anche il momento in cui l'API può iniziare a usare l'engine vero al posto del mock.

## Integrazione e bilanciamento — 24–30 novembre → Versione completa

È la settimana più carica: per questo `ENG-22` parte già prima del SAL.

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-19` | `DrawService`: Spell `DAMAGE`, `SHIELD`, `BURN` con i modificatori in attesa (D-10…D-13, D-34) | A | L | `ENG-11` |
| `ENG-20` | Zona Hidden e attivazione (D-15) | A | M | `ENG-19` |
| `ENG-21` | Terreni: bonus base (D-17) | A | S | `ENG-19` |
| `ENG-23` | Primi effetti `P1`, uno per tipo di momento: Hidebehind (esempio 09), Nurikabe, Kappa, SCP-096, SCP-2317, Smiler | B | M | `ENG-22` |
| `ENG-24` | Test di contratto con l'`EngineClient` dell'API: stesso JSON di `examples/` | lead + B | S | `ENG-16` |
| `ENG-25` | Effetti Theory of the Invitation e Theory of the Archive ([catalogo](../effetti/catalogo-effetti.md)): i `P1` entro il 30 | A | L | `ENG-22` |
| `ENG-26` | Effetti Theory of the Wound e Theory of the Dream: i `P1` entro il 30 | B | L | `ENG-22` |
| `ENG-28` | Task `balanceTest` e `BalanceSimulationTest`: primo report per i DSG (vittorie per fazione, partite a `ROUND_LIMIT`, durata media) | B | M | `ENG-11` |

**Versione completa (30 novembre):** carte speciali ed effetti `P1` nel registro, report di bilanciamento consegnato ai DSG.

## Test e rifinitura — 1–9 dicembre → Collaudo superato

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-25` / `ENG-26` | Effetti `P2` **solo se decisi entro il 30 novembre** | A / B | M | D-xx |
| `ENG-27` | Effetti secondari dei Terreni, se decisi (D-18) | A | M | `ENG-21` |
| `ENG-28` | Report di bilanciamento con gli effetti, dopo le modifiche dei DSG alle statistiche | B | S | `ENG-26` |
| `ENG-29` | Casi limite e correzione bug; Javadoc su tutto il core pubblico | A + B | M | — |
| `ENG-30` | rule set `1.0` caricato nel database dell'API con i valori decisi; documenti aggiornati (tabella effetti, regole-base, decisioni chiuse) | A + B | S | — |
| `ENG-31` | README dell'engine completo, screenshot di Swagger, numero di test e cosa coprono, per la presentazione | B | S | — |
| `ENG-32` | Partita dimostrativa per la presentazione (mazzi di prova o carte del catalogo, se pronte; seed scelto) e prova generale | A | S | — |

**Code freeze domenica 6 dicembre.** Dopo, solo bug con un test che li riproduce. **Collaudo superato (9 dicembre):** `gradlew build` verde, nessun bug bloccante.

## Come si lavora

- Una issue su GitHub Projects per ogni riga, con l'ID `ENG-xx` nel titolo e la colonna *Dim.* come etichetta (`S` ≤ mezza giornata, `M` 1–2 giorni, `L` 3+ giorni). La stessa board di DSG e WMD: ogni attività ha un responsabile e uno stato.
- Branch `feature/<n-issue>-descrizione`, PR revisionata **dall'altro sviluppatore** del motore, `Closes #N`.
- **Fatto** = test scritti prima del codice e verdi, `gradlew build` verde, Javadoc, documento in `docs/` aggiornato (checklist in [CodingRequirements](../../CodingRequirements.md#13-checklist-prima-della-pull-request)).
- Ogni lunedì in classe: 5 minuti di demo di quello che è stato chiuso e controllo di questa tabella.
- Una decisione che manca **non ferma il lavoro**: si implementa la proposta di [decisioni aperte](../decisioni-aperte/decisioni-aperte.md) con il commento `// [DA CONFERMARE] D-xx`.

## Rischi

| Rischio | Effetto | Cosa fare |
|---|---|---|
| Le decisioni dei DSG arrivano dopo il 16 novembre | carte speciali ed effetti fermi | si usano le proposte; tutto ciò che è `[DA CONFERMARE]` si cerca con `grep` e si cambia in un punto solo |
| Il framework degli effetti nasce sbagliato | gli effetti vanno riscritti | `ENG-22` si fa in coppia e si prova subito con 3 effetti diversi (Hidebehind, SCP-096, SCP-2317) prima di fare gli altri |
| Troppi effetti per il tempo rimasto (oltre 35 creature con effetto, contando le nuove) | effetti incompleti alla Versione completa | si chiudono per lotti `P1` → `P2`; un effetto non registrato non rompe la partita |
| Contratto REST cambiato senza avvisare | l'API smette di funzionare | ogni modifica a `api-engine.md` va segnalata al lead nella PR; `ENG-24` lo verifica |
| Un solo sviluppatore conosce un pezzo | blocco se è assente | revisione incrociata obbligatoria; ognuno fa almeno un effetto delle fazioni dell'altro se resta tempo |
