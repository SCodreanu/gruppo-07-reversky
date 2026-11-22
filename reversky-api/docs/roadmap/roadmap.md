# Roadmap dell'API

Piano di lavoro di `reversky-api` fino alla presentazione finale del **10 dicembre 2026**, con il **code freeze domenica 6 dicembre**.
Le fasi e le milestone sono quelle della [roadmap dell'engine](../../../reversky-engine/docs/roadmap/roadmap.md#fasi-e-milestone-dalla-proposta-al-cliente), cioè la roadmap presentata al cliente.
Le issue sono sulla board del progetto con l'ID `API-xx` nel titolo.

## Stato al 22 novembre

Fatto, unito in `backend_api` e con la issue chiusa (`API-01`…`API-06`, `API-08`…`API-11`, `API-14`…`API-20`, `API-22`…`API-44`, `API-54`…`API-58`, `API-62`, `API-63`, `API-70`, `API-71`):

| Area | Cosa c'è |
|---|---|
| Base | infrastruttura comune, Flyway con la baseline `V1`–`V8` e le migrazioni `V9`–`V14`, errori `ProblemDetail`, paginazione, Swagger |
| Account | registrazione, login, token, reset password, verifica email, gestione degli utenti dal backoffice (`API-03`) |
| Catalogo | rule set, rarità, fazioni, bonus di fazione, carte con rarità e fazioni, tipo Oggetto, 120 carte di `V11`, effetti validi nella tabella `effects` (`V13`, `API-36`), vincoli allineati all'engine (`V14`, `API-71`), creazione delle carte con l'illustrazione e immagini servite su `/images/**` (`API-14`) |
| Economia | inventario e crediti, pacchetti con slot e bonus, acquisto, apertura, pacchetto base (`V12`), test del flusso completo e degli errori a metà acquisto o apertura (`API-56`) |
| Gioco | collezione con copie riservate, mazzi con `DeckValidator`, amicizie, sfide amichevoli simulate dall'engine con storico e retry, scambi con proposta, controproposta e accettazione atomica (`API-31`…`API-34`), test dei flussi completi di scambi e amichevoli con i retry sovrapposti (`API-57`, senza la parte classificata) |
| Engine | `EngineClient` (solo `POST /api/v1/simulations`), test di contratto con le fotografie dell'engine (`ENG-24`), dati di prova del profilo `dev` (`API-25`), integrazione reale sullo stack di Docker Compose con sfida, carte speciali, retry a engine spento e scambio (`API-54`, ripetibile con `scripts/prova-sfida.ps1`, vedi [sfide](../sfide/sfide.md#prova-sullo-stack-completo)) |
| Sicurezza | matrice degli accessi di tutti gli endpoint controllata da `SecurityMatrixIT`, token scaduti ignorati sugli endpoint pubblici (bug corretto), risorse altrui `404` (`API-58`, [autenticazione](../autenticazione/autenticazione.md#endpoint-pubblici-e-protetti)) |

`./gradlew build` verde con 959 test (22 novembre, con `API-58`: la matrice conta 235 casi).

## Da fare

### 23–29 novembre

| Issue | Cosa | Note |
|---|---|---|
| — | Integrazione con il frontend su un branch separato (dal 23 novembre) | non mescolare il codice del frontend in `backend_api` |
| — | Chiedere ai DSG D-04 (bonus di fazione) | serve a `ENG-30` e ad `API-64`; D-01 è deciso il 22 novembre (30 punti vita, già nel rule set `1.0`) |

### 30 novembre – 6 dicembre (code freeze)

| Issue | Cosa | Note |
|---|---|---|
| `API-59` (#129) | Verifica finale di Swagger | ogni endpoint con le sue risposte |
| `API-64` (#134) | Valori di gioco nel database (rule set, rarità, bonus di fazione, carte) | con le risposte dei DSG; sono dati del backoffice o migrazioni nuove, mai codice |
| `ENG-29`, `ENG-30` | Lato engine: casi limite e Javadoc, rule set `1.0` definitivo | nella roadmap dell'engine |

### 7–10 dicembre (consegna)

| Issue | Cosa | Note |
|---|---|---|
| `API-60` (#130) | Documentazione del backend e README | |
| `API-61` (#131) | Export del database per la consegna | |
| — | Togliere il default di `JWT_SECRET` da `application.properties` e `compose.yaml` prima di qualsiasi deploy | il valore committato permette di firmare token di admin |
| `ENG-31`, `ENG-32` | Lato engine: README, materiale e partita dimostrativa | nella roadmap dell'engine |

## Rimandati

| Issue | Cosa | Perché |
|---|---|---|
| `API-51`…`API-53` (#121–#123) | Audit e statistiche del backoffice | il backoffice si completa dopo (deciso il 14 novembre) |
| `API-65`…`API-69` (#135–#139) | Notifiche e canale SSE | priorità bassa: si decide con il frontend dal 23 novembre se servono per la consegna |

## Fuori perimetro di questo gruppo di lavoro

| Issue | Cosa | Di chi |
|---|---|---|
| `API-07`, `API-12`, `API-13`, `API-21` (#77, #82, #83, #91) | Tag e filtro dei pacchetti per tag | `acistar0` |
| `API-45`…`API-50` (#115–#120) | Partite classificate: ricompense, rating Elo, coda di matchmaking | `antoninoconsolato-its` |
| parte classificata di `API-57` | Test di integrazione di coda, abbinamento, Elo e crediti | segue `API-45`…`API-50` |

## Piccole cose aperte

- `RuleSetRepository` legge ancora `battles` con SQL nativo per sapere se un rule set è usato: può passare a un metodo di `battle/` come gli inviti (#204).
- La tabella `effects` (`V13`) va tenuta allineata a mano con `EffectRegistry.withAllEffects()` dell'engine: un effetto nuovo arriva con una migrazione nuova insieme al suo codice.
- Nelle query JPQL sugli enum PostgreSQL gli stati vanno passati come parametri: un letterale enum diventa un cast al tipo sbagliato (es. `tradestatus` invece di `trade_status`).

## Come si lavora

- Un branch per issue da `backend_api` (`feature/<n-issue>-descrizione`), `./gradlew build` verde, documenti di `docs/` aggiornati nello stesso commit, PR verso `backend_api`.
- Le modifiche all'engine si fanno su `backend_engine` e arrivano qui passando da `backend` (due PR, come #201 e #202): la copia di `reversky-engine` in `backend_api` resta uguale a quella di `backend_engine`.
- Le PR verso `backend_api` non chiudono le issue (lo fa solo `main`): si chiudono a mano dopo il merge.
