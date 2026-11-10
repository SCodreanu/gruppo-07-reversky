# Coding Requirements — reversky-engine

Regole di scrittura del codice per il motore di gioco. Valgono per tutti: studenti e agenti AI.
Il motore deve essere **corretto, prevedibile e facile da testare**: queste regole servono a quello.

---

## 1. Lingua

| Cosa | Lingua | Esempio |
|---|---|---|
| Classi, metodi, variabili, package | Inglese | `CombatResolver.resolveRound()` |
| Commenti e Javadoc | Italiano | `// I danni si applicano insieme: nessuno colpisce per primo` |
| Messaggi del log di partita, nomi di carte, fazioni ed effetti (li vedono i giocatori) | Inglese | `"Wendigo deals 7 damage to Kappa"` |
| Messaggi di commit | Italiano | `feat(effect): aggiunto effetto Hidebehind` |

### Glossario italiano → inglese

| Italiano | Inglese nel codice |
|---|---|
| Partita | `Match` |
| Round (singolo scambio di colpi) | `Round` |
| Scontro (serie di round tra le stesse due creature) | `Combat` |
| Carta | `Card` / `CardDefinition` |
| Creatura in gioco | `CreatureInPlay` |
| Fazione / Tipologia | `Faction` (record con id e nome, non un enum) |
| Bonus di fazione | `FactionBonus` |
| Sfidante / Avversario | `CHALLENGER` / `OPPONENT` (`Side`) |
| Rarità | `Rarity` |
| Mazzo / Pescare | `Deck` / `draw` |
| Punti vita | `lifePoints` |
| Attacco / Difesa | `attack` / `defense` nel core; `atk` / `def` nel JSON (come nel database) |
| Effetto / Abilità | `Effect` |
| Terreno | `Terrain` |

---

## 2. Formattazione

- Indentazione con **tab**, righe di massimo **120 caratteri**.
- Parentesi graffe sempre, anche per `if` di una riga.
- Niente `import x.*`.
- Ordine dei membri: costanti → campi → costruttori → metodi pubblici → metodi privati.

## 3. Nomi

| Elemento | Convenzione | Esempio |
|---|---|---|
| Classe | `PascalCase`, sostantivo | `MatchSimulator` |
| Metodo | `camelCase`, verbo | `applyDamage()` |
| Costante | `UPPER_SNAKE_CASE` | `DEFAULT_LIFE_POINTS` |
| Booleano | `is` / `has` / `can` | `isDead()`, `hasEffect()` |
| Effetto | `<NomeCarta>Effect` | `HidebehindEffect` |
| Codice effetto | `UPPER_SNAKE_CASE` | `HIDEBEHIND` |
| Test | `XxxTest` | `CombatResolverTest` |

---

## 4. Isolamento del core (regola principale)

| Package | Può usare | Non può usare |
|---|---|---|
| `core.*` | solo JDK (`java.*`) | Spring, Lombok, Jackson, qualsiasi libreria esterna |
| `web.*` | Spring, Lombok, Jackson, `core.*` | logica di gioco |

Perché: il core deve essere testabile con JUnit puro, senza avviare Spring, e riutilizzabile ovunque.

Il controller fa solo: DTO → oggetti del core → `MatchSimulator.simulate(...)` → oggetti del core → DTO.

```java
// SÌ — nel controller (web)
SimulationInput input = mapper.toCore(request);
MatchResult result = simulator.simulate(input);
return mapper.toResponse(result);

// NO — logica di gioco nel controller
if (request.challenger().deck().get(0).atk() > ...) { ... }
```

Il `MatchSimulator` viene creato come bean Spring in una classe `@Configuration` dentro `web`, **non** annotato con `@Component` nel core.

---

## 5. Determinismo

Una partita deve essere **riproducibile** al 100% dato lo stesso input.

| Vietato | Usa invece |
|---|---|
| `Math.random()`, `new Random()` | il `RandomGenerator` creato dal seed e passato in giro |
| `System.currentTimeMillis()`, `LocalDateTime.now()` | niente: il motore non ha bisogno dell'ora |
| iterare su `HashMap` / `HashSet` dove l'ordine conta | `List`, `LinkedHashMap`, ordinamenti espliciti |
| campi `static` modificabili | stato dentro gli oggetti della singola partita |
| `parallelStream()` nella simulazione | stream normali o cicli |

Vedi [docs/mescolamento](docs/mescolamento/mescolamento.md).

---

## 6. Numeri e arrotondamenti

