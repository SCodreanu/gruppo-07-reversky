# Test

## Perché qui sono fondamentali

Il brief chiede esplicitamente che il motore sia **coperto da test automatici JUnit**. Ed è la parte dove un errore non si vede: una partita sbagliata sembra comunque una partita.

## Tipi di test

| Tipo | Cosa testa | Strumenti | Esempio |
|---|---|---|---|
| **Regole** | un singolo passo (round, danni, fine partita, fazioni) | JUnit 5 + AssertJ | `RoundResolverTest` |
| **Effetti** | un effetto alla volta | JUnit 5 + AssertJ | `HidebehindEffectTest` |
| **Partita completa** | `MatchSimulator` dall'inizio alla fine | JUnit 5 | `MatchSimulatorTest` |
| **Web** | JSON, validazione, status HTTP | `@WebMvcTest` | `SimulationControllerTest` |
| **Bilanciamento** (facoltativo) | migliaia di partite simulate | JUnit 5, `@Tag("balance")` | `BalanceSimulationTest` |

I test del `core` sono **JUnit puro**: niente `@SpringBootTest`, niente contesto Spring. Partono in millisecondi.

## Comandi

```bash
./gradlew test                                  # tutti
./gradlew test --tests "*HidebehindEffectTest"  # uno solo
```

## Come si scrive

- Nome che descrive la regola: `metodo_condizione_risultatoAtteso` o `@DisplayName` in italiano.
- Struttura **Arrange / Act / Assert**.
- Mazzi **non mescolati** nei test di regola: si costruiscono in un ordine preciso così si sa chi pesca cosa. Per i test di partita completa si usa un seed fisso.
- Classi di supporto in `src/test/java/.../support/`: `TestRules` (rule set di prova), `TestCards` (creature pronte), `TestDecks`, `TestInputs`.

### Esempio: regola del danno al giocatore

```java
class RoundResolverTest {

	private final RuleConfig rules = TestRules.standard();   // 30 PV, 75 round, come il rule set 1.0

	@Test
	@DisplayName("Se muore solo una creatura, il suo proprietario perde punti vita pari alla DEF negativa")
	void resolve_onlyOneCreatureDies_negativeDefenseDamagesOwner() {
		// Arrange: stessa fazione, nessun bonus
		MatchState state = TestStates.inCombat(
				TestCards.creature("A", TestFactions.ARCHIVE, 8, 6),   // sfidante
				TestCards.creature("B", TestFactions.ARCHIVE, 4, 5));  // avversario

		// Act
		new RoundResolver(rules, TestFactions.noBonuses()).resolve(state);

		// Assert
		assertThat(state.challenger().creature().defense()).isEqualTo(2);
		assertThat(state.opponent().creature()).isNull();          // B è morta con DEF −3
		assertThat(state.opponent().lifePoints()).isEqualTo(30 - 3);
	}
}
```

### Esempio: effetto

```java
@Test
@DisplayName("Hidebehind previene 2 danni dal primo attacco di ogni creatura avversaria")
void hidebehind_preventsTwoDamageOnFirstAttackOfEachOpponent() {
	CreatureInPlay hidebehind = TestCards.inPlay("Hidebehind", 5, 6, new HidebehindEffect());
	CreatureInPlay faceling = TestCards.inPlay("Faceling", 7, 9);
	CreatureInPlay dullers = TestCards.inPlay("Dullers", 7, 9);

	int firstFromFaceling = hidebehind.effect().modifyIncomingDamage(TestContexts.of(hidebehind, faceling), 7);
	int secondFromFaceling = hidebehind.effect().modifyIncomingDamage(TestContexts.of(hidebehind, faceling), 7);
	int firstFromDullers = hidebehind.effect().modifyIncomingDamage(TestContexts.of(hidebehind, dullers), 7);

	assertThat(firstFromFaceling).isEqualTo(5);
	assertThat(secondFromFaceling).isEqualTo(7);
	// nuova avversaria → lo scudo vale di nuovo
	assertThat(firstFromDullers).isEqualTo(5);
}
```

## Cosa deve essere coperto (obbligatorio)

