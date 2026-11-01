# Mescolamento, seed e riproducibilità

## Il requisito

Dal brief: *il mescolamento deve avvenire lato server, in modo che nessun giocatore possa influenzarlo.*

In più vogliamo che ogni partita sia **riproducibile**: con gli stessi dati deve uscire esattamente la stessa partita. Serve per:
- verificare un bug segnalato da un giocatore;
- rigiocare una simulazione fallita senza cambiare il risultato;
- scrivere test con risultati prevedibili.

## Come funziona

1. L'**API** genera un seed casuale (`SecureRandom.nextLong()`) quando la sfida viene accettata, e lo salva.
2. L'API invia il seed all'engine insieme ai mazzi.
3. L'engine crea **un solo** generatore dal seed e lo usa per **tutta** la casualità della partita.

```java
RandomGenerator random = new Random(input.seed());

List<CardDefinition> challengerDeck = new ArrayList<>(input.challenger().deck());
List<CardDefinition> opponentDeck = new ArrayList<>(input.opponent().deck());

// Ordine fisso: prima il mazzo dello sfidante, poi quello dell'avversario.
Collections.shuffle(challengerDeck, random);
Collections.shuffle(opponentDeck, random);
```

Il giocatore non invia mai il seed e non vede il seed prima della partita: non può prevedere né scegliere l'ordine delle carte.

## Regole per non rompere la riproducibilità

| Vietato | Perché |
|---|---|
| `Math.random()`, `new Random()` senza seed, `ThreadLocalRandom` | introducono casualità non controllata |
| creare un secondo `Random` dal seed a metà partita | la sequenza dei numeri cambia |
| usare l'ora di sistema | cambia a ogni esecuzione |
| iterare su `HashMap` / `HashSet` quando l'ordine conta | l'ordine non è garantito |
| `parallelStream()` | l'ordine di esecuzione non è garantito |

Se un effetto ha bisogno di casualità (es. "sceglie una carta a caso"), usa lo **stesso** `RandomGenerator` della partita, accessibile da `EffectContext`.

> Attenzione: aggiungere una chiamata a `random` in un punto qualsiasi cambia tutte le estrazioni successive. Le partite **già giocate** non cambiano (sono salvate nell'API), ma i test con seed fisso potrebbero dover essere aggiornati: è normale, va solo verificato che il cambiamento sia voluto.

## Test obbligatorio

```java
@Test
@DisplayName("Stesso input e stesso seed producono la stessa identica partita")
void simulate_sameSeed_sameResult() {
	SimulationInput input = TestInputs.randomDecks(12345L);

	MatchResult first = simulator.simulate(input);
	MatchResult second = simulator.simulate(input);

	assertThat(second).isEqualTo(first);   // record: confronta anche tutto il log
}
```

E il controllo opposto: seed diversi → (quasi sempre) ordine diverso delle carte.
