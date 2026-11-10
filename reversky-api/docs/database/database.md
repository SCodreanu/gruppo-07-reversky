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
| rule set `1.0` attivo, rarità, fazioni, bonus di fazione | migrazione dedicata del task API-15 (#85) |
| tag delle fazioni | migrazione del task API-13 (#83) |
| catalogo iniziale (120 carte) | migrazione dedicata del task API-55 (#125) |
| pacchetto base | migrazione dedicata del task API-63 (#133) |
| primo `ADMIN`, utenti, carte e collezioni di prova | task API-25 (#95), che decide anche come caricarli solo in sviluppo e test |

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
| Effetto | `effect_code` | codice di un effetto implementato nell'engine, es. `HIDEBEHIND`. Al massimo uno per carta |
| Sottotipo di una Spell | `subtype` | `DAMAGE`, `SHIELD` o `BURN` (obbligatorio) |
| Sottotipo di una Hidden | `subtype` | `SURVIVAL` o `DAMAGE` (obbligatorio) |
| Sottotipo di Creature, Terreni e Oggetti | `subtype` | sempre nullo |
| Fazione | `faction_id` | obbligatoria per Creature, Terreni e Oggetti; sempre nulla per Spell e Hidden (`chk_catalog_cards_faction_matches_type`) |
| Etichette | `tags` | libere, in maiuscolo (`^[A-Z0-9_]+$`), usate dai filtri dei pacchetti (`pack_tags`) |

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
