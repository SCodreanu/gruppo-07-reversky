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
- Classi di supporto in `src/test/java/.../support/`: `TestCards` (creature pronte), `TestDecks`, `TestInputs`.

### Esempio: regola del danno al giocatore

```java
class RoundResolverTest {

	private final RuleConfig rules = RuleConfig.defaults();

	@Test
	@DisplayName("Se muore solo una creatura, il suo proprietario perde punti vita pari alla DEF negativa")
	void resolve_onlyOneCreatureDies_negativeDefenseDamagesOwner() {
		// Arrange: stessa fazione, nessun bonus
		MatchState state = TestStates.inCombat(
				TestCards.creature("A", TestFactions.SCP, 8, 6),   // sfidante
				TestCards.creature("B", TestFactions.SCP, 4, 5));  // avversario

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
@DisplayName("Hidebehind azzera solo il primo attacco subito")
void hidebehind_dodgesOnlyFirstAttack() {
	CreatureInPlay hidebehind = TestCards.inPlay("Hidebehind", 5, 6, new HidebehindEffect());
	EffectContext ctx = TestContexts.of(hidebehind);

	int first = hidebehind.effect().modifyIncomingDamage(ctx, 7);
	int second = hidebehind.effect().modifyIncomingDamage(ctx, 7);

	assertThat(first).isZero();
	assertThat(second).isEqualTo(7);
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
- [ ] bonus non validi nella richiesta (negativi, fazione contro se stessa) → `400`.

### Carte speciali
- [ ] Spell pescata: effetto applicato subito, carta scartata, si continua a pescare;
- [ ] Hidden pescata: messa coperta, si attiva alla sua condizione;
- [ ] Terreno pescato: resta in gioco fino alla fine e dà il bonus alle creature della sua fazione.

### Determinismo
- [ ] stesso seed → stesso `MatchResult`, log compreso;
- [ ] seed diverso → ordine delle carte diverso.

### Effetti
- [ ] ogni effetto: si attiva quando deve, non si attiva quando non deve, non agisce se annullato.

### Web
- [ ] richiesta valida → `200` con la struttura attesa;
- [ ] `effectCode` sconosciuto → `400`;
- [ ] mazzo senza creature → `400`.

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
