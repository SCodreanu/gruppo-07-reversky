# Test

## Perché

Le parti più rischiose del backend (crediti, pacchetti, scambi) spostano "valore" degli utenti. Un bug lì significa carte duplicate o crediti persi. I test sono la garanzia che non succeda.

## Tipi di test

| Tipo | Cosa testa | Strumenti | Nome file |
|---|---|---|---|
| **Unitario** | una classe da sola (service, validator) | JUnit 5, Mockito, AssertJ | `XxxTest` |
| **Controller** | endpoint HTTP: status, JSON, sicurezza | `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers | `XxxControllerIT` |
| **Repository** | query JPA personalizzate | `@DataJpaTest` + Testcontainers PostgreSQL | `XxxRepositoryTest` |
| **Integrazione** | un flusso completo con database vero | `@SpringBootTest` + Testcontainers | `XxxIT` |

Testcontainers avvia un PostgreSQL vero in Docker (configurazione in `TestcontainersConfiguration`): **serve Docker acceso** per eseguire i test di repository, controller e integrazione.

I test dei controller caricano tutta l'applicazione invece di usare `@WebMvcTest`: così passano davvero dai filtri di Spring Security, da `GlobalExceptionHandler` e dal database, senza mock dei service. Spring riusa il contesto tra le classi di test con la stessa configurazione (stesse annotazioni e stessi `@MockitoBean`), quindi conviene tenerla uguale: ad esempio tutti i test che usano MockMvc dichiarano `@MockitoBean EmailSender`.

### Configurazione dei test

`src/test/resources/config/application.properties` si **somma** a `src/main/resources/application.properties` (Spring legge anche `classpath:/config/`, con la precedenza) e cambia solo ciò che serve ai test. Non va spostato nella radice di `src/test/resources`: un `application.properties` lì sostituirebbe quello principale invece di sommarsi.

Oggi spegne l'abbinamento della coda classificata (`reversky.matchmaking.enabled=false`, API-49): senza il job ogni 2 secondi e il tentativo dopo ogni ingresso, un test della coda come `MatchmakingControllerIT` non viene abbinato mentre gira. I test dell'abbinamento chiamano `MatchmakingService.matchAll()` da soli (`MatchmakingServiceIT`).

## Comandi

```bash
./gradlew test                              # tutti i test
./gradlew test --tests "*DeckValidatorTest" # una sola classe
```

## Come si scrive un test

- Nome che descrive il comportamento: `metodo_condizione_risultatoAtteso`, oppure `@DisplayName` in inglese.
- Struttura **Arrange / Act / Assert**, separata da una riga vuota.
- Un test verifica **una** cosa.
- Asserzioni con AssertJ (`assertThat`), più leggibili.

```java
class DeckValidatorTest {

	private final DeckValidator validator = new DeckValidator(DeckRules.defaults());

	@Test
	@DisplayName("A deck with 4 copies of the same Common is invalid")
	void validate_tooManyCommonCopies_returnsCopyLimitViolation() {
		// Arrange
		CatalogCard crawlers = TestCards.creature("Crawlers", TestRarities.common()); // deck_limit 3
		Map<CatalogCard, Integer> deck = TestDecks.validDeckWith(crawlers, 4);

		// Act
		List<DeckViolation> violations = validator.validate(deck, TestDecks.ownsEverything(deck));

		// Assert
		assertThat(violations)
				.extracting(DeckViolation::code)
				.containsExactly("DECK_COPY_LIMIT");
	}
}
```

## Test con la casualità

Pacchetti e seed usano un `RandomGenerator` iniettato. Nei test se ne passa uno con seed fisso:

```java
PackService service = new PackService(..., new Random(42));
```

Per le probabilità si fa un test statistico con tolleranza:

```java
@Test
void rollBonus_thousandUnits_succeedsAboutTenPercentOfTimes() {
	PackBonusRule mythicRule = TestPacks.bonusRule(TestRarities.mythic(), 1_000); // 10%
	int successes = 0;
	for (int i = 0; i < 100_000; i++) {
		if (roller.rollBonus(mythicRule)) {
			successes++;
		}
	}
	assertThat(successes / 100_000.0).isCloseTo(0.10, within(0.01));
}
```

## Test con l'engine

L'engine **non** viene avviato nei test dell'API. Si sostituisce `EngineClient` con un mock:

```java
@MockitoBean
EngineClient engineClient;

