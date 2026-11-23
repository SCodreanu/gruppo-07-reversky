# Database

## Scelte

- **PostgreSQL 18**, concordato con il cliente e approvato, al posto di MySQL indicato nel brief.
- Lo schema completo, fonte di verità per le migrazioni, è in [schema.dbml](schema.dbml) (si apre su dbdiagram.io).
- Lo schema è gestito **solo da Flyway**. Hibernate non crea né modifica tabelle (`ddl-auto=validate`).
- In locale il database parte con Docker grazie a `compose.yaml` e `spring-boot-docker-compose`.
- Nei test si usa un PostgreSQL vero tramite **Testcontainers** (vedi [test](../test/test.md)).
- Per la consegna serve l'**export del database** (`pg_dump`), da includere nello ZIP finale.

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
spring.flyway.locations=classpath:db/migration
```

## Migrazioni Flyway

Percorso: `src/main/resources/db/migration/`

Nome dei file: `V<numero>__<descrizione_in_snake_case>.sql`

### Schema bloccato (baseline)

Tutta la **struttura** del database si scrive in una volta sola, da [schema.dbml](schema.dbml), nel task API-70 (#141). Da lì in poi lo schema è **bloccato**: gli altri task trovano le tabelle già pronte e scrivono solo Java (entità, repository, service, controller), e `ddl-auto=validate` segnala subito un'entità che non corrisponde.

Le vecchie `V1`–`V6` (una per task) non sono mai arrivate su `main` e vengono sostituite dalla baseline, una migrazione per area:

```
V1__create_enums.sql                     tutti gli enum (compreso ITEM in card_type)
V2__create_users_and_auth.sql            roles (con USER e ADMIN), users e le tre tabelle dei token
V3__create_catalog.sql                   rarities, factions, faction_bonuses, rule_sets, tags, faction_tags,
                                         catalog_cards, card_tags
V4__create_inventory_and_packs.sql       inventories, credit_transactions, packs, pack_rarity_rules,
                                         pack_bonus_rules, pack_tags, pack_inventory, pack_openings
