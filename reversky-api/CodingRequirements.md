# Coding Requirements — reversky-api

Regole di scrittura del codice per il backend. Valgono per tutti: studenti e agenti AI.
Obiettivo: codice **leggibile, prevedibile e uguale ovunque**, anche se scritto da persone diverse.

---

## 1. Lingua

| Cosa | Lingua | Esempio |
|---|---|---|
| Classi, metodi, variabili, package | Inglese | `DeckService.validateDeck()` |
| Endpoint REST | Inglese, plurale, minuscolo | `/api/v1/decks` |
| Tabelle e colonne DB | Inglese, `snake_case` | `collection_cards.card_id` |
| Commenti e Javadoc | Italiano | `// Blocca le righe per evitare doppie spese` |
| Testi nel codice: errori per l'utente, validazione, email, log, Swagger, `@DisplayName` | Inglese | `"Insufficient credits"` |
| Messaggi di commit | Italiano | `feat(deck): aggiunta validazione copie` |

In breve: **tutto ciò che sta tra virgolette nel codice è in inglese** (il prodotto è English first); commenti, Javadoc e messaggi di commit restano in italiano.

### Glossario italiano → inglese

Usiamo sempre questi termini, mai sinonimi:

| Italiano | Inglese nel codice |
|---|---|
| Carta del catalogo | `CatalogCard` |
| Creatura | `Creature` |
| Rarità | `Rarity` |
| Fazione / Tipologia | `Faction` |
| Bonus di fazione | `FactionBonus` |
| Etichetta | `Tag` |
| Mazzo | `Deck` |
| Collezione | `CollectionCard` |
| Inventario (crediti + pacchetti) | `Inventory` |
| Pacchetto / Bustina | `Pack` |
| Crediti | `credits` |
| Amicizia | `Friendship` |
| Scambio / Offerta / Controproposta | `Trade` / `TradeOffer` / `counter` |
| Invito a una sfida | `BattleInvite` |
| Partita (sfida giocata) | `Battle` |
| Sfidante / Avversario | `challenger` / `opponent` |
| Punti vita | `lifePoints` |
| Attacco / Difesa | `atk` / `def` (come nel database) |

---

## 2. Formattazione