when(engineClient.simulate(any())).thenReturn(TestSimulations.challengerWins());
```

Per l'errore dell'engine: `thenThrow(new EngineUnavailableException(...))`.

### Test di contratto (`ENG-24`)

Il mock non dice se l'API e l'engine parlano lo stesso JSON: lo dice `EngineContractTest` (`battle/client`), deciso il 22 novembre. Senza Spring e senza engine, legge gli stessi file del test dell'engine:

- le richieste di `reversky-engine/examples/` (anche quelle di errore): passano da `SimulationRequest` e da `EngineClient` verso un server HTTP finto del JDK e devono arrivare **identiche** al file;
- le fotografie delle risposte in `reversky-engine/examples/responses/`, servite con il loro status: `EngineClient` le legge senza perdere campi (riscritta, la risposta è uguale al file; il `log` resta un JSON qualsiasi) e devono dare i valori di `expected-results.csv` ed `expected-results-effects.csv`; le risposte `400` diventano `ENGINE_INPUT_REJECTED`;
- ogni esempio deve avere la sua fotografia e la sua riga nei CSV.

L'engine controlla con `SimulationContractTest` che le fotografie siano le sue risposte vere. Un campo nuovo o rinominato da una delle due parti fa quindi fallire un test. `reversky-engine/examples` è un input del task `test` (`build.gradle`), così una fotografia cambiata rilancia anche i test dell'API.

## Cosa deve essere coperto

Obbligatorio:
- [ ] ogni regola di `DeckValidator`, compreso `min(deck_limit, copy_limit)`; da controllare regola per regola in un task a parte (deciso il 22 novembre con `API-58`);
- [x] acquisto pacchetto: crediti scalati, crediti insufficienti, saldo mai negativo, limite di acquisto, pacchetto gratuito (`PackPurchaseControllerIT`, `InventoryServiceIT`, il flusso in `PackFlowIT` e l'errore a metà in `PackFlowRollbackIT`);
- [ ] apertura pacchetto: numero e rarità delle carte, bonus, filtro dei tag, ripiego degli slot senza carte, nessun pacchetto da aprire; il filtro dei tag è coperto da `CardPoolTagsTest` e `PackTagsIT` (`API-21`), il resto si controlla insieme a `DeckValidator`;
- [x] scambi: tutti gli stati e i turni, controproposte, copie riservate, copie non più disponibili, **totale delle carte invariato** (`TradeProposalControllerIT`, `TradeControllerIT`, `TradeAcceptIT`, e il flusso completo in `TradeFlowIT`);
- [x] amicizie: richiesta inversa, richiesta dopo un rifiuto, richieste opposte contemporanee;
- [x] sfide: stati dell'invito, mazzo non più valido, engine non disponibile, retry con lo stesso seed, retry sovrapposti che non assegnano due volte Elo e crediti. Le amichevoli sono coperte (`BattleInviteControllerIT`, `BattleControllerIT`, `FriendlyBattleFlowIT`, compresi i retry sovrapposti), e così l'abbinamento delle classificate (`MatchmakingServiceIT`, API-49: sfidante, finestra, mazzo non più valido, anti-boosting, `SKIP LOCKED` e abbinamenti in parallelo) l'Elo (`RankedBattleFlowIT` e `RankingServiceTest`, API-50) e i crediti delle classificate (`RankedBattleFlowIT`, API-45), compresi i retry sovrapposti;
- [x] autenticazione: rotazione del refresh token, reset password che invalida i refresh token, account disattivato;
- [ ] backoffice: ogni azione scrive in `audit_logs`; arriva con `API-52`, rimandato con il resto del backoffice (14 novembre);
- [x] sicurezza: endpoint protetti senza token → `401`, admin con `USER` → `403`, risorse altrui → `404` (`API-58`: la matrice di tutti gli endpoint in `common/security/SecurityMatrixIT`, i token scaduti sugli endpoint pubblici in `PublicEndpointTokenIT`, le risorse altrui nei test di ogni endpoint).

### Sicurezza (`API-58` #128)

- `common/security/SecurityMatrixIT`: una riga per ogni endpoint (metodo, percorso, `PUBLIC`/`USER`/`ADMIN`). Senza token un endpoint protetto dà `401 AUTH_REQUIRED`; con un token `USER` `/admin/**` dà `403 ACCESS_DENIED`; chi ha diritto non riceve né `401` né `403` (le richieste usano id inesistenti e corpi vuoti, quindi si fermano a `400` o `404` e non scrivono niente). Un altro test legge gli endpoint da Spring e fallisce se uno non ha la sua riga o se una riga non esiste più.
- `common/security/PublicEndpointTokenIT`: un token scaduto, non valido o malformato sugli endpoint pubblici si ignora (refresh, login, catalogo, negozio); sugli endpoint protetti resta `401`. I quattro test sugli endpoint pubblici erano rossi prima della correzione.
- Risorse di altri utenti → `404`, nei test di ogni endpoint: mazzi (lettura, modifica, eliminazione), partite (dettaglio e retry), inviti (accettazione, rifiuto, ritiro), scambi (dettaglio, controproposta, accettazione, rifiuto), richieste di amicizia (accettazione e rifiuto da un terzo), rimozione di chi non è amico.

### Flussi completi (`API-56` #126, `API-57` #127)

Classi `XxxIT` nella cartella della funzionalità (non in `controller/`, perché un flusso attraversa controller, service e database) ripetono via HTTP, con un JWT per ogni utente, i percorsi che farà il frontend. I casi di errore restano nei test dei singoli endpoint.

| Classe | Flusso | Cosa controlla |
|---|---|---|
| `trade/TradeFlowIT` | proposta, due controproposte, accettazione; poi le copie ricevute regalate a un terzo | a ogni passo le copie riservate sono solo quelle dell'offerta in corso (lette da `GET /collection`); si sposta l'ultima offerta; il totale di ogni carta nel sistema non cambia; lo scambio chiuso non è in nessuna casella |
| `battle/FriendlyBattleFlowIT` | richiesta di amicizia e accettazione via HTTP, invito, accettazione, dettaglio e storico dei due giocatori | esito opposto per i due, nessun invito in attesa, crediti, movimenti di `credit_transactions` e carte possedute invariati, nessuna riga in `player_ratings`, colonne di rating della partita nulle |
| `battle/FriendlyBattleFlowIT` | due retry nello stesso momento su una partita senza risultato | le due chiamate all'engine finto si aspettano con un `CountDownLatch`, quindi sono davvero sovrapposte, e rispondono con risultati **diversi**: la partita resta con il primo risultato salvato e entrambe le risposte mostrano quello. Togliendo il controllo di `BattleService.save` sulla partita già completata il test fallisce (provato il 22 novembre) |
| `battle/RankedBattleFlowIT` | ingresso in coda dei due giocatori via HTTP, abbinamento (`MatchmakingService.matchAll()`, il job è spento nei test), dettaglio, storico, `GET /rankings/me` e `GET /inventory` | sfidante chi è entrato prima, `ratingBefore` e `ratingDelta` opposti per i due (±20 con K = 40), rating, massimo e contatori aggiornati, nessuno resta in coda; `win_reward` solo a chi vince con una riga `BATTLE_WIN`; un pareggio tra K 20 e K 40 dà −3 e +6 (variazioni non opposte) e `draw_reward` a entrambi con `BATTLE_DRAW` |
| `battle/RankedBattleFlowIT` | engine spento all'abbinamento, poi retry dei due giocatori; due retry nello stesso momento | senza risultato nessun Elo; il retry usa il rating di adesso (non quello del ticket), il secondo retry non cambia niente; con i retry sovrapposti (stesso `CountDownLatch` delle amichevoli) Elo e crediti si assegnano una volta sola: una partita per giocatore, colonne di rating uguali ai rating applicati, una sola riga `BATTLE_WIN` |
| `battle/RankedBattleFlowIT` | retry dopo l'attivazione di un rule set con altre ricompense; pareggio salvato mentre gli stessi due giocatori accettano uno scambio | il retry paga il `win_reward` del rule set della partita; pareggio e scambio finiscono entrambi con `200`, senza deadlock, con i crediti di pareggio e la carta spostata. La sovrapposizione delle due transazioni non è forzata con un blocco: la garanzia è l'ordine dei blocchi (vedi [sfide](../sfide/sfide.md#crediti-delle-classificate-api-45-115)) |
| `pack/PackFlowIT` | registrazione e login via HTTP, crediti di benvenuto, acquisto di due *Base Pack* (quello vero di `V12`, cercato in `GET /packs`), due aperture, collezione | a ogni passo il saldo dell'API, `inventories.credits` e la somma di `credit_transactions` coincidono; righe `SIGNUP` e `PACK_PURCHASE`; ogni carta delle aperture è nella collezione; due righe in `pack_openings`; aprire non costa crediti; una terza apertura è `422 NO_PACK_TO_OPEN` |
| `pack/PackFlowRollbackIT` | acquisto e apertura che falliscono **dopo** la prima scrittura, con `@MockitoSpyBean` su `InventoryService` e `CollectionService` | dopo il `500` crediti, righe di `credit_transactions`, pacchetti, collezione e `pack_openings` sono quelli di prima, e lo stesso pacchetto poi si apre. Togliendo `@Transactional` da `PackService.purchase` il test fallisce (provato il 22 novembre). Gli spy cambiano la configurazione, quindi la classe ha un contesto Spring suo; stub e verifiche si fanno sullo spy dietro al proxy (`AopTestUtils.getUltimateTargetObject`) |

`FriendlyBattleFlowIT` e `RankedBattleFlowIT` hanno la stessa configurazione di `BattleControllerIT` (mock di `EmailSender` ed `EngineClient`), `TradeFlowIT` e `PackFlowIT` quella dei test degli endpoint (mock di `EmailSender`), così Spring riusa i contesti già avviati.

Consigliato: test di concorrenza sugli spostamenti di carte e crediti (due accettazioni incrociate in parallelo, uno scambio mentre si apre un pacchetto). Il pareggio in classificata mentre si accetta uno scambio è in `RankedBattleFlowIT` (API-45).

## Dati di test

Le immagini caricate dai test (API-14) vanno in `build/test-images`: il task `test` di `build.gradle` imposta `reversky.storage.images-dir`, così non toccano la cartella `uploads` di `bootRun`. `AdminCardImageControllerIT` prova gli endpoint, `ImageStorageIT` i file con le transazioni vere (file nuovo tolto se la transazione fallisce, vecchio tolto solo dopo il commit).

Classi di supporto in `src/test/java/.../support/` (`TestCards`, `TestDecks`, `TestUsers`, `TestPacks`) per creare dati di prova in una riga. Non duplicare la costruzione degli oggetti in ogni test.

`TestUsers` esiste già: crea utenti sul database dei test (`create()` per un `USER`, `createAdmin()` per un `ADMIN`, poi `disable`, `markEmailVerified`, `delete`) e contiene la password comune `TestUsers.PASSWORD`. Per chiamare un endpoint protetto basta `jwtService.createAccessToken(testUsers.createAdmin())`, senza passare dal login (esempio in `AdminFactionControllerIT`). È un `@TestComponent`, quindi si aggiunge al test insieme al container:

```java
@Import({TestcontainersConfiguration.class, TestUsers.class})
@SpringBootTest
class DeckServiceIT {

	@Autowired
	private TestUsers testUsers;

	// ... User user = testUsers.create();
}
```

`TestCards` esiste già: crea carte valide (`create(name, type)`, `create(name, type, rarity)`, `createCreature()`, `createVariantOf(original)`, `deactivate(card)`) e nomi univoci con `TestCards.uniqueName(...)`. Finché non c'è `POST /admin/cards` (API-14) è l'unico modo di creare carte nei test. L'effetto è un codice vero della tabella `effects` per il tipo della carta (`TestCards.effectCodeFor(type)`, o le costanti `CREATURE_EFFECT`, `SPELL_EFFECT`, `HIDDEN_EFFECT`, `TERRAIN_EFFECT`, `ITEM_EFFECT`): la chiave esterna `fk_catalog_cards_effect` (V13) rifiuta un codice inventato. **Non** è un `@TestComponent`: si crea nel test con `new TestCards(catalogCardRepository, rarityRepository, factionRepository)`.

Fazioni in `TestCards`: Creature, Terreni e Oggetti ricevono `faction()`, una fazione con nome univoco creata alla prima chiamata e poi riusata dalla stessa istanza; Spell e Hidden nessuna, come chiede `chk_catalog_cards_faction_matches_type`. Per una seconda fazione si usa `createFaction()`.

Rarità in `TestCards` (task #80):

- `common()`, `uncommon()` e `rare()` cercano la rarità con `sorting_order` 1, 2 e 3 e, se manca, la creano; con il seed di `V9` (#85) trovano quella del seed. `create(name, type)` usa Rare per Terreni e Oggetti e Common per gli altri tipi.
- I test **non** devono contare sui valori di queste tre rarità (`deck_limit`, `max_per_deck`), perché il seed li può cambiare: per una rarità diversa si usa `createRarity()`, con `sorting_order` casuale ≥ 1000 (quindi la ammettono solo le creature).
- Per lo stesso motivo i test sulle rarità (`RarityServiceIT`, `AdminRarityControllerIT`, `RarityControllerIT`) usano `sorting_order` casuali ≥ 1000 e lasciano liberi 1, 2 e 3. Così `@Import` resta `{TestcontainersConfiguration.class, TestUsers.class}` come negli altri IT e Spring riusa lo stesso contesto (e lo stesso container); con un `@Import` diverso ne avvierebbe un secondo. Esempi in `CatalogCardServiceIT`.

Le carte non si cancellano mai, quindi restano nel database tra un test e l'altro: le ricerche dei test filtrano per un token univoco nel nome (`TestUsers.uniqueSuffix()`).

`TestRuleSets` crea rule set di prova: `request(version)` con i valori del rule set `1.0`, `requestWithDeck(...)` per una composizione diversa, `toJson(request)` per i test HTTP, `create()` con una versione univoca (`uniqueVersion()`) e `markUsedByBattle(id)` per un rule set già usato da una partita. Come `TestCards` **non** è un `@TestComponent`: si crea con `new TestRuleSets(ruleSetService, ruleSetRepository, jdbcTemplate, testUsers)`.

`TestFactionBonuses` aiuta i test dei bonus di fazione: `createFaction()` crea una fazione con nome univoco, `currentMatrix()` legge la matrice nella forma del corpo del `PUT`, `toJson(rows)` la scrive per i test HTTP. Come `TestRuleSets` **non** è un `@TestComponent`: si crea con `new TestFactionBonuses(factionBonusService, factionService)`. La matrice è una sola per tutto il database e `GET /rules` la legge: un test che la modifica chiama `rememberMatrix()` in `@BeforeEach` e `restoreMatrix()` in `@AfterEach`. Esempi in `FactionBonusServiceIT`.

`TestNotifications` legge le notifiche di un utente per i test dei collegamenti (API-67…69): `types(user)` dalla più vecchia, `lastPayload(user, type)` come testo JSON e `clear(users...)` per togliere quelle nate dalla preparazione (un'amicizia creata con `FriendshipService` manda le sue); si crea con `new TestNotifications(jdbcTemplate)`.

`TestFriends` aiuta i test delle amicizie: `makeFriends(a, b)` e `sendRequest(a, b)` passano per `FriendshipService`, `countRowsBetween(a, b)` conta le righe tra due utenti in entrambe le direzioni, `createBattleInvite(challenger, opponent, status)` e `battleInviteExists(id)` scrivono e leggono `battle_invites` con SQL (con un mazzo vuoto dello sfidante) finché non esistono le entità delle sfide. Come `TestRuleSets` **non** è un `@TestComponent`: si crea con `new TestFriends(friendshipService, jdbcTemplate)`.

`TestCollections` aiuta i test della collezione: `give(user, card, quantity)` passa per `CollectionService.addCards` dentro una transazione, come farebbe il chiamante; `quantity(user, card)` legge le copie con SQL; `createDeck(user, cards)` e `createTradeOffer(requester, recipient, status, offerer, card, quantity)` scrivono mazzi e scambi con SQL finché non esistono le entità. Si crea con `new TestCollections(collectionService, transactionTemplate, jdbcTemplate)`. Il riepilogo si controlla su una rarità creata dal test, perché il catalogo cresce con gli altri test.

Il rule set attivo è uno solo per tutto il database e altre funzionalità lo leggono (crediti di benvenuto, mazzi, sfide). Un test che attiva o disattiva rule set chiama `rememberActive()` in `@BeforeEach` e `restoreActive()` in `@AfterEach`, così il rule set attivo prima del test (il `1.0` del seed di `V9`, #85) torna attivo per gli altri test. Esempi in `RuleSetServiceIT`.

`ReferenceDataMigrationIT` controlla i dati di `V9`: rarità, fazioni (id 1–4), rule set `1.0` e `GET /rules` senza bonus di fazione. Cerca le righe del seed per ordine, id o versione, perché gli altri test aggiungono rarità e fazioni.
