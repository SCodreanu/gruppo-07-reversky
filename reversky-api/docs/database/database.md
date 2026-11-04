# Database

## Scelte

- **PostgreSQL 18**, approvato dai docenti al posto di MySQL indicato nel brief.
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

```
V1__create_enums.sql
V2__create_users_and_auth.sql
V3__create_catalog_and_tags.sql
V4__create_collection_and_decks.sql
V5__create_battles.sql
V6__create_friendships.sql
V7__create_trades.sql
V8__create_inventories_and_packs.sql
V9__create_audit_logs.sql
V10__insert_reference_data.sql      ruoli, rarità, fazioni, bonus di fazione, tag
V11__insert_initial_catalog.sql     carte iniziali e pacchetto base
V12__add_ranked_battles.sql         modalità di sfida, coda di matchmaking, rating Elo
```

### Regole

1. **Mai modificare una migrazione già su `main`.** Per correggere, crea una nuova migrazione.
2. Una migrazione = un cambiamento logico.
3. Prima di creare `V<n>`, fai `git pull`: due persone non devono usare lo stesso numero.
4. I dati di riferimento (ruoli, rarità, fazioni, tag) e il catalogo iniziale vanno in migrazioni dedicate.

## Convenzioni (quelle usate dallo schema)

| Elemento | Regola | Esempio |
|---|---|---|
| Tabella | inglese, `snake_case` | `collection_cards`, `faction_bonus` |
| Colonna | `snake_case` | `password_hash` |
| Chiave primaria semplice | `id INTEGER` generato, oppure `id UUID` | `users.id`, `trades.id` |
| Chiave primaria composta | sulle tabelle di collegamento | `collection_cards (user_id, card_id)` |
| Chiave esterna | `<entità>_id` | `rarity_id`, `challenger_id` |
| Vincolo CHECK | `chk_<tabella>_<significato>` | `chk_inventories_credits_nonnegative` |
| Booleano | prefisso `is_` | `is_active`, `is_enabled` |
| Enum | tipo enum PostgreSQL | `card_type`, `trade_status` |
| JSON | `jsonb` | `battles.rounds_log` |

Quali tabelle usano `UUID`: token (`refresh_tokens`, `password_reset_tokens`, `email_verification_tokens`), `battle_invites`, `battles`, `trades`, `trade_offers`. Tutte le altre usano `INTEGER`.

## Schema per aree

### Utenti e autenticazione → [autenticazione](../autenticazione/autenticazione.md)

| Tabella | Contenuto |
|---|---|
| `roles` | ruoli con `authority_level` univoco (gerarchia: più alto = più permessi) |
| `users` | username, email, `password_hash`, ruolo, `is_enabled`, `email_verified_at`, date di accettazione privacy e termini, `created_at` |
| `refresh_tokens` | refresh token (solo **hash**) con scadenza |
| `password_reset_tokens` | token di reset password (solo hash) |
| `email_verification_tokens` | token di verifica email (solo hash) |

### Catalogo → [backoffice](../backoffice/backoffice.md)

