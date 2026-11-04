# Guida allo sviluppo: avviare e testare l'engine

Per chi inizia a lavorare su `reversky-engine`. Comandi per **Windows** (cmd o PowerShell); su macOS/Linux usa `./gradlew` al posto di `gradlew.bat`.
Tutti i comandi si lanciano dalla cartella `reversky-engine`, salvo dove indicato.

## 1. Primo giorno: controlli da fare

Ogni passo ha un risultato atteso: se non lo ottieni, fermati e guarda [Problemi comuni](#7-problemi-comuni).

| # | Comando | Risultato atteso |
|---|---|---|
| 1 | `java -version` | `openjdk version "25…"` e `Temurin` (installazione: [INSTALLAZIONE.md](../../../INSTALLAZIONE.md)) |
| 2 | `gradlew.bat test` | `BUILD SUCCESSFUL`. La prima volta scarica Gradle e le dipendenze (qualche minuto) |
| 3 | apri `build\reports\tests\test\index.html` | report dei test: per ora 1 test (`contextLoads`), 100% |
| 4 | `..\scripts\start-engine.bat` | nel log: `Tomcat started on port 8081` e `Started ReverskyEngineApplication` |
| 5 | in un altro terminale: `curl -i http://localhost:8081/` | `HTTP/1.1 404`: il server risponde, ma gli endpoint non esistono ancora |
| 6 | `Ctrl+C` nel terminale dell'engine | l'engine si ferma |

Quando i 6 passi funzionano sei pronto per la prima issue della [roadmap](../roadmap/roadmap.md).

## 2. Avviare l'engine

| Modo | Comando | Quando usarlo |
|---|---|---|
| Script | `scripts\start-engine.bat` (dalla cartella principale) | il più semplice |
| Gradle | `gradlew.bat bootRun` | uguale allo script |
| IDE | *Run* su `ReverskyEngineApplication.main()` | per il **debug** con i breakpoint |
| Docker | `docker compose up --build engine` (cartella principale) | per provarlo come in produzione, insieme all'API |

La porta è **8081** (`server.port` in `application.properties`). L'API usa la 8080: si possono avviare insieme.

Con Docker l'engine **non** è raggiungibile dal PC (`expose`, non `ports`): solo l'API lo vede. Per provarlo con curl o Swagger
scommenta le due righe `ports` del servizio `engine` in `compose.yaml` (non committare la modifica).

Con *devtools* attivo, quando ricompili dall'IDE l'engine si riavvia da solo.

## 3. Chiamare l'engine

Gli endpoint sono descritti in [api-engine](../api-engine/api-engine.md). Le richieste pronte sono in [`examples/`](../../examples/README.md),
ognuna con il risultato atteso.

```bat
:: Simulare una partita (cmd)
curl -s -X POST http://localhost:8081/api/v1/simulations -H "Content-Type: application/json" --data-binary @examples\08-partita-completa-seed-42.json

:: Effetti implementati (le regole non stanno nell'engine: arrivano nella richiesta)
curl -s http://localhost:8081/api/v1/effects
```

```powershell
# PowerShell: in PowerShell "curl" è un alias, usa curl.exe oppure Invoke-RestMethod
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/simulations `
       -ContentType 'application/json' -InFile examples\08-partita-completa-seed-42.json
$r.result; $r.endReason; $r.totalRounds
$r.log | Format-Table round, type, side, message
```

Con IntelliJ o VS Code (*REST Client*) apri [`examples/simulazioni.http`](../../examples/simulazioni.http) e lancia le richieste con ▶.

Quando c'è springdoc (issue `ENG-15`): Swagger UI su `http://localhost:8081/swagger-ui.html`.

## 4. Lanciare i test

| Cosa | Comando |
|---|---|
| Tutti i test | `gradlew.bat test` |
| Una classe | `gradlew.bat test --tests "*RoundResolverTest"` |
| Un solo metodo | `gradlew.bat test --tests "*RoundResolverTest.resolve_onlyOneCreatureDies*"` |
| Un package | `gradlew.bat test --tests "it.downfall.reversky.engine.core.effect.*"` |
| Rilanciare anche se nulla è cambiato | `gradlew.bat cleanTest test` |
| Rilanciare a ogni salvataggio | `gradlew.bat test --continuous` |
| Compilazione + test (prima della PR) | `gradlew.bat build` |
| Test di bilanciamento (lenti) | `gradlew.bat balanceTest` — dopo `ENG-28`, vedi sotto |

Dall'IDE: clic sulla freccia verde accanto alla classe o al metodo di test. Per il debug: *Debug* invece di *Run*.

Il report HTML è in `build\reports\tests\test\index.html`: per ogni test fallito mostra il messaggio di AssertJ e lo stack trace.

I test del `core` non avviano Spring e durano millisecondi. Solo i test `web` (`@WebMvcTest`) e `contextLoads` avviano Spring.

## 5. Scrivere un test: dal regolamento al codice

Si scrive **prima il test**, poi il codice che lo fa passare. Esempio completo per la prima issue (`ENG-04`, `FactionMatrix`):

```java
package it.downfall.reversky.engine.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FactionMatrixTest {

	private static final int WOUND = 1;   // Theory of the Wound
	private static final int DREAM = 4;   // Theory of the Dream

	private final FactionMatrix matrix = new FactionMatrix(List.of(
			new FactionBonus(WOUND, DREAM, 2, 0)));

	@Test
	@DisplayName("Una coppia con una riga di bonus restituisce il bonus della riga")
	void atkBonus_pairWithRow_returnsRowBonus() {
		assertThat(matrix.atkBonus(WOUND, DREAM)).isEqualTo(2);
	}

	@Test
	@DisplayName("La direzione opposta, senza riga, non ha bonus")
	void atkBonus_reversePairWithoutRow_returnsZero() {
		assertThat(matrix.atkBonus(DREAM, WOUND)).isZero();
	}

	@Test
	@DisplayName("Una fazione contro se stessa non ha bonus")
	void atkBonus_sameFaction_returnsZero() {
		assertThat(matrix.atkBonus(DREAM, DREAM)).isZero();
	}
}
```

Ricetta per ogni regola:

1. Trova la regola nel documento (`regole-base`, `fazioni`, `carte`, `effetti`).
2. Scegli i numeri più piccoli che la mettono alla prova, **calcola a mano** il risultato e scrivilo nel `@DisplayName`.
3. Scrivi anche la **controprova**: il caso in cui la regola *non* deve scattare.
4. `gradlew.bat test --tests "*NomeTest"` → deve fallire (rosso), poi scrivi il codice → verde.

## 6. Test di accettazione con gli esempi

Quando esiste il controller (`ENG-12`), gli esempi di `examples/` diventano un unico test parametrico: una riga di CSV = un test.
Se una regola cambia, si aggiorna il CSV, non il codice del test.

```java
@WebMvcTest(SimulationController.class)
@Import(EngineConfiguration.class)
class SimulationExamplesTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest(name = "{0}")
	@CsvFileSource(files = "examples/expected-results.csv", numLinesToSkip = 1)
	@DisplayName("Ogni esempio di examples/ dà il risultato calcolato a mano")
	void example_matchesExpectedResult(String file, int status, String result, String endReason,
			Integer totalRounds, Integer challengerLifePoints, Integer opponentLifePoints,
			Integer challengerDefeated, Integer opponentDefeated, String code) throws Exception {
		String body = Files.readString(Path.of("examples", file));

		ResultActions response = mockMvc.perform(post("/api/v1/simulations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().is(status));

		if (status == 200) {
			response.andExpect(jsonPath("$.result").value(result))
					.andExpect(jsonPath("$.endReason").value(endReason))
					.andExpect(jsonPath("$.totalRounds").value(totalRounds))
					.andExpect(jsonPath("$.challenger.finalLifePoints").value(challengerLifePoints))
					.andExpect(jsonPath("$.opponent.finalLifePoints").value(opponentLifePoints))
					.andExpect(jsonPath("$.challenger.creaturesDefeated").value(challengerDefeated))
					.andExpect(jsonPath("$.opponent.creaturesDefeated").value(opponentDefeated));
		} else {
			response.andExpect(jsonPath("$.code").value(code));
		}
	}
}
```

- Gli `import` li aggiunge l'IDE. In Spring Boot 4 `@WebMvcTest` è nel package `org.springframework.boot.webmvc.test.autoconfigure`.
- Il percorso `examples/...` è relativo alla cartella `reversky-engine`, che è la cartella di lavoro dei test con Gradle e con IntelliJ.
- Quando `HIDEBEHIND` è implementato, aggiungi un secondo metodo uguale che legge `expected-results-effects.csv`.

### Test di bilanciamento separati

Proposta per `ENG-28` (da confermare con chi gestisce `build.gradle`): i test con `@Tag("balance")` non partono con `test`, ma con un task a parte.

```groovy
tasks.named('test') {
	useJUnitPlatform {
		excludeTags 'balance'
	}
}

tasks.register('balanceTest', Test) {
	description = 'Simulazioni di bilanciamento (lente, si lanciano a mano)'
	group = 'verification'
	testClassesDirs = sourceSets.test.output.classesDirs
	classpath = sourceSets.test.runtimeClasspath
	useJUnitPlatform {
		includeTags 'balance'
	}
}
```

## 7. Problemi comuni

| Problema | Soluzione |
|---|---|
| `Toolchain … 25 not found` | `JAVA_HOME` non punta al JDK 25: correggilo e riapri terminale e IDE |
| `Port 8081 was already in use` | c'è già un engine acceso (un altro terminale o Docker): `docker compose down` o chiudi l'altro terminale |
| I test passano in IntelliJ ma non con Gradle (o viceversa) | *Settings → Build Tools → Gradle → Run tests using: Gradle* |
| `NoSuchFileException: examples\…` nel test | la cartella di lavoro non è `reversky-engine`: in IntelliJ, *Run configuration → Working directory* = `$MODULE_WORKING_DIR$` |
| `400` con `UNKNOWN_EFFECT` su una carta valida | l'effetto non è ancora registrato in `EffectRegistry.withAllEffects()` |
| Il test con seed fisso è cambiato dopo una modifica | hai aggiunto una chiamata a `random`: è normale se voluto, vedi [mescolamento](../mescolamento/mescolamento.md) |
| La partita in un test non finisce mai | manca il controllo del limite di round: esempio `06-stallo-limite-round` |

## 8. Prima di aprire una Pull Request

La checklist completa è in [CodingRequirements.md](../../CodingRequirements.md#13-checklist-prima-della-pull-request). In breve:

```bat
gradlew.bat build
```

deve finire con `BUILD SUCCESSFUL`, e il documento in `docs/` della regola che hai toccato deve essere aggiornato.
