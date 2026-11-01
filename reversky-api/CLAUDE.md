# CLAUDE.md — reversky-api

Istruzioni per Claude Code su questo modulo. Le regole complete e condivise con gli altri agenti sono in AGENTS.md, importato qui sotto:

@AGENTS.md

## Note specifiche per Claude

- Rispondi e scrivi commenti/documentazione **in italiano**; nomi di classi, metodi e variabili **in inglese** (vedi `CodingRequirements.md`).
- Prima di modificare una funzionalità apri il suo file in `docs/` e rispettane le regole.
- Prima di creare una nuova classe cerca se ne esiste già una simile nel package della funzionalità.
- Non aggiungere dipendenze a `build.gradle` senza chiederlo.
- Non modificare file in `src/main/resources/db/migration/` già esistenti: crea sempre una nuova migrazione.
- Non toccare il modulo `../reversky-engine` quando lavori qui, salvo richiesta esplicita.
- A fine lavoro: esegui `./gradlew test` e riassumi in poche righe cosa hai cambiato e quali documenti hai aggiornato.