- Si usano solo **interi** (`int`). Niente `double` per statistiche e danni.
- "Arrotondato per difetto" = divisione intera; con numeri che possono essere negativi usa `Math.floorDiv`.
- Gli attacchi effettivi non scendono mai sotto 0: `Math.max(0, attack)`.
- I bonus percentuali si calcolano così: `value * percent / 100` (es. 20% di 13 = `13 * 20 / 100` = 2).

```java
// Skin-Stealers: DEF pari al 25% della DEF base avversaria, per difetto, massimo 3
int gained = Math.min(3, opponentBaseDefense * 25 / 100);   // 13 → 3, 9 → 2
```

---

## 7. Classi e oggetti

- **Input e output sono immutabili**: `record` (`CardDefinition`, `SimulationInput`, `MatchResult`, `MatchEvent`).
- Lo stato che cambia durante la partita (`CreatureInPlay`, `PlayerState`) è mutabile ma **incapsulato**: si modifica solo tramite metodi con un nome chiaro (`takeDamage`, `addAttackModifier`), mai con setter generici.
- Un `MatchSimulator` non conserva stato tra una partita e l'altra: ogni chiamata crea il proprio `MatchState`.
- Metodi corti (~30 righe max): la risoluzione di un round è divisa in passi con metodi dedicati.
- Niente `null` per le liste. Per "assenza" usa `Optional` o un oggetto vuoto.
- Niente "numeri magici": i valori di gioco arrivano nella richiesta (`RuleConfig`, `FactionMatrix`); il resto in costanti con nome.

```java
// SÌ
creature.takeDamage(damage);

// NO
creature.setDefense(creature.getDefense() - damage);
```

---

## 8. Errori

- Input incoerente (mazzo vuoto, statistiche negative, effetto sconosciuto) → `InvalidSimulationInputException` nel core, trasformata in `400` dal `@RestControllerAdvice` in `web`.
- Una situazione "impossibile" durante la simulazione → `IllegalStateException` con messaggio chiaro: è un bug, deve emergere subito.
- Mai `catch` vuoti.

---

## 9. Commenti e Javadoc

### Regole

- **Spiega il perché, non il cosa.**
- Quando il codice implementa una regola del regolamento, citala: `// Regolamento, punto 7: ...`.
- Quando implementa una scelta **[DA CONFERMARE]**, scrivilo nel commento, così è facile ritrovarla.
- Niente codice commentato.

```java
// NO
// sottrae il danno alla difesa
defense -= damage;

// SÌ
// Regolamento base, punto 5: il danno riduce la difesa in modo permanente
// e non si rigenera. Solo gli effetti che curano possono ignorare questa regola
// (es. Wendigo, The Bloodless Hound, tramite heal()).
defense -= damage;
```

### Javadoc

Obbligatorio su: tutte le classi pubbliche del core, tutti i metodi pubblici del core, ogni classe effetto (con il testo della carta).

```java
/**
 * Hidebehind — "Previene 2 danni dal primo attacco ricevuto".
 * <p>
 * Il primo attacco con danno maggiore di 0 viene ridotto di 2 (minimo 0).
 * Dopo l'effetto non ha più alcun impatto.
 */
public final class HidebehindEffect implements CreatureEffect { ... }
```

### TODO

Formato: `// TODO(@username): cosa manca`, con una issue collegata su GitHub Projects.

---

## 10. Logging

- Il **log di partita** (eventi restituiti all'API) non è il logging tecnico: sono due cose diverse.
- Nel core **niente logging tecnico** (niente SLF4J): gli eventi vanno nel `MatchLog`.
- Nello strato `web` si usa `@Slf4j` (es. `log.info("Simulazione completata in {} round", rounds)`).

---

## 11. Test

Vedi [docs/test](docs/test/test.md). In breve:
- test JUnit **puri** per il core (niente `@SpringBootTest`);
- un test per ogni regola e ogni effetto;
- nomi parlanti, struttura **Arrange / Act / Assert**.

---

## 12. Git e GitHub Projects

- Branch: `feature/<n-issue>-descrizione`, `fix/<n-issue>-descrizione`.
- Commit in italiano: `tipo(ambito): descrizione` (`feat`, `fix`, `refactor`, `test`, `docs`, `chore`).
- Sempre Pull Request con almeno una revisione di un altro SWD, che cita la issue (`Closes #N`).

---

## 13. Checklist prima della Pull Request

- [ ] `./gradlew build` passa.
- [ ] Nessun import Spring/Lombok nel package `core`.
- [ ] Nessuna fonte di casualità diversa dal seed.
- [ ] Test per ogni regola o effetto nuovo/modificato.
- [ ] Javadoc su classi e metodi pubblici del core.
- [ ] Documento in `docs/` aggiornato se è cambiata una regola.
- [ ] Se è cambiato il contratto REST: avvisato chi lavora sull'API.
