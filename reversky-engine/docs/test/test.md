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
| **Bilanciamento** (`ENG-28`) | migliaia di partite simulate, report per i DSG | JUnit 5, `@Tag("balance")`, `gradlew balanceTest` | `BalanceSimulationTest` |
| **Valore degli effetti** (`ENG-42`) | ogni effetto tolto e rigiocato a partite appaiate | JUnit 5, `@Tag("effects")`, `gradlew effectTest` | `EffectAblationSimulationTest` |

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
- Classi di supporto in `src/test/java/.../support/`:
  - `TestRules`: rule set di prova (`standard()` = 30 punti vita, 75 round; `of(...)` per valori scelti dal test);
  - `TestFactions`: id delle quattro fazioni come negli esempi (`WOUND` 1, `INVITATION` 2, `ARCHIVE` 3, `DREAM` 4), bonus di D-04 (`standardBonuses()`, `standardMatrix()`) e `noBonuses()`;
  - `TestCards`: creature, Spell, Hidden, Terreni e Oggetti in una riga (`cardId` 0 se non serve un id);
  - `TestDecks`: mazzi piccoli in ordine (`of(...)`) e un mazzo completo da 30 carte per fazione con le carte del catalogo (`full(fazione)`), controllato da `TestDecksTest`;
  - `TestInputs`: `SimulationInput` con regole, fazioni e bonus standard;
  - `TestStates`: `inCombat(...)` (due creature già in campo) e `withDecks(...)` (prima della pescata iniziale);
  - `TestEffects`: `stubsFor(...)`, un registro con un effetto finto, che non fa niente, per ogni codice di Spell, Hidden, Terreni e Oggetti delle carte date; `gameWithStubsFor(input)`, il registro del gioco più un effetto finto solo per i codici che il gioco non ha ancora. Servono ai test con carte il cui effetto non è ancora implementato;
  - `TestCards.inPlay(carta, lato)`: una creatura già in campo.

### Esempio: regola del danno al giocatore

