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
│   ├── CreatureInPlay      creatura sul campo: ATK, DEF attuale, modificatori, effetto
│   ├── Faction             record (id, nome): le fazioni sono dati, non un enum
│   ├── PlayerState         lato (CHALLENGER/OPPONENT), punti vita, mazzo, creatura in campo, Hidden, Terreno
│   └── MatchState          i due giocatori + numero round + log + random
├── rules/
│   ├── RuleConfig          punti vita, limite round, versione: costruita dalla richiesta
│   └── FactionMatrix       bonus ATK/DEF tra fazioni, costruita dai dati della richiesta
├── simulation/
│   ├── MatchSimulator      punto d'ingresso: simulate(SimulationInput) → MatchResult
│   ├── RoundResolver       risolve un singolo round nell'ordine stabilito; danno al giocatore = DEF negativa
│   ├── DrawService         pesca, gestisce Spell/Hidden/Terreni pescati
│   └── EndConditionChecker controlla se e come finisce la partita
├── effect/
│   ├── CreatureEffect      interfaccia con i "momenti" in cui un effetto può agire
│   ├── EffectRegistry      codice effetto → implementazione
│   └── impl/               un file per effetto (HidebehindEffect, WendigoEffect…)
└── log/
    ├── MatchLog            lista ordinata di eventi
    └── MatchEvent          record di un evento
```

### `web` — l'adattatore HTTP

Spring Boot. Contiene solo:
- `SimulationController` (`POST /api/v1/simulations`, `GET /api/v1/effects`);
- DTO di richiesta/risposta con le annotazioni di validazione;
- `SimulationMapper` (DTO ↔ core);
- `EngineConfiguration` che crea i bean del core (`MatchSimulator`, `EffectRegistry`);
- `GlobalExceptionHandler`.

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
