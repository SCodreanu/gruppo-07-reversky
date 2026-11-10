# Roadmap del motore di gioco

Piano di lavoro di `reversky-engine` per **due sviluppatori**, dal 4 novembre alla presentazione finale del **10 dicembre 2026**.
Le fasi e le milestone sono quelle della **roadmap presentata al cliente** il 2 novembre (proposta al cliente, "Roadmap di sviluppo"),
che coincidono con il calendario del laboratorio. Dentro le fasi, il lavoro segue gli sprint della board del progetto.

| Chi | Ruolo | Possiede |
|---|---|---|
| **Dev A** — _______ | Regole e simulazione | `core.model`, `core.rules`, `core.simulation`; effetti della Theory of the Invitation e della Theory of the Archive |
| **Dev B** — _______ | Contratto, log e qualità | `web`, `core.log`, classi di test in `support/`, `examples/`; effetti della Theory of the Wound e della Theory of the Dream |
| Team leader SWD | Revisione e integrazione | approva le PR, `build.gradle`, `EngineClient` nell'API, decisioni con i DSG |

Ognuno lavora nei **propri package**: così le Pull Request non vanno in conflitto. I punti di contatto (modello e interfaccia degli effetti) si decidono insieme all'inizio (`ENG-02`, `ENG-22`, `ENG-23`).

## Fasi e milestone (dalla proposta al cliente)

| Fase | Date | Milestone | Cosa deve funzionare nell'engine |
|---|---|---|---|
| Analisi e progettazione | 19 ott – 2 nov | primo incontro | documenti in `docs/` ✔ |
| Architettura e prototipi | 3 – 16 nov | **Regole approvate** (16 nov) | [decisioni](../decisioni-aperte/decisioni-aperte.md) `Alta` chiuse; regole base e simulazione via REST con i test verdi |
| Sviluppo funzionalità | 17 – 23 nov | **SAL** (lun 23 nov) | "prima sfida simulata dal motore": una partita riproducibile, con Spell, Hidden e Terreni, avviata dall'API; esempi verdi |
| Integrazione e bilanciamento | 24 – 30 nov | **Versione completa** (30 nov) | effetti `P1` delle creature, primo report di bilanciamento |
| Test e rifinitura | 1 – 9 dic | **Collaudo superato** (9 dic) | effetti `P2` decisi, casi limite, Javadoc, documenti; code freeze domenica 6 dic |
| Presentazione finale | gio 10 dic | **Consegna** | demo, documentazione, archivio ZIP |

Lezioni in classe: lunedì 9, 16, 23 (SAL) e 30 novembre. Martedì 8 dicembre è festivo (Immacolata).

## Priorità

| Livello | Cosa | Se manca il tempo |
|---|---|---|
| **P0** — obbligatorio dal brief | regole base, fazioni, mescolamento lato server, log round per round, REST, Swagger, test JUnit | non si taglia |
| **P1** — meccaniche aggiunte da Downfall | Spell, Hidden, Terreni (bonus base), effetti con testo chiaro | le meccaniche di Spell, Hidden e Terreni non si tagliano; gli effetti si tagliano dal fondo della lista |
| **P2** — dopo le decisioni | effetti ambigui, effetti secondari dei Terreni, bilanciamento fine | si rimanda |

Un effetto non implementato **non rompe nulla**: non è in `GET /api/v1/effects`, quindi il backoffice non lo può assegnare a una carta.
Spell, Hidden, Terreni e Oggetti hanno sempre un effetto: finché il suo effetto non esiste, la carta non entra nel catalogo. Le creature possono non averne e fino a `ENG-23` non ne hanno.
Le meccaniche `P1` (Spell, Hidden, Terreni) sono state **approvate dal cliente** il 2 novembre (D-21).

