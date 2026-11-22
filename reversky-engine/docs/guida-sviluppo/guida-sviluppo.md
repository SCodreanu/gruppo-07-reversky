# Guida allo sviluppo: avviare e testare l'engine

Per chi inizia a lavorare su `reversky-engine`. Comandi per **Windows** (cmd o PowerShell); su macOS/Linux usa `./gradlew` al posto di `gradlew.bat`.
Tutti i comandi si lanciano dalla cartella `reversky-engine`, salvo dove indicato.

## 1. Primo giorno: controlli da fare

Ogni passo ha un risultato atteso: se non lo ottieni, fermati e guarda [Problemi comuni](#7-problemi-comuni).

| # | Comando | Risultato atteso |
|---|---|---|
| 1 | `java -version` | `openjdk version "25…"` e `Temurin` (installazione: [INSTALLAZIONE.md](../../../INSTALLAZIONE.md)) |
| 2 | `gradlew.bat test` | `BUILD SUCCESSFUL`. La prima volta scarica Gradle e le dipendenze (qualche minuto) |
| 3 | apri `build\reports\tests\test\index.html` | report dei test: nessun fallimento; alcuni test sono *saltati* in attesa degli effetti (vedi [§6](#6-test-di-accettazione-con-gli-esempi)) |
| 4 | `..\scripts\start-engine.bat` | nel log: `Tomcat started on port 8081` e `Started ReverskyEngineApplication` |
| 5 | in un altro terminale: `curl -i -X POST http://localhost:8081/api/v1/simulations -H "Content-Type: application/json" --data-binary @examples\01-base-esempio-regolamento.json` | `HTTP/1.1 200` e un JSON con `"result":"CHALLENGER_WIN"` |
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
scommenta le due righe `ports` del servizio `engine` in `compose.yaml` (non committare la modifica), oppure avvia un container
usa e getta con la sola immagine dell'engine, senza toccare `compose.yaml` (dalla cartella principale):

```bat
docker build -t reversky-engine reversky-engine
docker run --rm --name reversky-engine -p 8081:8081 reversky-engine
```

`--rm` cancella il container quando lo fermi (`Ctrl+C` o `docker stop reversky-engine`); l'immagine si toglie con `docker rmi reversky-engine`.

Con *devtools* attivo, quando ricompili dall'IDE l'engine si riavvia da solo.

## 3. Chiamare l'engine

Gli endpoint sono descritti in [api-engine](../api-engine/api-engine.md). Le richieste pronte sono in [`examples/`](../../examples/README.md),
ognuna con il risultato atteso.

```bat
:: Simulare una partita (cmd)
curl -s -X POST http://localhost:8081/api/v1/simulations -H "Content-Type: application/json" --data-binary @examples\08-partita-completa-seed-42.json
```

```powershell
# PowerShell: in PowerShell "curl" è un alias, usa curl.exe oppure Invoke-RestMethod
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/simulations `
       -ContentType 'application/json' -InFile examples\08-partita-completa-seed-42.json
$r.result; $r.endReason; $r.totalRounds
$r.log | Format-Table round, type, side, message
```

Con IntelliJ o VS Code (*REST Client*) apri [`examples/simulazioni.http`](../../examples/simulazioni.http) e lancia le richieste con ▶.

Swagger UI su `http://localhost:8081/swagger-ui.html` (springdoc, D-41), il documento OpenAPI su `http://localhost:8081/v3/api-docs`.
In *Try it out* scegli l'esempio `basic` (esempio 01) o `full` (esempio 08, seed 42) e premi *Execute*: la risposta compare sotto, nella tabella *Server response*, dopo il riquadro *Curl*.

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
| Test di bilanciamento (lenti) | `gradlew.bat balanceTest` e `gradlew.bat effectTest`, vedi sotto |

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

	// La matrice lavora su liste di fazioni, una o due per carta (docs/fazioni, "Carte con due fazioni")
	private static final List<Integer> ONLY_WOUND = List.of(WOUND);
	private static final List<Integer> ONLY_DREAM = List.of(DREAM);

	private final FactionMatrix matrix = new FactionMatrix(List.of(
			new FactionBonus(WOUND, DREAM, 2, 0)));

	@Test
	@DisplayName("Una coppia con una riga di bonus restituisce il bonus della riga")
	void atkBonus_pairWithRow_returnsRowBonus() {
		assertThat(matrix.atkBonus(ONLY_WOUND, ONLY_DREAM)).isEqualTo(2);
	}

	@Test
	@DisplayName("La direzione opposta, senza riga, non ha bonus")
	void atkBonus_reversePairWithoutRow_returnsZero() {
		assertThat(matrix.atkBonus(ONLY_DREAM, ONLY_WOUND)).isZero();
	}

	@Test
	@DisplayName("Una fazione contro se stessa non ha bonus")
	void atkBonus_sameFaction_returnsZero() {
		assertThat(matrix.atkBonus(ONLY_DREAM, ONLY_DREAM)).isZero();
	}
}
```

Ricetta per ogni regola:

1. Trova la regola nel documento (`regole-base`, `fazioni`, `carte`, `effetti`).
2. Scegli i numeri più piccoli che la mettono alla prova, **calcola a mano** il risultato e scrivilo nel `@DisplayName`.
3. Scrivi anche la **controprova**: il caso in cui la regola *non* deve scattare.
4. `gradlew.bat test --tests "*NomeTest"` → deve fallire (rosso), poi scrivi il codice → verde.

## 6. Test di accettazione con gli esempi

Gli esempi di `examples/` sono un unico test parametrico, [`SimulationExamplesTest`](../../src/test/java/it/downfall/reversky/engine/web/SimulationExamplesTest.java): una riga di CSV = un test, inviata al controller con MockMvc.
Se una regola cambia, si aggiorna il CSV, non il codice del test.

- Un metodo legge `examples/expected-results.csv`, un altro `examples/expected-results-effects.csv` (esempi con effetti).
- Con `200` controlla risultato, motivo di fine, round, punti vita e creature sconfitte; con `400` controlla `code`.
- Il percorso `examples/...` è relativo alla cartella `reversky-engine`, che è la cartella di lavoro dei test con Gradle e con IntelliJ.
- `@WebMvcTest` non carica i `@Component`: il test importa `EngineConfiguration` e `SimulationMapper`.
- Da `ENG-23` non si salta più nessun esempio: il 90 dà `UNKNOWN_EFFECT` anche su una creatura e il 09 usa `HIDEBEHIND`.

### Test di bilanciamento separati

Dal 20 novembre (`ENG-28`) i test con `@Tag("balance")` non partono con `test` né con `build`, ma con il task `balanceTest` di `build.gradle`:

```bash
gradlew.bat balanceTest    # alcuni minuti; scrive docs/bilanciamento/report-bilanciamento.md e report-carte.csv
gradlew.bat balanceTest -PbalanceGrid=forme   # forme del bonus di D-04: report-forme-bonus.md e il suo CSV
gradlew.bat balanceTest -PbalanceGrid=pv30    # tre bonus a 30 PV (ENG-42): report-30pv.md e il suo CSV
```

Il task si rilancia sempre (non è mai "up to date") e mostra l'avanzamento per configurazione. Come sono fatti mazzi, seed, configurazioni e avvisi: [test](../test/test.md#test-di-bilanciamento-eng-28).

Dal 21 novembre (`ENG-42`) anche i test con `@Tag("effects")` sono separati, con il task `effectTest`:

```bash
gradlew.bat effectTest     # valore di ogni effetto a partite appaiate: report-effetti.md e report-effetti-carte.csv
```

Come si misura e come si giudica un effetto: [test](../test/test.md#valore-degli-effetti-eng-42-fase-2).

## 7. Problemi comuni

| Problema | Soluzione |
|---|---|
| `Toolchain … 25 not found` | `JAVA_HOME` non punta al JDK 25: correggilo e riapri terminale e IDE |
| `Port 8081 was already in use` | c'è già un engine acceso (un altro terminale o Docker): `docker compose down` o chiudi l'altro terminale |
| I test passano in IntelliJ ma non con Gradle (o viceversa) | *Settings → Build Tools → Gradle → Run tests using: Gradle* |
| `NoSuchFileException: examples\…` nel test | la cartella di lavoro non è `reversky-engine`: in IntelliJ, *Run configuration → Working directory* = `$MODULE_WORKING_DIR$` |
| `400` con `UNKNOWN_EFFECT` su una carta valida | l'effetto non è ancora registrato in `EffectRegistry.withAllEffects()`, oppure il codice è di un altro tipo di carta (es. il codice di un Terreno su una Spell). Nei test si può usare `TestEffects.stubsFor(...)` |
| Il test con seed fisso è cambiato dopo una modifica | hai aggiunto una chiamata a `random`: è normale se voluto, vedi [mescolamento](../mescolamento/mescolamento.md) |
| La partita in un test non finisce mai | manca il controllo del limite di round: esempio `06-stallo-limite-round` |

## 8. Prima di aprire una Pull Request

La checklist completa è in [CodingRequirements.md](../../CodingRequirements.md#13-checklist-prima-della-pull-request). In breve:

```bat
gradlew.bat build
```

deve finire con `BUILD SUCCESSFUL`, e il documento in `docs/` della regola che hai toccato deve essere aggiornato.
