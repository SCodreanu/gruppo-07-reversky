# Architettura del motore

## In una frase

**Input:** seed, regole della partita (rule set), fazioni con i loro bonus, mazzo dello sfidante e dell'avversario → **Output:** risultato (`CHALLENGER_WIN` / `OPPONENT_WIN` / `DRAW`), motivo di fine, log round per round.

```
reversky-api ──POST /api/v1/simulations──▶ web (Spring)
                                              │  DTO → oggetti core
                                              ▼
                                        core.MatchSimulator
                                              │
                                              ▼
                                         MatchResult
                                              │  oggetti core → DTO
reversky-api ◀────────── JSON ────────────  web (Spring)
```

## Due strati

### `core` — il gioco

Java puro. Contiene **tutte** le regole. Nessuna dipendenza da Spring, Lombok o altre librerie.

```
core/
├── model/
│   ├── CardDefinition      record: dati della carta come arrivano dall'API
│   ├── CreatureInPlay      creatura sul campo: ATK, DEF permanente più scudi, modificatori, effetto
│   ├── StatModifier        modificatore con origine, lato, durata e sospensione (D-35)
│   ├── PendingModifier     modificatore in attesa della prossima creatura di un giocatore (D-34)
│   ├── Faction             record (id, nome): le fazioni sono dati, non un enum
│   ├── PlayerState         lato (CHALLENGER/OPPONENT), punti vita, mazzo, creatura in campo, Hidden, Oggetto
│   └── MatchState          i due giocatori + numero round + log + random + il Terreno in gioco (uno solo, D-19)
├── rules/
│   ├── RuleConfig          punti vita, limite round, versione: costruita dalla richiesta
│   └── FactionMatrix       bonus ATK/DEF tra fazioni, costruita dai dati della richiesta
├── simulation/
│   ├── MatchSimulator      punto d'ingresso: simulate(SimulationInput) → MatchResult
│   ├── RoundResolver       risolve un singolo round nell'ordine stabilito
│   ├── DeathResolver       morti e danno al giocatore (DEF negativa), dopo ogni fase che toglie DEF (D-08)
│   ├── DrawService         pesca finché esce una creatura; risolve le Spell pescate (Hidden, Terreni, Oggetti: ENG-20…)
│   └── EndConditionChecker controlla se e come finisce la partita
├── effect/
│   ├── CardEffect          interfaccia chiusa degli effetti di Spell, Hidden, Terreni e Oggetti, una per tipo
│   ├── EffectContext       partita, proprietario, avversario e carta visti da un effetto
│   ├── CreatureEffect      interfaccia con i "momenti" in cui un effetto di creatura può agire (ENG-23)
│   ├── EffectRegistry      codice effetto → tipo di carta e implementazione
│   └── impl/               un file per effetto (HidebehindEffect, WendigoEffect…)
├── validation/
│   ├── SimulationInputValidator  controlla l'input nell'ordine dei codici di errore di api-engine
│   ├── ErrorCode                 i codici di errore del contratto, nell'ordine dei controlli
│   └── InvalidSimulationInputException  input incoerente, diventa 400 nello strato web
└── log/
    ├── MatchLog            lista ordinata di eventi
    └── MatchEvent          record di un evento
```

### `web` — l'adattatore HTTP

Spring Boot. Contiene solo:
- `SimulationController` (`POST /api/v1/simulations`, `GET /api/v1/effects`);
- DTO di richiesta/risposta in `web/dto` (record con le annotazioni di validazione e i messaggi in inglese, come nell'API):
  `SimulationRequest`, `RulesRequest`, `FactionRequest`, `FactionBonusRequest`, `PlayerRequest`, `CardRequest`,
  `SimulationResponse`, `PlayerResultResponse`, `MatchEventResponse`;
- `SimulationMapper` (DTO ↔ core: copia i campi, `atk`/`def` diventano `attack`/`defense`, nessuna regola);
- `EngineConfiguration` che crea i bean del core (`MatchSimulator`, `EffectRegistry`);
- `GlobalExceptionHandler`: ogni errore diventa un `ProblemDetail` con `code` (400 per l'input, 500 `INTERNAL_ERROR` per un bug);
- `SimulationFailedException`: avvolge un bug della simulazione con il seed, che finisce nel log tecnico per riprodurre la partita.

```java
@Configuration
public class EngineConfiguration {

	@Bean
	EffectRegistry effectRegistry() {
		return EffectRegistry.withAllEffects();
	}

	@Bean
	MatchSimulator matchSimulator(EffectRegistry effects) {
		return new MatchSimulator(effects);
	}
}
```

## Perché è separato dall'API

- Il brief chiede un **componente isolato e testato**.
- Le regole cambieranno spesso durante il bilanciamento: tenerle in un posto solo evita errori.
- Senza database, i test sono veloci e non serve Docker.
- Si possono lanciare migliaia di partite simulate per verificare il bilanciamento delle carte.

## Senza stato

L'engine non salva niente. Ogni richiesta è indipendente: può essere riavviato in qualsiasi momento e possono esserci più istanze. Salvare il risultato è compito dell'API.

## Configurazione

```properties
spring.application.name=reversky-engine
server.port=8081
```

Nessun valore di gioco sta nella configurazione o nel codice dell'engine. Punti vita, limite di round e bonus di fazione arrivano in **ogni richiesta**, presi dal rule set attivo e dalla tabella `faction_bonuses` dell'API, che si modificano dal backoffice (vedi [regole-base](../regole-base/regole-base.md#valori-rule-set)).
La versione delle regole (`rulesVersion`) torna in ogni risultato: così nello storico si sa con quali regole è stata giocata una partita.

`MatchSimulator.simulate(SimulationInput)` riceve le regole dentro `SimulationInput`: lo stesso simulatore (un solo bean) gioca partite con rule set diversi.