## Sprint 3 — 2–8 novembre: preparazione

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-01` | Ambiente pronto: JDK 25, `gradlew test` verde, engine su 8081 ([guida, §1](../guida-sviluppo/guida-sviluppo.md#1-primo-giorno-controlli-da-fare)) | A + B | S | — |
| `ENG-02` | Modello condiviso: `CardDefinition` (con `secondaryFactionId`), `SimulationInput`, `PlayerInput`, `MatchResult`, `PlayerResult`, `MatchEvent` e gli enum `CardType`, `CardSubtype`, `Side`, `BattleResult`, `EndReason`, `EventType` (solo record ed enum, senza logica) | A + B in coppia | S | `ENG-01` |
| `ENG-03` | Leggere le [decisioni aperte](../decisioni-aperte/decisioni-aperte.md) e portare le `Alta` ai DSG il 9 novembre | lead + A | S | — |

`ENG-02` va fatto **insieme, in una sola PR**, prima di tutto il resto: tutte le altre issue usano questi tipi.

## Sprint 4 — 9–15 novembre: regole base e simulazione

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-04` | `RuleConfig` (costruita dalla richiesta, senza valori predefiniti) e `FactionMatrix` con i test: malus −1 (D-04) e carte con due fazioni (D-22) (esempio di test nella [guida, §5](../guida-sviluppo/guida-sviluppo.md#5-scrivere-un-test-dal-regolamento-al-codice)) | A | S | `ENG-02` |
| `ENG-05` | `CreatureInPlay`, `PlayerState`, `MatchState`, `StatModifier` con origine (D-35) | A | M | `ENG-02` |
| `ENG-06` | `RoundResolver`: attacco simultaneo, danni, morti, danno al giocatore | A | M | `ENG-04`, `ENG-05` |
| `ENG-07` | `EndConditionChecker`: le 5 condizioni nell'ordine di [regole-base](../regole-base/regole-base.md#fine-della-partita) | A | M | `ENG-05` |
| `ENG-08` | `MatchLog` con i metodi per ogni tipo di evento e i messaggi in inglese (li leggono i giocatori) | B | S | `ENG-02` |
| `ENG-09` | Classi di supporto ai test: `TestRules` (rule set di prova), `TestCards`, `TestFactions` (`WOUND`, `INVITATION`, `ARCHIVE`, `DREAM`), `TestDecks`, `TestInputs`, `TestStates` | B | S | `ENG-02` |
| `ENG-10` | Validazione dell'input nel core: `InvalidSimulationInputException` con i codici di [api-engine](../api-engine/api-engine.md#errori); `UNKNOWN_EFFECT` arriva con `ENG-22` | B | S | `ENG-02` |
| `ENG-11` | `MatchSimulator`: mescolamento con il seed, pescata delle creature, ciclo dei round, test di determinismo | A | M | `ENG-06`, `ENG-07`, `ENG-08` |
| `ENG-12` | DTO, `SimulationMapper`, `SimulationController` con `POST /api/v1/simulations` | B | M | `ENG-02`, `ENG-10` |
| `ENG-13` | `GlobalExceptionHandler` con `ProblemDetail` e `code` | B | S | `ENG-12` |

**Regole approvate (16 novembre):** decisioni `Alta` chiuse e tutti i punti "Regolamento base" e "Fazioni" della checklist di [test](../test/test.md) coperti da test verdi.

## Sprint 5 — 16–22 novembre: carte speciali e prima sfida

Le issue si chiudono in quest'ordine: prima `ENG-22`, poi `ENG-19`, poi `ENG-20` e `ENG-21`.
In questo sprint le creature non hanno effetti.

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-22` | Framework degli effetti: `EffectRegistry`, `EffectContext` e interfaccia degli effetti di Spell, Hidden e Terreni, controllo `UNKNOWN_EFFECT`, controllo delle morti dopo ogni fase (D-08). B scrive registro e contesto, A li usa nel `DrawService` e nel `RoundResolver` | A + B | M | `ENG-11` |
| `ENG-19` | `DrawService`: Spell `DAMAGE`, `SHIELD`, `BURN` con i modificatori in attesa (D-10…D-13, D-34) e i codici `P1` delle Spell | A | L | `ENG-11`, `ENG-22` |
| `ENG-20` | Zona Hidden e attivazione (D-15, D-16) e codici `P1` delle Hidden | A | M | `ENG-19` |
| `ENG-21` | Terreni: bonus base (D-17, D-19) e codici dei quattro Terreni | A | S | `ENG-19` |
| `ENG-33` | Oggetti (`ITEM`): messi accanto al mazzo come i Terreni, effetto monouso (D-47, D-48), eventi `ITEM_PLACED` e `ITEM_CONSUMED` e codici dei quattro Oggetti | A | S | `ENG-21` |
| `ENG-14` | `GET /api/v1/effects`: elenco dei codici già implementati | B | S | `ENG-12`, `ENG-22` |
| `ENG-15` | Swagger con springdoc 3.2.0, la stessa versione dell'API (D-41, serve l'ok del lead) | B | S | `ENG-12` |
| `ENG-16` | `SimulationExamplesTest`: gli esempi 01–08, 10–12 e 90–95 di [`examples/`](../../examples/README.md) verdi | B | S | `ENG-11`, `ENG-12`, `ENG-22` |
| `ENG-17` | Prova con Docker: engine nel `compose.yaml`, chiamata dall'API o da curl | B | S | `ENG-12` |
| `ENG-24` | Test di contratto con l'`EngineClient` dell'API: stesso JSON di `examples/` | lead + B | S | `ENG-16`, `API-35` |
| `ENG-18` | Demo per il SAL: esempio 08 via Swagger o curl, log mostrato round per round | A + B | S | `ENG-16` |

**SAL (23 novembre):** `gradlew build` verde, una partita si simula via REST con risultato riproducibile.
Con `API-54` l'API usa l'engine vero al posto del mock: una sfida con Spell, Hidden e Terreni arriva fino al risultato.

## Sprint 6 — 23–29 novembre: effetti delle creature e bilanciamento

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-23` | Effetti delle creature: `CreatureEffect` con numero massimo di attivazioni (D-31) e priorità (D-30), collegati al registro di `ENG-22`. Primi effetti `P1`: Hidebehind (esempio 09), Nurikabe, Kappa, SCP-096, SCP-2317, Smiler. A chiama i momenti dal `RoundResolver`, B scrive gli effetti | A + B | L | `ENG-22` |
| `ENG-25` | Effetti Theory of the Invitation e Theory of the Archive ([catalogo](../effetti/catalogo-effetti.md)): i `P1` entro il 30 | A | L | `ENG-22`, `ENG-23` |
| `ENG-26` | Effetti Theory of the Wound e Theory of the Dream: i `P1` entro il 30 | B | L | `ENG-22`, `ENG-23` |
| `ENG-28` | Task `balanceTest` e `BalanceSimulationTest`: primo report per i DSG (vittorie per fazione, partite a `ROUND_LIMIT`, durata media) | B | M | `ENG-11`, `ENG-26` |

**Versione completa (30 novembre):** effetti `P1` nel registro, report di bilanciamento consegnato ai DSG.

## Sprint 7 — 30 novembre – 6 dicembre: test e rifinitura

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-25` / `ENG-26` | Effetti `P2` **solo se decisi entro il 30 novembre** | A / B | M | D-xx |
| `ENG-27` | Effetti secondari dei Terreni, se decisi (D-18) | A | M | `ENG-21` |
| `ENG-28` | Report di bilanciamento con gli effetti, dopo le modifiche dei DSG alle statistiche | B | S | `ENG-26` |
| `ENG-29` | Casi limite e correzione bug; Javadoc su tutto il core pubblico | A + B | M | `ENG-25`, `ENG-26` |
| `ENG-30` | rule set `1.0` caricato nel database dell'API con i valori decisi; documenti aggiornati (tabella effetti, regole-base, decisioni chiuse) | A + B | S | `API-15`, `ENG-03` |
| `ENG-31` | README dell'engine completo, screenshot di Swagger, numero di test e cosa coprono, per la presentazione | B | S | `ENG-15`, `ENG-29` |

**Code freeze domenica 6 dicembre.** Dopo, solo bug con un test che li riproduce.

## Sprint 8 — 7–10 dicembre: collaudo e presentazione

| Issue | Titolo | Chi | Dim. | Dipende da |
|---|---|---|---|---|
| `ENG-32` | Partita dimostrativa per la presentazione (mazzi di prova o carte del catalogo, se pronte; seed scelto) e prova generale | A | S | `ENG-29` |

**Collaudo superato (9 dicembre):** `gradlew build` verde, nessun bug bloccante.

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
| Carte speciali, framework e integrazione finiscono tutti nello Sprint 5 | la prima sfida completa arriva a ridosso del SAL | si rispetta l'ordine `ENG-22` → `ENG-19` → `ENG-20`, `ENG-21`; se manca tempo si rimandano i singoli effetti, non le meccaniche |
| Il framework degli effetti nasce sbagliato | gli effetti vanno riscritti | `ENG-22` si fa in coppia e si prova con un effetto di prova per Spell, Hidden e Terreno; `ENG-23` si prova subito con 3 effetti diversi (Hidebehind, SCP-096, SCP-2317) prima di fare gli altri |
| Troppi effetti per il tempo rimasto (oltre 35 creature con effetto, contando le nuove) | effetti incompleti alla Versione completa | si chiudono per lotti `P1` → `P2`; un effetto non registrato non si può assegnare a una carta |
| Contratto REST cambiato senza avvisare | l'API smette di funzionare | ogni modifica a `api-engine.md` va segnalata al lead nella PR; `ENG-24` lo verifica |
| Un solo sviluppatore conosce un pezzo | blocco se è assente | revisione incrociata obbligatoria; ognuno fa almeno un effetto delle fazioni dell'altro se resta tempo |
