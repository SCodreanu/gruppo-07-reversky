# CLAUDE.md — reversky-engine

Istruzioni per Claude Code su questo modulo. Le regole complete e condivise con gli altri agenti sono in AGENTS.md, importato qui sotto:

@AGENTS.md

## Note specifiche per Claude

- Rispondi e scrivi commenti/documentazione **in italiano**; nomi di classi, metodi e variabili **in inglese** (vedi `CodingRequirements.md`).
- Prima di toccare la simulazione rileggi [docs/regole-base](docs/regole-base/regole-base.md): l'**ordine** dei passi di un round è parte delle regole.
- Quando aggiungi un effetto, segui la ricetta in [docs/effetti](docs/effetti/effetti.md) e scrivi il test nello stesso lavoro.
- Non importare classi Spring nel package `core`. Se ti serve qualcosa di Spring, mettilo in `web`.
- Non introdurre casualità fuori dal `RandomGenerator` creato dal seed.
- Non aggiungere dipendenze a `build.gradle` senza chiederlo.
- Non toccare il modulo `../reversky-api` quando lavori qui, salvo richiesta esplicita. Se cambi il contratto REST, segnalalo: va aggiornato anche l'`EngineClient` dell'API.
- A fine lavoro: esegui `./gradlew test` e riassumi in poche righe cosa hai cambiato e quali documenti hai aggiornato.