```java
class RoundResolverTest {

	@Test
	@DisplayName("Se muore solo una creatura, il suo proprietario perde punti vita pari alla DEF negativa")
	void resolve_onlyOneCreatureDies_negativeDefenseDamagesOwner() {
		// Arrange: stessa fazione, nessun bonus
		MatchState state = TestStates.inCombat(
				TestCards.creature("A", TestFactions.ARCHIVE, 8, 6),   // sfidante
				TestCards.creature("B", TestFactions.ARCHIVE, 4, 5));  // avversario

		// Act
		new RoundResolver(TestFactions.noBonuses(), EffectRegistry.withAllEffects()).resolve(state);

		// Assert
		assertThat(state.challenger().creature().orElseThrow().defense()).isEqualTo(2);
		assertThat(state.opponent().creature()).isEmpty();         // B è morta con DEF −3
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
- [ ] una sola Hidden attiva per giocatore: la seconda aspetta in fila e diventa attiva dal prossimo scontro dopo che la prima si scarta, anche se annullata (`ENG-41`);
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
Il test è `SimulationExamplesTest`; come funziona e quali esempi sono ancora saltati è nella [guida allo sviluppo](../guida-sviluppo/guida-sviluppo.md#6-test-di-accettazione-con-gli-esempi).

Servono a due cose: dimostrano che regole e contratto REST funzionano insieme, e sono gli stessi file che l'API può usare per il proprio test di contratto.

## Test di bilanciamento (`ENG-28`)

Migliaia di partite simulate tra mazzi casuali di una fazione, per decidere i valori delle carte e delle regole: il report va ai DSG.
Sono lenti: hanno il tag `balance`, sono esclusi da `gradlew build` e `gradlew test` e si lanciano a mano.

```bash
./gradlew balanceTest       # Windows: gradlew.bat balanceTest; alcuni minuti
```

Il codice è in `src/test/java/.../balance/`; solo `BalanceSimulationTest` ha il tag, le altre classi hanno test veloci che girano nella build normale (`CatalogCsvTest`, `BalanceDeckBuilderTest`, `BalanceStatsTest`, `BalanceReportTest`, `BalanceRunnerTest`).
Il report si scrive in `docs/bilanciamento/` (cartella diversa con `-Dbalance.outputDir`, già impostata dal task):

- [`report-bilanciamento.md`](../bilanciamento/report-bilanciamento.md): sintesi, fazioni, scontri diretti, durata, motivi di fine, rarità, avvisi;
- [`report-carte.csv`](../bilanciamento/report-carte.csv): una riga per carta e configurazione.

Il report è deterministico: con lo stesso engine e lo stesso catalogo esce identico, quindi `git diff` mostra solo quello che una modifica ha spostato. Non si modifica a mano.

Scelte decise il 20 novembre:

| Cosa | Scelta |
|---|---|
| Catalogo | `docs/bilanciamento/catalogo-finale.csv` (`CatalogCsv`); id = numero di riga, nome com'è nel CSV; Spell e Hidden contano come Uncommon |
| Mazzi | di una sola fazione, nuovi a ogni partita: 20 creature della fazione (le Mutant valgono per entrambe), Terreno e Oggetto della fazione, 6 Spell, 2 Hidden. Ogni carta si sceglie a caso, con la stessa probabilità, tra quelle ancora ammesse (`BalanceDeckBuilder`) |
| Limiti dei mazzi | quelli dell'API, ancora proposte (Q-02, Q-03): copie Common 3, Uncommon 2, Rare 2, Mythic, Legendary e Mutant 1; massimo 2 Legendary, 2 Mythic, 1 Mutant, 5 Rare contando Terreno e Oggetto; al massimo una Hidden `DAMAGE` |
| Partite | 16 accoppiamenti ordinati di fazioni (speculari compresi) × 2.500 = 40.000 per configurazione |
| Seed | partita `i` dell'accoppiamento `p`: `20261120 + p × 2500 + i`, per costruire i mazzi e per il mescolamento; uguale in tutte le configurazioni, così cambiano solo le regole. I mazzi si costruiscono con uno `SplittableRandom` del seed, non correlato al `java.util.Random` con cui l'engine mescola |
| Configurazioni | griglia 2 × 2: 30 e 25 punti vita (D-01) × bonus D-04 (+2/−1 ATK) e bonus di prova del 13 novembre (+1 DEF contro la fazione che si batte); 75 round (D-02) |
| Punteggio | vittoria 1, pareggio ½, sconfitta 0 |
| Fazioni | senza le partite speculari; scontri diretti nei due posti |
| Carte | punteggio "nel mazzo" e "giocata" (creatura entrata, Spell risolta, Hidden attivata, Terreno o Oggetto messo), una volta per lato e partita; attivazioni per partita giocata |
| Gruppi | rarità per le creature, tipo per le altre carte; media del gruppo pesata sulle partite. Il punteggio da giocata è spostato verso il basso (chi perde gioca più carte): la banda delle carte lo usa, perché il confronto dentro un gruppo resta valido; la scala delle rarità usa il punteggio nel mazzo |
| Invarianti (fanno fallire il test) | nessun errore dell'engine, ogni partita finisce con `MATCH_ENDED` entro il limite di round, le prime 200 partite di ogni configurazione danno lo stesso risultato due volte |
| Avvisi (solo nel report) | scontro di vantaggio fuori da 55–65%, scontro neutro e fazione fuori da 45–55%, `ROUND_LIMIT` sopra il 5%, primo giocatore fuori da 45–55%, carta a più di 10 punti dalla media del gruppo con almeno 500 partite giocata (sotto: "campione basso"), scala delle rarità delle creature non crescente (nel mazzo); quota delle partite finite per punti vita mostrata senza avviso |

### Forme del bonus di fazione (D-04)

La griglia `forme` (`BalanceGrid.FORME`, scelta il 20 novembre) prova altre forme del bonus con gli stessi mazzi e seed della griglia base, quindi cambia solo il bonus. Si lancia a parte e scrive un report suo, [`report-forme-bonus.md`](../bilanciamento/report-forme-bonus.md) con [`report-forme-bonus-carte.csv`](../bilanciamento/report-forme-bonus-carte.csv):

```bash
./gradlew balanceTest -PbalanceGrid=forme   # Windows: gradlew.bat balanceTest -PbalanceGrid=forme
```

Le forme usano solo righe intere di `faction_bonuses`, senza modifiche all'engine:

- **+1 ATK contro +1 DEF:** chi batte l'avversaria ha +1 ATK in ogni round dello scontro; chi è battuto ha +1 DEF una volta per scontro contro chi lo batte.
- **Per coppia:** come sopra, ma +2 DEF invece di +1 a Wound contro Archive e a Invitation contro Dream, le due coppie che restavano alte.

Risultati del 20 novembre, con le statistiche del catalogo **prima** di `ENG-42` (i report di oggi usano quelle nuove, vedi sotto). Scontri di vantaggio (la prima fazione contro quella che batte) e partite finite per punti vita, con 30 PV / 25 PV; in grassetto quelli dentro 55–65%:

| Forma | Wound–Dream | Invitation–Archive | Archive–Wound | Dream–Invitation | Fazioni fuori da 45–55% | Finite per PV |
|---|---|---|---|---|---|---|
| D-04, +2/−1 ATK (griglia base) | 99,8 / 99,8 | 100,0 / 99,9 | 100,0 / 99,9 | 100,0 / 100,0 | nessuna / nessuna | 53,6 / 72,9 |
| Bonus di prova, +1 DEF (griglia base) | 79,9 / 77,3 | 78,2 / 74,3 | 90,2 / 84,6 | 89,8 / 88,7 | Invitation 44,6, Archive 55,5 / Invitation 42,5 | 28,8 / 54,0 |
| +1 ATK contro +1 DEF | **57,2 / 61,2** | **58,2 / 58,0** | 71,6 / 67,9 | 75,5 / 76,0 | Invitation 42,5, Archive 56,1 / Invitation 41,3 | 32,3 / 57,5 |
| Per coppia (+2 DEF a Wound e Invitation) | **57,2 / 61,2** | **58,2 / 58,0** | 30,6 / 33,2 | 41,6 / 47,7 | Wound 60,6, Archive 42,4, Dream 43,2 / Wound 62,0, Archive 43,2, Dream 44,1 | 30,9 / 55,9 |

Cosa dicono:

- Nessuna forma con righe intere mette tutti e quattro gli scontri di vantaggio in 55–65%. +1 ATK contro +1 DEF è la più vicina: Wound–Dream e Invitation–Archive ci stanno, Archive–Wound e Dream–Invitation restano al 68–76%.
- Un punto di DEF in più a chi è battuto sposta lo scontro di 28–41 punti (Archive–Wound da 71,6 a 30,6, Dream–Invitation da 75,5 a 41,6 con 30 PV): il passo intero è troppo grosso per una banda di 10 punti. Per stringere ancora servono una forma che l'engine oggi non ha (per esempio il bonus ATK solo nel primo round dello scontro) o una ritaratura del catalogo.
- Invitation sta al 41–45% contro le altre con il bonus di prova e con +1 ATK contro +1 DEF, e Dream la batte al 75–76% anche con il bonus più piccolo: il problema è anche nelle carte di Invitation, non solo nel bonus. Lo scontro neutro Wound–Invitation esce dalla banda con 25 PV in tutte le forme (58,0–58,2%).
- D-01: con +1 ATK contro +1 DEF finisce per punti vita il 57,5% delle partite con 25 PV e il 32,3% con 30 PV; come il bonus di prova, questa forma va con 25 PV.

D-04 e D-01 restano aperti: le forme sono proposte per i DSG, non decisioni.

### Ribilanciamento delle statistiche (`ENG-42`)

`ENG-42` porta gli scontri nelle bande con il bonus +1 ATK contro +1 DEF, cambiando il catalogo. La fase 1 (20 novembre) tocca solo ATK e DEF delle creature; la fase 2 rivede i numeri degli effetti carta per carta, prima del code freeze. Il risultato della fase 1 è una **proposta** per i DSG, non una decisione: la fase 2 può spostarlo di nuovo.

La griglia `pv30` (`BalanceGrid.PV30`) gioca, con gli stessi mazzi e seed della griglia base, le tre forme del bonus a 30 PV (D-04 +2/−1 ATK, bonus di prova +1 DEF, +1 ATK contro +1 DEF) e scrive [`report-30pv.md`](../bilanciamento/report-30pv.md) con [`report-30pv-carte.csv`](../bilanciamento/report-30pv-carte.csv):

```bash
./gradlew balanceTest -PbalanceGrid=pv30   # Windows: gradlew.bat balanceTest -PbalanceGrid=pv30
```

Metodo, scelto il 20 novembre:

- **Prima le carte:** si cerca ogni creatura a più di 5 punti (punteggio da giocata) dalla media della sua fazione e rarità. Non ce n'era nessuna (al massimo 1,6 punti, con un errore statistico di circa 0,45), quindi il passo è stato saltato.
- **Poi le fazioni:** un giro di taratura con ±1 DEF su tutte le creature di una fazione ha spostato gli scontri di vantaggio di 35–65 punti (Archive–Wound da 71,6% a 19,4%): troppo. I giri seguenti hanno cambiato 1 DEF su poche creature, scelte per valore V dentro la fazione (si alza la V più bassa, si abbassa la più alta), tra le Common e le Uncommon.
- **Infine la diluizione:** gli spostamenti per fazione pareggiano i quattro scontri di vantaggio ma non ne abbassano la media (64,9%). +1 ATK a tutte le creature rende il bonus meno pesante e porta la media al 58,3%; +1 DEF a tutte la allarga (fino al 72,6%).
- Le Mutant non si spostano con le fazioni (ognuna ha una fazione che sale e una che scende); hanno avuto il +1 ATK di tutte.

Il 20 novembre la fase 1 si è chiusa con 14 cambi di DEF (Archive e Dream giù, Wound e Invitation su). Il 21 novembre la fase 2 ha mostrato che compensavano soprattutto i Terreni di Archive e Dream (6–7 V più degli altri): corretti i Terreni, quei cambi si sono tolti tutti e la fase 1 si è ritarata con lo stesso metodo, in tre giri. Cambi finali al [catalogo finale](../bilanciamento/catalogo-finale.csv), con `valore_v` ricalcolato (V = 2,5 × ATK + DEF + PS):

| Cambio | Carte |
|---|---|
| +1 ATK | tutte le 92 creature, Mutant comprese |
| −1 DEF | Archive: Il Timbro di Nessuno; Dream: The Bone Lantern, Rokurokubi, L'Ombra del Torii |
| −2 DEF | Archive: SCP-053 |
| Effetti | i due Terreni e sette creature della fase 2 (sotto); `ps_effetto` del Badge di Uscita Negata da 4,5 a 5,3 |

Spell, Hidden e gli altri Oggetti non cambiano. `TestDecks` copia le nuove statistiche.

Risultati con +1 ATK contro +1 DEF (30 PV / 25 PV); bande: vantaggio 55–65%, neutri e fazioni 45–55%:

| | Prima di `ENG-42` | Dopo le fasi 1 e 2 |
|---|---|---|
| Archive–Wound | 71,6 / 67,9 | 58,5 / 57,1 |
| Dream–Invitation | 75,5 / 76,0 | 57,1 / 59,8 |
| Wound–Dream | 57,2 / 61,2 | 57,9 / 61,4 |
| Invitation–Archive | 58,2 / 58,0 | 61,8 / 62,1 |
| Neutri: Wound–Invitation, Archive–Dream | 55,3 e 54,7 / 58,0 e 54,3 | 51,0 e 53,2 / 53,5 e 54,8 |
| Fazioni | 42,5–56,1 / 41,3–54,7 | 48,7–51,2 / 47,9–52,6 |
| Carta più lontana dal suo gruppo (punti) | 6,8 / 7,6 | 3,1 / 3,4 |
| Finite per punti vita | 32,3% / 57,5% | 34,6% / 57,6% |
| Durata media in round | 42,5 / 39,3 | 36,7 / 33,8 |
| Avvisi | 6 / 5 | 0 / 0 |

Cosa dicono:

- Con +1 ATK contro +1 DEF tutti gli scontri e tutte le fazioni stanno nelle bande, con 30 e con 25 PV, senza avvisi.
- Con le stesse statistiche gli altri due bonus restano fuori: +2/−1 ATK al 99,9–100% negli scontri di vantaggio, il bonus di prova al 79,9–89,0%.
- D-01: a 30 PV finisce per punti vita il 34,6% delle partite, a 25 PV il 57,6%; D-01 cercava circa metà.
- La scala delle rarità resta quasi piatta (nel mazzo, a 30 PV: Common 49,8%, Uncommon e Rare 50,1%, Mythic 50,2%, Legendary 50,8%, Mutant 50,5%), ma ora cresce.

### Valore degli effetti (`ENG-42`, fase 2)

La fase 2 misura quanto vale l'effetto di ogni carta, perché il punteggio "da giocata" del report di bilanciamento non lo vede: una carta è circa il 5% delle creature di un mazzo, e dopo la fase 1 nessuna carta sta a più di 2,7 punti dal suo gruppo. Il test è `EffectAblationSimulationTest`, con il tag `effects`; si lancia a parte e scrive [`report-effetti.md`](../bilanciamento/report-effetti.md) con [`report-effetti-carte.csv`](../bilanciamento/report-effetti-carte.csv):

```bash
./gradlew effectTest   # Windows: gradlew.bat effectTest; circa 15-20 minuti
```

Scelte del 21 novembre:

| Cosa | Scelta |
|---|---|
| Partite | 30 PV con +1 ATK contro +1 DEF (la proposta della fase 1), 16 accoppiamenti × 1.250 = 20.000 partite di base, mazzi casuali e seed come nella griglia base (`EffectAblation`) |
| Partite appaiate | per ogni carta si rigiocano, con gli stessi seed e mazzi, solo le partite in cui i due mazzi ne hanno un numero diverso di copie, con la variante della carta al posto della carta (nelle altre la variante tocca i due lati allo stesso modo) |
| Valore per copia | regressione senza intercetta del guadagno dello sfidante (punteggio con la variante meno punteggio di base) sulla differenza di copie, sfidante meno avversario; con il suo errore standard |
| Senza effetto | una creatura perde il codice e tiene le statistiche; Spell, Hidden, Terreni e Oggetti hanno un effetto registrato che non fa niente, quindi il valore è quello di tutta la carta (un Terreno perde anche il bonus base) |
| Taratura | +1 DEF su ognuna delle 28 creature senza effetto (1 DEF = 1 punto di V); la media dei guadagni per copia dà i punti per V, e il valore di un effetto in V è il suo valore per copia diviso per questa media |
| Giudizio | le creature contro il prezzo del loro effetto (`ps_effetto`), Spell, Hidden, Terreni e Oggetti contro la media in V del loro tipo; "troppo forte" o "troppo debole" se lo scarto supera 2 punti di V e anche il 50% del riferimento (`EffectAblationReport`) |
| Invarianti | come `balanceTest`: nessun errore dell'engine, ogni partita finisce, le prime 200 partite di base danno lo stesso risultato due volte |

Prima misura del 21 novembre (catalogo dopo la fase 1, 20.000 partite di base, 10 minuti; nessun errore dell'engine):

- Taratura: 1 punto di V vale 1,39 punti di partita per copia. Errore standard di una carta: 0,1–0,4 V.
- Creature: in media l'effetto vale 1,2 V meno del suo prezzo; per rarità Uncommon −0,6, Rare −0,7, Mythic −1,5, Mutant −2,0, Legendary −3,1. Le carte rare valgono meno del prezzo, come mostra anche la scala delle rarità quasi piatta.
- Troppo deboli: Smiler (3,2 V contro 10,1), Shuten-dōji (4,9 contro 10,1), SCP-3999 (3,6 contro 8,6), The Thorned Matriarch (1,6 contro 6,5), The Unseen Matriarch (2,2 contro 4,5). Vicine alla soglia: Wendigo (−3,7, sotto il 50%), The Rootbound Doppelganger (−3,0). La Regina dei Sentieri Perduti vale −1,2 V: il suo effetto fa perdere.
- Troppo forte: Badge di Uscita Negata (5,5 V contro 3,1 della media degli Oggetti).
- Terreni (tutta la carta, bonus base compreso): Devouring Abyss 20,4 V e Watching Rift 19,6 contro Echo Rift e Hollow Clearing 13,6. Nessun avviso, perché lo scarto dalla media (16,8) è sotto il 50%, ma tra le fazioni la differenza è di circa 6 V in ogni mazzo: è una delle ragioni per cui Archive e Dream erano più forti.
- Per fazione, lo scarto totale degli effetti delle creature dal prezzo: Wound −12,0 V, Dream −13,9, Archive −19,6, Invitation −27,8.
- Spell e Hidden: tutte nella soglia (Spell 3,7–5,3 V, Hidden 2,2–4,5).

Correzioni decise il 21 novembre, una alla volta e rimisurate (proposte per i DSG; ogni numero ha il suo test):

| Carta | Prima | Ora | Valore in V, prima → dopo |
|---|---|---|---|
| Watching Rift (Terreno, Archive) | bonus base +1 ATK, +1 DEF | +1 ATK (il secondario vale 0,2 V) | 19,6 → 13,4 |
| Devouring Abyss (Terreno, Dream) | bonus base +1 ATK | +1 DEF (il secondario vale 7,9 V) | 20,4 → 16,5 |
| Smiler | +2 ATK a round (max +6), avv. −2 (max −4) | +3 (max +9), avv. −3 (max −6) | 3,2 → 5,4 |
| Shuten-dōji | +4 ATK a ferita, max +12 | +6, max +18 | 4,9 → 5,9 |
| SCP-3999 | all'entrata e ogni due round (3°, 5°, …) | all'entrata e in ogni round dal 2° | 3,6 → 5,6 |
| The Thorned Matriarch | 4 danni | 6 danni | 1,6 → 5,3 |
| Wendigo | −3 DEF a fine round, max 6 | −4, max 7 | 3,7 → 5,0 |
| The Unseen Matriarch | avv. −1 ATK | −2 ATK | 2,2 → 2,3 |
| SCP-682 | −2 a ogni attacco dopo il primo | −3 | 1,7 → 2,8 |
| Badge di Uscita Negata | `ps_effetto` 4,5 | 5,3 (effetto invariato) | 5,5 → 4,9 |

- I Terreni si misurano anche senza effetto secondario (sonda del 21 novembre, cancellata): bonus base da solo circa 12 V con +1 ATK, 7 con +1 DEF, 13 con +2 DEF, 18 con +1/+1.
- Il Badge solo contro gli attacchi è stato provato e tolto: valeva 5,1 V invece di 5,3, quasi tutti i suoi salvataggi sono da attacchi; si è alzato il prezzo.
- Smiler con il solo +3 ATK restava a 3,6 V: con 4/8 sopravvive di rado a un round; il malus più grande all'avversaria lo protegge.
- Dopo i Terreni Archive scendeva al 42,5% (Invitation–Archive 71,0%): i 14 cambi di DEF della fase 1 compensavano soprattutto i Terreni, quindi si sono tolti tutti e la fase 1 si è ritarata (vedi sopra).

Misura finale (stesso catalogo dei report di oggi): 1 punto di V vale 1,45 punti per copia; **nessuna carta fuori soglia**. Restano sotto il prezzo, ma dentro la soglia, Smiler (−4,7 V), Shuten-dōji (−4,2), SCP-3999 (−3,1), Wendigo (−2,4) e The Unseen Matriarch (−2,2). In media l'effetto delle creature vale 0,9 V meno del prezzo (Legendary −2,1, Mythic −1,2, Rare −0,7, Uncommon −0,5). Terreni 12,4–16,5 V, Oggetti 2,4–4,9.