- Indentazione con **tab** (come i file generati da Spring Initializr).
- Parentesi graffe sempre, anche per `if` di una riga.
- Un solo `import` per riga, niente `import x.*`.
- Ordine dei membri di una classe: costanti → campi → costruttori → metodi pubblici → metodi privati.
- Formatta il file prima di ogni commit (formatter dell'IDE).

```java
// SÌ
if (deck.isEmpty()) {
	throw new InvalidDeckException("The deck is empty");
}

// NO
if (deck.isEmpty()) throw new InvalidDeckException("The deck is empty");
```

---

## 3. Nomi

| Elemento | Convenzione | Esempio |
|---|---|---|
| Classe | `PascalCase`, sostantivo | `TradeService` |
| Metodo | `camelCase`, verbo | `acceptTrade()` |
| Variabile | `camelCase`, parlante | `remainingCredits` (non `rc`) |
| Costante | `UPPER_SNAKE_CASE` | `MAX_DECK_SIZE` |
| Booleano | prefisso `is` / `has` / `can` | `isValid`, `hasEnoughCredits` |
| DTO richiesta | `XxxRequest` | `CreateDeckRequest` |
| DTO risposta | `XxxResponse` | `DeckResponse` |
| Eccezione | `XxxException` | `InsufficientCreditsException` |
| Test | `XxxTest` / `XxxIT` (integrazione) | `DeckServiceTest` |

---

## 4. Architettura a livelli

```
Controller  →  Service  →  Repository  →  Database
   (HTTP)      (logica)      (JPA)
```

| Livello | Fa | Non fa |
|---|---|---|
| **Controller** | riceve la richiesta, valida il DTO (`@Valid`), chiama il service, restituisce il DTO | logica di business, accesso diretto ai repository |
| **Service** | regole di business, transazioni, conversione entità ↔ DTO | conoscere HTTP (`HttpServletRequest`, status code) |
| **Repository** | query al database | logica |

- Un controller **non restituisce mai un'entità JPA**: sempre un DTO.
- I service di una funzionalità possono chiamare service di un'altra funzionalità, **non i suoi repository**.

---

## 5. Classi e metodi

- **Dependency injection solo tramite costruttore** (con `@RequiredArgsConstructor` e campi `private final`). Mai `@Autowired` sui campi.
- Metodi corti: se supera ~30 righe, spezzalo in metodi privati con nomi chiari.
- Un metodo fa una cosa sola.
- DTO come **`record`** Java.
- Non restituire mai `null` per una lista: restituisci una lista vuota.
- Per "potrebbe non esserci" usa `Optional` nei repository, poi nel service trasformalo in eccezione.
- Niente "numeri magici": i valori del regolamento stanno nel database (rule set attivo, rarità, `faction_bonus`) e si modificano dal backoffice; in `application.properties` solo la configurazione tecnica.

```java
// SÌ
@Service
@RequiredArgsConstructor
public class DeckService {

	private final DeckRepository deckRepository;
	private final CollectionService collectionService;
	...
}

// NO
@Service
public class DeckService {
	@Autowired
	private DeckRepository deckRepository;
}
```

```java
// DTO come record, con validazione
public record CreateDeckRequest(
		@NotBlank @Size(max = 50) String name,
		@NotNull @Size(min = 1) List<DeckCardRequest> cards) {
}
```

### Lombok

| Permesso | Vietato |
|---|---|
| `@Getter`, `@Setter` (solo dove serve) | `@Data` sulle entità JPA (rompe `equals`/`hashCode`) |
| `@RequiredArgsConstructor` | `@SneakyThrows` |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` sulle entità | `@AllArgsConstructor` sulle entità |
| `@Slf4j` | |

---

## 6. Entità JPA

- I tipi seguono lo schema: `Integer id` per le chiavi intere, `UUID id` per le tabelle con UUID, `@EmbeddedId` per le chiavi composte.
- Relazioni `@ManyToOne(fetch = FetchType.LAZY)` di default.
- Enum PostgreSQL: `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- Colonne `jsonb`: `@JdbcTypeCode(SqlTypes.JSON)`.
- Date `timestamp` in `LocalDateTime`, sempre in UTC.
- Le tabelle vengono create **solo da Flyway**: `spring.jpa.hibernate.ddl-auto=validate`.

Esempi completi in [docs/database](docs/database/database.md).

---

## 7. Transazioni

- `@Transactional` sui **metodi del service**, mai sul controller.
- Metodi di sola lettura: `@Transactional(readOnly = true)`.
- Ogni operazione che modifica crediti o carte (comprare o aprire un pacchetto, accettare uno scambio) deve essere **una sola transazione**: o va tutto a buon fine, o non cambia niente.
- Le azioni del backoffice scrivono la riga di `audit_logs` nella **stessa transazione** della modifica.
- Quando due utenti modificano le stesse righe (scambi), blocca le righe in ordine di `id` crescente per evitare deadlock (vedi [docs/scambi](docs/scambi/scambi.md)).

---

## 8. Errori

- Ogni errore di business è un'eccezione specifica che estende `BusinessException`.
- Le eccezioni vengono trasformate in risposte HTTP **in un unico punto**: `GlobalExceptionHandler` (`@RestControllerAdvice`) in `common/`.
- Il formato della risposta di errore è `ProblemDetail` (vedi [docs/api-rest](docs/api-rest/api-rest.md)).
- Mai `catch (Exception e) {}` vuoti. Mai restituire lo stack trace al client.

```java
public class InsufficientCreditsException extends BusinessException {
	public InsufficientCreditsException(int required, int available) {
		super(Category.RULE_VIOLATION, "INSUFFICIENT_CREDITS", "Insufficient credits",
				"Insufficient credits: " + required + " required, " + available + " available");
	}
}
```

---

## 9. Commenti e Javadoc

### Quando scrivere un commento

- **Spiega il perché, non il cosa.** Il codice dice già cosa fa.
- Commenta le regole di business non ovvie e i riferimenti al regolamento.
- Non lasciare codice commentato: c'è Git.

```java
// NO: ripete il codice
// incrementa la quantità di 1
userCard.setQuantity(userCard.getQuantity() + 1);

// SÌ: spiega una scelta
// Blocchiamo prima l'utente con id minore: se due scambi incrociati
// partono insieme, entrambi bloccano nello stesso ordine e non si crea deadlock.
List<Integer> orderedIds = Stream.of(requesterId, recipientId).sorted().toList();
```

### Javadoc

Obbligatorio su:
- classi pubbliche di service, controller ed eccezioni;
- metodi pubblici dei service.

Formato:

```java
/**
 * Apre un pacchetto dell'inventario dell'utente.
 * <p>
 * Toglie il pacchetto dall'inventario, estrae le carte secondo slot e bonus
 * del pacchetto e le aggiunge alla collezione, tutto nella stessa transazione.
 *
 * @param userId id dell'utente che apre il pacchetto
 * @param packId id del tipo di pacchetto
 * @return le carte ottenute, nell'ordine degli slot
 * @throws NoPackToOpenException se l'utente non ha pacchetti di quel tipo
 */
@Transactional
public OpenPackResponse openPack(Integer userId, Integer packId) { ... }
```

### TODO

Formato: `// TODO(@username): cosa manca` — con l'utente GitHub di chi se ne occupa. Ogni TODO deve avere una issue su GitHub Projects.

---

## 10. Logging

- Usa `@Slf4j` e `log.info/warn/error`, mai `System.out.println`.
- `info` per eventi importanti (partita simulata, scambio accettato), `debug` per i dettagli, `error` solo per errori inattesi.
- **Mai** loggare password, token (JWT, refresh, reset) o dati personali.

```java
log.info("Trade {} accepted by user {}", tradeId, userId);
```

---

## 11. Sicurezza

- Password salvate solo con **BCrypt**; token (refresh, reset, verifica email) solo come hash **SHA-256**.
- L'id dell'utente si prende **sempre dal token**, mai dal body o dall'URL quando si agisce "come me".
- Ogni endpoint che legge una risorsa altrui (mazzo, scambio, partita) controlla che l'utente ne abbia diritto.
- Gli endpoint `/api/v1/admin/**` richiedono il ruolo `ADMIN`.

---

## 12. Test

Vedi [docs/test](docs/test/test.md). In breve:
- ogni service ha test unitari; ogni regola di business ha almeno un test;
- nome del test che descrive il comportamento, struttura **Arrange / Act / Assert**;
- un bug corretto = un test che lo riproduce.

---

## 13. Git e GitHub Projects

- Branch: `feature/<n-issue>-descrizione-breve`, `fix/<n-issue>-descrizione`. Es: `feature/42-validazione-mazzi`.
- Commit in italiano, formato *Conventional Commits*: `tipo(ambito): descrizione`.
  - tipi: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
  - es: `fix(trade): impedita accettazione di scambi già chiusi`.
- Mai push diretto su `main`: sempre Pull Request con almeno **una revisione** di un altro SWD.
- La PR cita la issue (`Closes #42`).

---

## 14. Checklist prima della Pull Request

- [ ] `./gradlew build` passa (compilazione + test).
- [ ] Nuova logica coperta da test.
- [ ] Nessun `System.out`, nessun codice commentato, nessun import inutilizzato.
- [ ] Javadoc sui metodi pubblici dei service.
- [ ] Endpoint nuovi visibili e descritti in Swagger.
- [ ] Migrazione Flyway nuova se è cambiato lo schema.
- [ ] Documento in `docs/` aggiornato se è cambiato un comportamento.