### Regolamento base
- [ ] attacco simultaneo: entrambe le DEF diminuiscono nello stesso round;
- [ ] DEF ≤ 0 → morte; DEF = 1 → vive;
- [ ] nessuna muore → nuovo round con le stesse creature e la DEF ridotta;
- [ ] muoiono entrambe → nessun danno ai giocatori;
- [ ] muore una sola → il proprietario perde esattamente la DEF negativa; DEF finale 0 → nessun danno;
- [ ] la creatura sopravvissuta resta in campo con la DEF residua;
- [ ] tutte le condizioni di fine partita e il loro ordine;
- [ ] due creature con ATK 0 → `ROUND_LIMIT`, nessun loop infinito.

### Fazioni
- [ ] coppia con riga in `factionBonuses` → bonus ATK e DEF della riga; senza riga → 0;
- [ ] bonus DEF applicato una sola volta per scontro;
- [ ] malus −1: l'attacco effettivo non scende sotto 0;
- [ ] carta con due fazioni: somma di tutte le coppie, in attacco e in difesa (esempi `11` e `12`);
- [ ] bonus non validi nella richiesta (fazione contro se stessa, `defBonus` negativo) → `400`; `atkBonus` negativo accettato;
- [ ] `secondaryFactionId` uguale a `factionId` → `400 INVALID_CARD_FACTIONS`.

### Carte speciali
- [ ] Spell pescata: effetto applicato subito, carta scartata, si continua a pescare;
- [ ] Hidden pescata: messa coperta; senza condizione si attiva all'inizio del prossimo scontro del proprietario, con una condizione quando la condizione si verifica (D-15);
- [ ] Terreno pescato: dà il bonus alle creature della sua fazione di entrambi i giocatori; un secondo Terreno, di chiunque, lo sostituisce e i suoi bonus si tolgono (D-17, D-19);
- [ ] Oggetto pescato: resta accanto al mazzo, scatta una volta sulla prima creatura del proprietario che ne soddisfa la condizione, poi si consuma (D-47, `ENG-33`);
- [ ] DEF temporanea: assorbe i danni per prima; alla scadenza su una creatura danneggiata la creatura non muore (D-35).

### Determinismo
- [ ] stesso seed → stesso `MatchResult`, log compreso;
- [ ] seed diverso → ordine delle carte diverso.

### Effetti
- [ ] ogni effetto: si attiva quando deve, non si attiva quando non deve, non agisce se annullato.

### Web
- [ ] richiesta valida → `200` con la struttura attesa;
- [ ] `effectCode` sconosciuto → `400`;
- [ ] mazzo senza creature → `400`.
- [ ] tutti gli esempi di `examples/expected-results.csv` danno il risultato atteso (`SimulationExamplesTest`).

## Test di accettazione con gli esempi

La cartella [`examples/`](../../examples/README.md) contiene richieste JSON con il risultato **calcolato a mano**, e un CSV con i valori attesi.
Un solo test parametrico (`@ParameterizedTest` + `@CsvFileSource`) le invia tutte al controller e controlla risultato, motivo di fine, round, punti vita e codice d'errore.
Il codice completo del test è nella [guida allo sviluppo](../guida-sviluppo/guida-sviluppo.md#6-test-di-accettazione-con-gli-esempi).

Servono a due cose: dimostrano che regole e contratto REST funzionano insieme, e sono gli stessi file che l'API può usare per il proprio test di contratto.

## Test di bilanciamento (facoltativo, molto utile)

Si fanno giocare migliaia di partite tra mazzi casuali o tra mazzi "tipo" (uno per fazione) e si contano le vittorie. Serve ai DSG per il bilanciamento delle carte e a decidere il limite di round.

```java
@Tag("balance")
@Test
void factionsWinRateIsBalanced() {
	Map<Faction, Integer> wins = ...;
	for (long seed = 0; seed < 10_000; seed++) { ... }
	// nessuna fazione sopra il 60% di vittorie contro le altre
}
```

Questi test sono lenti: si escludono dalla build normale e si lanciano a mano.

Altre domande a cui rispondono:
- quante partite finiscono per `ROUND_LIMIT`? (se sono tante, il limite è troppo basso);
- quanto dura in media una partita?
- c'è una carta che vince troppo spesso quando è in campo?