| Tabella | Contenuto |
|---|---|
| `rule_sets` | **valori di gioco dal backoffice**, versionati, uno solo attivo: punti vita iniziali e limite di round (inviati all'engine), composizione del mazzo, crediti di benvenuto/vittoria/pareggio |
| `rarities` | rarità con `deck_limit` (copie massime per carta), `max_per_deck` (bilanciamento del mazzo), `max_atk`/`max_def` (statistiche massime) e `sorting_order` |
| `factions` | fazioni |
| `faction_bonus` | bonus ATK/DEF di una fazione contro un'altra: `atk_bonus` può essere negativo (malus −1), `def_bonus` ≥ 0 |
| `catalog_cards` | carte: tipo, rarità, fazione e seconda fazione facoltativa (`secondary_faction_id`, diversa dalla prima), ATK/DEF (nulli per le carte senza statistiche), `copy_limit`, `is_active`, immagine, `effect_description`, `flavor_text` |
| `tags` / `card_tags` / `faction_tags` | etichette per carte e fazioni (filtri dei pacchetti, effetti, sottotipi) |

### Collezione e mazzi → [collezione](../collezione/collezione.md), [mazzi](../mazzi/mazzi.md)

| Tabella | Contenuto |
|---|---|
| `collection_cards` | copie possedute per carta (`quantity > 0`, la riga si elimina a 0) |
| `decks` / `deck_cards` | mazzi (nome univoco per utente) e quantità per carta |

### Sfide → [sfide](../sfide/sfide.md)

| Tabella | Contenuto |
|---|---|
| `battle_invites` | invito diretto **tra amici**, solo per le partite `FRIENDLY`: sfidante, mazzo dello sfidante, avversario scelto, stato, `created_at`, `expires_at` (scadenza dopo 48 ore) |
| `battles` | partita con `mode` `FRIENDLY` (legata a un invito) o `RANKED` (creata dal matchmaking, `invite_id` nullo): fotografie dei mazzi, `rule_set_id` (regole usate), `battle_stats` (anche la copia delle regole e dei bonus di fazione inviati all'engine), `rounds_log`, risultato, `created_at`; per le `RANKED` anche rating prima della partita e variazione di entrambi i giocatori |
| `matchmaking_queue` | utenti in coda per una partita classificata: un ticket per utente, mazzo scelto, rating al momento dell'ingresso, `queued_at` |
| `player_ratings` | rating Elo (parte da 1000), rating massimo e statistiche delle sole partite classificate; le amichevoli non lo modificano |

### Amicizie e scambi → [amicizie](../amicizie/amicizie.md), [scambi](../scambi/scambi.md)

| Tabella | Contenuto |
|---|---|
| `friendships` | richiesta di amicizia (chiave: richiedente + destinatario) e stato |
| `trades` | scambio tra due utenti e stato |
| `trade_offers` | carte che **ogni partecipante** mette nello scambio |

### Inventario e pacchetti → [crediti](../crediti/crediti.md), [pacchetti](../pacchetti/pacchetti.md)

| Tabella | Contenuto |
|---|---|
| `inventories` | un inventario per utente con il saldo `credits` (≥ 0) |
| `packs` | tipi di pacchetto: costo, immagine, attivo |
| `pack_inventory` | pacchetti comprati e non ancora aperti |
| `pack_rarity_rules` | slot base per rarità (es. 3 Common, 2 Uncommon, 2 Rare) |
| `pack_bonus_rules` | probabilità di carte bonus per rarità, in unità su 10.000 |
| `pack_tags` | filtro: quali carte possono uscire dal pacchetto |

### Audit

| Tabella | Contenuto |
|---|---|
| `audit_logs` | ogni azione del backoffice, con valori prima/dopo in `jsonb` |

## Convenzioni sui tag

Lo schema non ha colonne per il codice dell'effetto e per il sottotipo delle carte: si usano i **tag**. (`effect_description` è solo il testo mostrato al giocatore.)

| Tag | Significato |
|---|---|
| `EFFECT_<CODICE>` | codice dell'effetto implementato nell'engine. Es. `EFFECT_HIDEBEHIND`. **Al massimo uno per carta.** |
| `DAMAGE`, `SHIELD`, `BURN` | sottotipo di una Spell |
| `SURVIVAL`, `DAMAGE` | sottotipo di una Hidden |
| altri | liberi, usati soprattutto dai filtri dei pacchetti (`pack_tags`) |

L'API traduce i tag in `effectCode` e `subtype` quando invia le carte all'engine.

## Mappatura JPA

| Caso | Come |
|---|---|
| `id` intero | `Integer id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `id` UUID | `UUID id` + `@GeneratedValue(strategy = GenerationType.UUID)` |
| Chiave composta | `@EmbeddedId` con una classe `@Embeddable` (es. `CollectionCardId`) |
| Enum PostgreSQL | `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` |
| `jsonb` | `@JdbcTypeCode(SqlTypes.JSON)` su un campo `Map`, `List` o record |
| `timestamp` | `LocalDateTime`, sempre in UTC |

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
