# Architettura

## Visione d'insieme

Reversky è composto da tre parti indipendenti:

```
┌──────────────┐   HTTP/JSON   ┌──────────────────┐   HTTP/JSON   ┌───────────────────┐
│   Frontend   │ ────────────▶ │   reversky-api   │ ────────────▶ │  reversky-engine  │
│  (Next.js)   │ ◀──────────── │  (questo modulo) │ ◀──────────── │ (motore di gioco) │
└──────────────┘               └────────┬─────────┘               └───────────────────┘
                                        │ JPA
                                        ▼
                                 ┌─────────────┐
                                 │ PostgreSQL  │
                                 └─────────────┘
```

| Modulo | Responsabilità | Database |
|---|---|---|
| Frontend | interfaccia, validazione "di comodità" | no |
| **reversky-api** | utenti, inventario, carte, mazzi, amicizie, scambi, sfide, backoffice | **sì, è l'unico** |
| reversky-engine | simula una partita dati due mazzi e un seed | no |

**Regola chiave:** solo `reversky-api` parla con il database. L'engine riceve tutto ciò che gli serve nella richiesta e restituisce il risultato.

## Organizzazione dei package

Package per funzionalità. Dentro ogni funzionalità c'è un sotto-package per ogni livello:

```
it.downfall.reversky.api
├── deck/
│   ├── controller/   DeckController
│   ├── service/      DeckService, DeckValidator
│   ├── repository/   DeckRepository, DeckCardRepository
│   ├── entity/       Deck, DeckCard
│   ├── dto/          CreateDeckRequest, DeckResponse
│   └── exception/    DeckNotFoundException, ...
├── trade/ ...
└── common/
    ├── exception/   BusinessException, GlobalExceptionHandler
    ├── security/    SecurityConfig, JwtService, TokenHasher
    ├── mail/        EmailSender e la sua implementazione SMTP
    ├── audit/       AuditService (scrive audit_logs)
    └── config/      configurazioni condivise (Clock, OpenAPI)
```

| Sotto-package | Contiene |
|---|---|
| `controller/` | i `@RestController` della funzionalità |
| `service/` | i `@Service` e le classi di logica che usano (es. `DeckValidator`) |
| `repository/` | le interfacce Spring Data JPA |
| `entity/` | le entità JPA |
| `dto/` | i `record` di richiesta e risposta |
| `exception/` | le eccezioni di business della funzionalità |
| `job/` | i task `@Scheduled` (es. `auth/job/TokenCleanupJob`) |

- Si crea solo il sotto-package che serve: una funzionalità senza job non ha `job/`.
- `common/` non è una funzionalità: resta diviso per argomento (`security/`, `mail/`, ...).
- I test seguono gli stessi package delle classi che testano (es. `auth/service/AuthServiceIT`).

Vantaggio: tutto ciò che riguarda i mazzi è in un posto solo, e ogni membro del team può lavorare su una funzionalità senza toccare le altre. Dentro la funzionalità, ogni classe ha un posto preciso in base al suo livello.

## Flusso di una richiesta

Esempio: l'utente salva un nuovo mazzo.

```
POST /api/v1/decks
   │
   ▼
SecurityFilter      → verifica il JWT, ricava l'utente
   │
   ▼
DeckController      → @Valid sul CreateDeckRequest (campi obbligatori, lunghezze)
   │
   ▼
DeckService         → @Transactional
   ├─ CollectionService.getAvailableQuantities(userId)   copie disponibili
   ├─ DeckValidator.validate(...)                    vincoli del regolamento
   └─ DeckRepository.save(...)
   │
   ▼
DeckResponse (201 Created)
```

Se una regola non è rispettata il service lancia un'eccezione (es. `InvalidDeckException`), che `GlobalExceptionHandler` trasforma in un errore `400/422` con formato standard.

## Comunicazione con l'engine

- L'API chiama l'engine con un `EngineClient` (in `battle/`), basato su `RestClient` di Spring.
- L'URL dell'engine è in configurazione: `reversky.engine.base-url=http://localhost:8081`.
- L'API invia **tutti i dati che servono**: carte (statistiche, fazione, sottotipo, effetto) e tabella dei bonus di fazione, perché l'engine non ha database.
- L'API genera il **seed** casuale e lo salva: così una partita è riproducibile e nessun giocatore può influenzare il mescolamento.

Dettagli in [sfide](../sfide/sfide.md) e nel modulo `reversky-engine` (`docs/api-engine`).

## Configurazione

**Tutti i valori di gioco ed economia stanno nel database e si gestiscono dal backoffice:** rule set (punti vita, limite di round,
composizione del mazzo, crediti), rarità e limiti, bonus di fazione, pacchetti e probabilità. L'engine li riceve in ogni richiesta.

In `application.properties` (prefisso `reversky.`) resta solo la configurazione **tecnica**:

```properties
reversky.engine.base-url=http://localhost:8081
```

Vengono letti con una classe `@ConfigurationProperties`, non con `@Value` sparsi nel codice.
