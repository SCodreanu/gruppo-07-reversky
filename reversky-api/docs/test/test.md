# Test

## Perché

Le parti più rischiose del backend (crediti, pacchetti, scambi) spostano "valore" degli utenti. Un bug lì significa carte duplicate o crediti persi. I test sono la garanzia che non succeda.

## Tipi di test

| Tipo | Cosa testa | Strumenti | Nome file |
|---|---|---|---|
| **Unitario** | una classe da sola (service, validator) | JUnit 5, Mockito, AssertJ | `XxxTest` |
| **Controller** | endpoint HTTP: status, JSON, sicurezza | `@WebMvcTest`, MockMvc | `XxxControllerTest` |
| **Repository** | query JPA personalizzate | `@DataJpaTest` + Testcontainers PostgreSQL | `XxxRepositoryTest` |
| **Integrazione** | un flusso completo con database vero | `@SpringBootTest` + Testcontainers | `XxxIT` |

Testcontainers avvia un PostgreSQL vero in Docker (configurazione in `TestcontainersConfiguration`): **serve Docker acceso** per eseguire i test di repository e integrazione.

## Comandi

```bash
./gradlew test                              # tutti i test
./gradlew test --tests "*DeckValidatorTest" # una sola classe
```

## Come si scrive un test

- Nome che descrive il comportamento: `metodo_condizione_risultatoAtteso`, oppure `@DisplayName` in italiano.
- Struttura **Arrange / Act / Assert**, separata da una riga vuota.
- Un test verifica **una** cosa.
- Asserzioni con AssertJ (`assertThat`), più leggibili.

```java
class DeckValidatorTest {

	private final DeckValidator validator = new DeckValidator(DeckRules.defaults());

	@Test
	@DisplayName("Un mazzo con 4 copie della stessa Common non è valido")
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

## Cosa deve essere coperto

Obbligatorio:
- [ ] ogni regola di `DeckValidator`, compreso `min(deck_limit, copy_limit)`;
- [ ] acquisto pacchetto: crediti scalati, crediti insufficienti, saldo mai negativo;
- [ ] apertura pacchetto: numero e rarità delle carte, bonus, filtro dei tag, nessun pacchetto da aprire;
- [ ] scambi: tutti gli stati e i turni, controproposte, carte non più possedute, **totale delle carte invariato**;
- [ ] amicizie: richiesta inversa, richiesta dopo un rifiuto;
- [ ] sfide: stati dell'invito, mazzo non più valido, engine non disponibile, retry con lo stesso seed;
- [ ] autenticazione: rotazione del refresh token, reset password che invalida i refresh token, account disattivato;
- [ ] backoffice: ogni azione scrive in `audit_logs`;
- [ ] sicurezza: endpoint protetti senza token → `401`, admin con `USER` → `403`, risorse altrui → `404`.

Consigliato: un test di concorrenza sugli scambi (due accettazioni incrociate in parallelo).

## Dati di test

Classi di supporto in `src/test/java/.../support/` (`TestCards`, `TestDecks`, `TestUsers`, `TestPacks`) per creare dati di prova in una riga. Non duplicare la costruzione degli oggetti in ogni test.