V5__create_collection_and_decks.sql      collection_cards, decks, deck_cards
V6__create_friendships_and_trades.sql    friendships, trades, trade_offers
V7__create_battles.sql                   battle_invites, battles, matchmaking_queue, player_ratings
V8__create_audit_and_notifications.sql   audit_logs, notifications
```

Dopo il merge della baseline ogni sviluppatore cancella il volume del database locale: i checksum delle vecchie `V1`–`V6` non coincidono più.

Alcune colonne obbligatorie collegano due entità che hanno task separati (es. `battle_invites.challenger_deck_id`, `battles.rule_set_id`, `matchmaking_queue.deck_id`): l'entità le mappa già nel suo task, e il task di collegamento aggiunge solo le regole.

Dati inseriti con le migrazioni:

| Dati | Dove |
|---|---|
| ruoli `USER` e `ADMIN` | `V2`, insieme alla tabella, perché il codice ne usa i nomi |
| rule set `1.0` attivo, rarità e fazioni (nessun bonus di fazione finché non si decide D-04) | `V9__insert_reference_data.sql`, task API-15 (#85); valori in [decisioni](../decisioni/decisioni.md) |
| inventario e riga `SIGNUP` per gli utenti registrati prima di API-17 (su un database vuoto non inserisce niente) | `V10__create_missing_inventories.sql`, task API-17 (#87) |
| catalogo iniziale: 92 Creature, 12 Spell, 8 Hidden, 4 Terreni e 4 Oggetti da `reversky-engine/docs/bilanciamento/catalogo-finale.csv` (ENG-42) | `V11__insert_initial_catalog.sql`, task API-55 (#125) |
| pacchetto base attivo: *Base Pack*, 100 crediti **[DA CONFERMARE]**, 3 Common, 2 Uncommon, 2 Rare, bonus Mythic 10%, Legendary 5%, Mutant 2,5% | `V12__insert_base_pack.sql`, task API-63 (#133) |
| tabella `effects` con i 92 effetti del registro dell'engine e chiave esterna `fk_catalog_cards_effect`: prima struttura nuova dopo la baseline, decisa il 21 novembre | `V13__create_effects.sql`, task API-36 (#106): vedi [Effetti](#effetti-sottotipi-e-tag) |
| vincolo `chk_catalog_cards_secondary_faction_creature_only`: solo le creature hanno una seconda fazione, come chiede l'engine (deciso il 22 novembre) | `V14__add_secondary_faction_check.sql`, task API-71 (#146) |
| tag delle quattro fazioni `WOUND`, `INVITATION`, `ARCHIVE`, `DREAM`, collegati per nome alla loro fazione | `V15__insert_faction_tags.sql`, task API-13 (#83) |
| le tre bustine della proposta DSG "Economia di gioco 2.0" **[DA CONFERMARE]**: il Base Pack diventa *Standard* (600 crediti), più *Mini* (300 crediti, 1 Common, 1 Uncommon, 1 Rare, bonus 7%, 2,5%, 0,5%) e *Premium* (1.200 crediti, 5 Common, 4 Uncommon, 6 Rare, bonus 52,54%, 27,10%, 8,73%) | `V16__insert_mini_standard_premium_packs.sql`: vedi [pacchetti](../pacchetti/pacchetti.md#le-bustine-del-negozio) |
| utenti `admin`, `alice`, `bob`, collezioni, mazzi e amicizia di prova, **solo in sviluppo** | `db/dev/R__dev_seed.sql`, task API-25 (#95): vedi sotto |

### Catalogo iniziale (`V11`)

Generato una volta dal CSV del bilanciamento (commit `2c6ec71` di `backend_engine`) con uno script usa e getta, non nel repository: le correzioni si fanno dal backoffice o con una migrazione nuova.

- Rarità per `sorting_order` e fazioni per nome, come le inserisce `V9`. Spell e Hidden non hanno rarità nel CSV: sono **Rare** (deciso il 21 novembre) e non contano nel `max_per_deck` (vedi [mazzi](../mazzi/mazzi.md#bilanciamento-richiesto-dal-brief)).
- Nomi ed `effect_description` come nel CSV, in italiano: la traduzione arriva nell'ultima settimana. Ogni `effect_code` esiste nell'engine, anche quelli delle creature.
- **[DA CONFERMARE]** `image_path` segnaposto `cards/<nome-della-carta>.webp` finché non ci sono le illustrazioni, che si caricano con `POST /admin/cards/{id}/image` (API-14, vedi [backoffice](../backoffice/backoffice.md#illustrazioni-comportamento-già-implementato-api-14-84)). `copy_limit` e `flavor_text` vuoti, nessun tag proprio (API-12).
- I due Mutant (Invitation/Dream e Wound/Archive) hanno la seconda fazione.

### Dati di prova (solo sviluppo)

`db/dev/R__dev_seed.sql` è una migrazione **ripetibile**: Flyway la esegue dopo quelle versionate e di nuovo solo quando il file cambia, e ogni istruzione inserisce soltanto ciò che manca (nessun duplicato, niente sovrascritto). La cartella `db/dev` è nelle `spring.flyway.locations` solo con il profilo `dev` (`application-dev.properties`).

- **Attivazione:** `./gradlew bootRun` usa il profilo `dev` da solo (`build.gradle`); con `SPRING_PROFILES_ACTIVE` impostato vale quello. Il `compose.yaml` della cartella principale passa `SPRING_PROFILES_ACTIVE` all'API (default `default`): `docker compose up --build` carica i dati di prova solo con `SPRING_PROFILES_ACTIVE=dev` nell'ambiente o nel file `.env` (vedi `.env.example` e [sfide](../sfide/sfide.md#prova-sullo-stack-completo)). `java -jar` senza profilo e i test (tranne `DevSeedIT`, che ha il suo database) non li caricano.
- **Utenti**, tutti con email verificata e password di sviluppo `Reversky2026` (mai usarla altrove):

| Username | Email | Ruolo | Dati |
|---|---|---|---|
| `admin` | `admin@reversky.dev` | `ADMIN` | solo l'inventario |
| `alice` | `alice@reversky.dev` | `USER` | 10.000 crediti (benvenuto più una ricarica `ADMIN_ADJUSTMENT`, una volta sola) per provare le tre bustine, tutte le carte originali attive fino al limite di copie, mazzo valido *Alice Invitation* |
| `bob` | `bob@reversky.dev` | `USER` | come alice, con il mazzo *Bob Wound* |

- `alice` e `bob` sono amici (`ACCEPTED`), così possono sfidarsi in amichevole.
- Per ripartire da zero basta cancellare il volume del database locale.

### Regole

1. **Lo schema è bloccato.** Un cambiamento di struttura dopo la baseline si decide insieme e va in una migrazione **nuova**, con il primo numero libero, aggiornando `schema.dbml` nello stesso commit. Mai modificare una migrazione già su `main`.
2. Una migrazione = un cambiamento logico.
3. Prima di creare `V<n>`, fai `git pull` e guarda il numero più alto nella cartella. Se due branch usano lo stesso numero, chi unisce per secondo rinomina la propria migrazione prima del merge.
4. I dati (riferimento, catalogo, pacchetto base, utenti di prova) vanno in migrazioni dedicate dopo la baseline, mai insieme alla creazione delle tabelle. Unica eccezione: i ruoli in `V2`.
5. Un nuovo valore di un enum (`ALTER TYPE ... ADD VALUE`) aggiunto dopo la baseline va in una migrazione **da solo**: PostgreSQL non permette di usarlo (in un `CHECK` o in un `INSERT`) nella stessa transazione che lo aggiunge, e Flyway esegue ogni migrazione in una transazione.
6. Nei `CHECK` su colonne nullable si scrive `IS NOT NULL` in modo esplicito: PostgreSQL accetta un controllo che vale `NULL`.

## Convenzioni (quelle usate dallo schema)

| Elemento | Regola | Esempio |
|---|---|---|
| Tabella | inglese, `snake_case` | `collection_cards`, `faction_bonuses` |
| Colonna | `snake_case` | `password_hash` |
| Chiave primaria semplice | `id INTEGER` generato, oppure `id UUID` | `users.id`, `trades.id` |
| Chiave primaria composta | sulle tabelle di collegamento | `collection_cards (user_id, card_id)` |
| Chiave esterna | `<entità>_id` | `rarity_id`, `challenger_id` |
| Vincolo CHECK | `chk_<tabella>_<significato>` | `chk_inventories_credits_nonnegative` |
| Booleano | prefisso `is_` | `is_active`, `is_enabled` |
| Enum | tipo enum PostgreSQL | `card_type`, `trade_status` |
| JSON | `jsonb` | `battles.rounds_log` |

Quali tabelle usano `UUID`: token (`refresh_tokens`, `password_reset_tokens`, `email_verification_tokens`), `battle_invites`, `battles`, `trades`. Tutte le altre usano `INTEGER`.

## Schema per aree

### Utenti e autenticazione → [autenticazione](../autenticazione/autenticazione.md)

| Tabella | Contenuto |
|---|---|
| `roles` | ruoli degli utenti (`USER`, `ADMIN`), con `name` univoco |
| `users` | username, email, `password_hash`, ruolo, `is_enabled`, `email_verified_at`, date di accettazione privacy e termini, `created_at` |
| `refresh_tokens` | refresh token (solo **hash**) con scadenza |
| `password_reset_tokens` | token di reset password (solo hash) |
| `email_verification_tokens` | token di verifica email (solo hash) |

### Catalogo → [backoffice](../backoffice/backoffice.md)

| Tabella | Contenuto |
|---|---|
| `rule_sets` | **valori di gioco dal backoffice**, versionati, uno solo attivo: punti vita iniziali e limite di round (inviati all'engine), composizione del mazzo, crediti di benvenuto e di vittoria/pareggio (solo nelle partite `RANKED`) |
| `rarities` | rarità con `deck_limit` (copie massime per carta), `max_per_deck` (bilanciamento del mazzo) e `sorting_order`; nessun massimo di ATK e DEF |
| `factions` | fazioni: nome, `emblem_path` (emblema) e `description` (testo mostrato ai giocatori, facoltativo) |
| `faction_bonuses` | bonus ATK/DEF di una fazione contro un'altra: `atk_bonus` può essere negativo (malus −1), `def_bonus` ≥ 0 |
| `catalog_cards` | carte: tipo (Creatura, Spell, Hidden, Terreno, Oggetto), `subtype` (solo Spell e Hidden), `effect_code`, rarità, fazione (obbligatoria per Creature, Terreni e Oggetti, nulla per Spell e Hidden, che sono condivise) e seconda fazione facoltativa (`secondary_faction_id`, diversa dalla prima), ATK/DEF (nulli per le carte senza statistiche), `copy_limit` (nullo = solo il limite della rarità), `variant_of_id` (versione evento di un'altra carta), `is_active`, immagine, `effect_description`, `flavor_text`. Il nome è univoco tra le carte originali |
| `effects` | effetti implementati dall'engine (`code`, `applies_to`): gli unici `effect_code` ammessi, ognuno per un solo tipo di carta. Sola lettura, si aggiorna con una migrazione (V13) |
| `tags` / `card_tags` / `faction_tags` | etichette libere per carte e fazioni, usate dai filtri dei pacchetti |

### Collezione e mazzi → [collezione](../collezione/collezione.md), [mazzi](../mazzi/mazzi.md)

| Tabella | Contenuto |
|---|---|
| `collection_cards` | copie possedute per carta (`quantity > 0`, la riga si elimina a 0) |
| `decks` / `deck_cards` | mazzi (nome univoco per utente) e quantità per carta |

### Sfide → [sfide](../sfide/sfide.md)

| Tabella | Contenuto |
|---|---|
| `battle_invites` | invito diretto **tra amici**, solo per le partite `FRIENDLY`: sfidante, mazzo dello sfidante, avversario scelto, stato, `created_at`. Non scade; al massimo un invito `PENDING` per direzione; la riga si elimina quando l'invito viene rifiutato o ritirato |
| `battles` | partita con `mode` `FRIENDLY` (legata a un invito) o `RANKED` (creata dal matchmaking, `invite_id` nullo): fotografie dei mazzi, `rule_set_id` (regole usate), `battle_stats` (anche la copia delle regole e dei bonus di fazione inviati all'engine), `rounds_log`, risultato, `created_at`; per le `RANKED` anche rating prima della partita e variazione di entrambi i giocatori |
| `matchmaking_queue` | utenti in coda per una partita classificata: un ticket per utente, mazzo scelto, rating al momento dell'ingresso, `queued_at` |
| `player_ratings` | rating Elo (parte da 1000), rating massimo e statistiche delle sole partite classificate; le amichevoli non lo modificano |

### Amicizie e scambi → [amicizie](../amicizie/amicizie.md), [scambi](../scambi/scambi.md)

| Tabella | Contenuto |
|---|---|
| `friendships` | richiesta di amicizia (chiave: richiedente + destinatario) e stato; al massimo una riga per coppia di utenti, eliminata quando la richiesta viene rifiutata o ritirata |
| `trades` | scambio tra due utenti e stato |
| `trade_offers` | carte che **ogni partecipante** mette nello scambio (chiave: scambio + chi cede + carta); le copie di chi ha fatto l'offerta in corso sono riservate finché lo scambio è aperto |

### Inventario e pacchetti → [crediti](../crediti/crediti.md), [pacchetti](../pacchetti/pacchetti.md)

| Tabella | Contenuto |
|---|---|
| `inventories` | un inventario per utente (chiave: `user_id`) con il saldo `credits` (≥ 0) |
| `credit_transactions` | storico di ogni movimento di crediti: importo (positivo, negativo, oppure 0 per un pacchetto gratuito), motivo, `created_at` |
| `packs` | tipi di pacchetto: costo (0 = omaggio), `purchase_limit` per utente (nullo = nessun limite, obbligatorio per gli omaggi), immagine, attivo |
| `pack_inventory` | pacchetti comprati e non ancora aperti (`quantity > 0`, la riga si elimina a 0) |
| `pack_openings` | storico delle aperture: utente, pacchetto, carte ricevute in `jsonb`, `created_at` |
| `pack_rarity_rules` | slot base per rarità (es. 3 Common, 2 Uncommon, 2 Rare) |
| `pack_bonus_rules` | probabilità di carte bonus per rarità, in unità su 10.000 |
| `pack_tags` | filtro: quali carte possono uscire dal pacchetto |

### Audit

| Tabella | Contenuto |
|---|---|
| `audit_logs` | ogni azione del backoffice, con valori prima/dopo in `jsonb` |

### Notifiche

| Tabella | Contenuto |
|---|---|
| `notifications` | notifiche di ogni utente (`type`, `payload` in `jsonb`, `read_at`), inviate anche con SSE |

## Effetti, sottotipi e tag

Effetto e sottotipo di una carta sono **colonne** di `catalog_cards`, così il database ne garantisce le regole. L'API le invia all'engine come `effectCode` e `subtype`. (`effect_description` è solo il testo mostrato al giocatore.)

| Dato | Dove | Regola |
|---|---|---|
| Effetto | `effect_code` | codice di un effetto implementato nell'engine, es. `HIDEBEHIND`. Al massimo uno per carta. Deve essere nella tabella `effects` con lo stesso tipo della carta (`fk_catalog_cards_effect`, V13) |
| Sottotipo di una Spell | `subtype` | `DAMAGE`, `SHIELD` o `BURN` (obbligatorio) |
| Sottotipo di una Hidden | `subtype` | `SURVIVAL` o `DAMAGE` (obbligatorio) |
| Sottotipo di Creature, Terreni e Oggetti | `subtype` | sempre nullo |
| Fazione | `faction_id` | obbligatoria per Creature, Terreni e Oggetti; sempre nulla per Spell e Hidden (`chk_catalog_cards_faction_matches_type`) |
| Etichette | `tags` | libere, in maiuscolo (`^[A-Z0-9_]+$`), usate dai filtri dei pacchetti (`pack_tags`) |

### Tabella `effects` (V13, API-36)

L'elenco degli effetti validi vive **nell'API**: l'engine si chiama solo per simulare (`POST /api/v1/simulations`), mai per sapere quali effetti esistono (deciso il 21 novembre). `V13` copia i 92 codici di `EffectRegistry.withAllEffects()` dell'engine (commit `1719c94` di `backend_engine`, ENG-42), ognuno con il suo tipo di carta (`applies_to`).

- Chiave esterna composta `fk_catalog_cards_effect`: `catalog_cards (effect_code, type)` → `effects (code, applies_to)`, con l'unicità `uq_effects_code_applies_to` come destinazione. Il database rifiuta un codice inesistente o di un altro tipo; con `effect_code` nullo (creature senza effetto) non si controlla niente.
- Un effetto nuovo è codice Java nell'engine: arriva con una migrazione nuova nello stesso momento, mai dal backoffice. Tabella e registro dell'engine vanno tenuti uguali a mano.

## Mappatura JPA

| Caso | Come |
|---|---|
| `id` intero | `Integer id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `id` UUID | `UUID id` + `@GeneratedValue(strategy = GenerationType.UUID)` |
| Chiave composta | `@EmbeddedId` con una classe `@Embeddable` (es. `CollectionCardId`) |
| Enum PostgreSQL | `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` |
| `jsonb` | `@JdbcTypeCode(SqlTypes.JSON)` su un campo `Map`, `List` o record |
| `timestamptz` | `Instant`, sempre in UTC |

```java
@Entity
@Table(name = "battles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Battle {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	private BattleResult result;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rounds_log", nullable = false)
	private List<MatchEventDto> roundsLog;
	...
}
```

## Dati sensibili

- Password solo come hash **BCrypt** (`password_hash`).
- Token (refresh, reset, verifica) solo come **hash SHA-256**: il valore in chiaro viene inviato all'utente e mai salvato.
- Nell'export per la consegna usare utenti di test, non dati reali.
