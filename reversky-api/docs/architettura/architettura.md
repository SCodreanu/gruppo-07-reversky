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

Package per funzionalità. Ogni funzionalità contiene i propri livelli:

```
it.downfall.reversky.api
├── deck/
│   ├── DeckController.java
│   ├── DeckService.java
│   ├── DeckValidator.java
│   ├── DeckRepository.java
│   ├── Deck.java              (entità)
│   ├── DeckCard.java          (entità)
│   └── dto/
│       ├── CreateDeckRequest.java
│       └── DeckResponse.java
├── trade/ ...
└── common/
    ├── exception/   BusinessException, GlobalExceptionHandler
    ├── security/    SecurityConfig, JwtService, TokenHasher
    ├── audit/       AuditService (scrive audit_logs)
    └── config/      configurazioni condivise
```

Vantaggio: tutto ciò che riguarda i mazzi è in un posto solo, e ogni membro del team può lavorare su una funzionalità senza toccare le altre.

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
   ├─ CollectionService.getOwnedQuantities(userId)   possesso carte
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
- L'API invia **tutti i dati che servono**: carte (statistiche, fazione, effetto ricavato dai tag) e tabella dei bonus di fazione, perché l'engine non ha database.
- L'API genera il **seed** casuale e lo salva: così una partita è riproducibile e nessun giocatore può influenzare il mescolamento.

Dettagli in [sfide](../sfide/sfide.md) e nel modulo `reversky-engine` (`docs/api-engine`).

## Configurazione

Ciò che sta nel database (rarità, limiti di copie, bonus di fazione, pacchetti e probabilità) si gestisce dal backoffice.
Gli altri valori di gioco ed economia stanno in `application.properties` con prefisso `reversky.`:

```properties
reversky.engine.base-url=http://localhost:8081
reversky.credits.signup-bonus=500
reversky.credits.win-reward=30
reversky.deck.total-cards=40
```

Vengono letti con una classe `@ConfigurationProperties`, non con `@Value` sparsi nel codice.
