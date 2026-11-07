# Autenticazione e utenti

## Cosa fa

- Registrazione con accettazione di privacy e termini.
- Login con **access token JWT** (breve) + **refresh token** (lungo, salvato come hash).
- Logout, rinnovo del token, reset della password, verifica dell'email.
- Due ruoli: `USER` e `ADMIN`.

Tabelle: `roles`, `users`, `refresh_tokens`, `password_reset_tokens`, `email_verification_tokens`, `inventories`.

## Ruoli

| `name` | Uso |
|---|---|
| `USER` | giocatore |
| `ADMIN` | backoffice |

- In Spring Security il ruolo diventa l'authority `ROLE_<name>`.
- Gli `ADMIN` si creano solo da migrazione o dal backoffice, mai dalla registrazione pubblica. Il primo `ADMIN` e gli utenti di test sono inseriti dalla migrazione dei dati di prova (task API-25, #95), che deve valorizzare anche `privacy_accepted_at` e `terms_accepted_at` e creare il loro `inventories`, con la riga `SIGNUP` in `credit_transactions` se hanno crediti.

## Registrazione

`POST /api/v1/auth/register`

```json
{
  "username": "pioggia_inversa",
  "email": "mario@example.com",
  "password": "UnaPasswordSicura1",
  "acceptPrivacy": true,
  "acceptTerms": true
}
```

Regole:
- `username`: 3–30 caratteri (lettere, numeri, `_`), **univoco**.
- `email`: formato valido, **univoca**.
- `password`: da 8 a 72 caratteri (72 è il limite di BCrypt), almeno una lettera e un numero; salvata solo come hash **BCrypt**.
- `acceptPrivacy` e `acceptTerms` devono essere `true`: si salvano `privacy_accepted_at` e `terms_accepted_at`.
- L'email viene normalizzata (spazi tolti, minuscolo) prima di salvarla e prima di ogni ricerca.

Nella **stessa transazione**:
1. crea l'utente con ruolo `USER`;
2. crea il suo `inventories` con i crediti di benvenuto e la riga `SIGNUP` in `credit_transactions` (vedi [crediti](../crediti/crediti.md)) — **non ancora implementato**: arriva con l'inventario (TODO in `AuthService.register`);
3. crea un token di verifica email e invia la mail.

Risposta `201 Created` senza corpo. Campi non validi → `400` con l'elenco dei campi in `errors`. Username o email già usati → `409 Conflict`.

## Login

`POST /api/v1/auth/login`

```json
{ "email": "mario@example.com", "password": "UnaPasswordSicura1" }
```

Risposta `200 OK`:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "q8Zk1...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- Email o password mancanti → `400`.
- Credenziali errate → `401` con messaggio generico ("Invalid credentials"), **senza dire** se è sbagliata l'email o la password.
- Utente con `is_enabled = false` → `403` ("Account disabled"), solo se la password è corretta.

## Token

| Token | Durata | Dove vive |
|---|---|---|
| Access token (JWT) | 15 minuti | solo nel client; mai salvato nel DB |
| Refresh token | 30 giorni | client + **hash** in `refresh_tokens` |

- JWT firmato HS256 con chiave da variabile d'ambiente (`reversky.security.jwt-secret=${JWT_SECRET}`, almeno 32 caratteri).
  Per comodità del team, in sviluppo `application.properties` e il `compose.yaml` principale hanno un valore di default: **va tolto prima del primo deploy**, così senza `JWT_SECRET` l'applicazione non parte.
- Contenuto del JWT: `sub` = id utente, `username`, `role`, `iat`, `exp`.
- I token salvati nel DB (refresh, reset, verifica) sono **hash SHA-256** del valore casuale: si cerca per hash, quindi non si usa BCrypt per loro.

```java
String rawToken = generateSecureToken();          // 32 byte casuali, Base64 URL
String tokenHash = sha256(rawToken);              // questo va nel database
```

### Rinnovo

`POST /api/v1/auth/refresh` con `{ "refreshToken": "..." }`
- cerca l'hash, controlla la scadenza;
- **rotazione**: elimina il vecchio refresh token e ne crea uno nuovo;
- restituisce un nuovo access token e un nuovo refresh token (`200`, stesso formato del login).

Errori:
- token inesistente, scaduto o già usato → `401` (`AUTH_REFRESH_TOKEN_INVALID`): il frontend riporta l'utente al login;
- account disabilitato nel frattempo → `403`.

Due rinnovi simultanei con lo stesso token: vince solo il primo. La cancellazione restituisce il numero di righe eliminate, e chi ne elimina 0 riceve `401`.

### Logout

`POST /api/v1/auth/logout` con `{ "refreshToken": "..." }` → elimina quel refresh token. `204`, anche se il token non esiste o è già stato eliminato.

L'access token già emesso resta valido fino alla scadenza (al massimo 15 minuti): il frontend lo deve semplicemente scartare.

## Reset della password

1. `POST /api/v1/auth/password/forgot` con `{ "email": "..." }`
   - risponde **sempre** `204`, anche se l'email non esiste (non rivela quali email sono registrate);
   - se l'utente esiste: elimina i suoi token di reset precedenti, ne crea uno nuovo (scadenza 30 minuti) e invia la mail con il link.
2. `POST /api/v1/auth/password/reset` con `{ "token": "...", "newPassword": "..." }`
   - `newPassword` segue le stesse regole della registrazione;
   - token valido e non scaduto → aggiorna la password, `204`;
   - elimina **tutti i token di reset** e **tutti i refresh token** dell'utente (logout ovunque);
   - token inesistente o scaduto → `400` (`AUTH_RESET_TOKEN_INVALID`).

Gli access token già emessi restano validi fino alla scadenza (al massimo 15 minuti). Funziona anche per un account disabilitato, che però al login riceve comunque `403`.

## Verifica dell'email

Il token di verifica scade dopo 24 ore.

`POST /api/v1/auth/email/verify` con `{ "token": "..." }`:
- token valido: imposta `users.email_verified_at` ed elimina i token di verifica dell'utente, `204`;
- token inesistente o scaduto → `400` (`AUTH_VERIFICATION_TOKEN_INVALID`).

`POST /api/v1/auth/email/resend`, senza corpo: invia un nuovo link all'utente del token.
- È l'unico endpoint di `/auth/**` che **richiede l'access token** (`Authorization: Bearer ...`): senza token → `401`.
- Il link precedente smette di funzionare. Risposta `204`.
- Email già verificata → `409` (`AUTH_EMAIL_ALREADY_VERIFIED`); utente non più esistente → `404`.

## Pulizia

`TokenCleanupJob` (`@Scheduled`, ogni giorno alle 3:00, ora del server) elimina i token scaduti dalle tre tabelle (gli indici su `expires_at` servono a questo) e scrive nel log quante righe ha cancellato per tabella.

## Chi sono io

Nei controller l'utente corrente si ricava dal token, mai dalla richiesta:

```java
@GetMapping("/me")
public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
	Integer userId = Integer.valueOf(jwt.getSubject());
	return ResponseEntity.ok(userService.getProfile(userId));
}
```

`GET /api/v1/users/me` restituisce:

```json
{
  "id": 42,
  "username": "pioggia_inversa",
  "email": "mario@example.com",
  "role": "USER",
  "emailVerified": false
}
```

- `emailVerified` serve al frontend per mostrare l'avviso e il pulsante di reinvio della verifica.
- I **crediti** si aggiungeranno quando esisterà `inventories` (TODO in `UserResponse`).
- Il service converte l'entità nel DTO dentro la propria transazione: il ruolo è `LAZY` e `spring.jpa.open-in-view=false`, quindi fuori dalla transazione non si può leggere.
- Token valido di un utente eliminato nel frattempo → `404` (`USER_NOT_FOUND`).

## Codici di errore

Il formato è quello di [api-rest](../api-rest/api-rest.md); il campo `code` vale:

| `code` | Status | Quando |
|---|---|---|
| `VALIDATION_FAILED` | `400` | campi della richiesta non validi (elenco in `errors`) |
| `AUTH_RESET_TOKEN_INVALID` | `400` | link di reset inesistente o scaduto |
| `AUTH_VERIFICATION_TOKEN_INVALID` | `400` | link di verifica inesistente o scaduto |
| `AUTH_REQUIRED` | `401` | access token mancante, scaduto o non valido |
| `AUTH_INVALID_CREDENTIALS` | `401` | email o password errate |
| `AUTH_REFRESH_TOKEN_INVALID` | `401` | refresh token inesistente, scaduto o già usato |
| `AUTH_ACCOUNT_DISABLED` | `403` | account disabilitato |
| `ACCESS_DENIED` | `403` | ruolo insufficiente (es. `USER` su `/admin/**`) |
| `USER_NOT_FOUND` | `404` | l'utente del token non esiste più |
| `AUTH_USERNAME_TAKEN` | `409` | username già usato |
| `AUTH_EMAIL_TAKEN` | `409` | email già usata |
| `AUTH_EMAIL_ALREADY_VERIFIED` | `409` | reinvio della verifica con email già verificata |

## Endpoint pubblici e protetti

| Endpoint | Accesso |
|---|---|
| `/auth/**` | pubblico, tranne `/auth/email/resend` (utente autenticato) |
| `GET /cards/**`, `GET /packs`, `GET /factions`, `GET /rarities` | pubblico (servono anche alle pagine SEO) |
| Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) | pubblico in sviluppo |
| `/admin/**` | solo `ADMIN` |
| tutto il resto | utente autenticato |

## Configurazione di Spring Security (schema)

La configurazione completa è in `common/security/SecurityConfig.java`:

```java
http
	.csrf(csrf -> csrf.disable())                 // API stateless con token
	.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
	.authorizeHttpRequests(auth -> auth
		.requestMatchers("/api/v1/auth/email/resend").authenticated()   // prima di /auth/**: vince la prima regola che corrisponde
		.requestMatchers("/api/v1/auth/**").permitAll()
		.requestMatchers(HttpMethod.GET, "/api/v1/cards/**", "/api/v1/packs", "/api/v1/factions", "/api/v1/rarities").permitAll()
		.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
		.anyRequest().authenticated())
	.oauth2ResourceServer(o -> o.jwt(...));       // spring-boot-starter-oauth2-resource-server valida i JWT
```

- Il claim `role` del JWT diventa l'authority `ROLE_USER` / `ROLE_ADMIN`.
- I `401` e `403` dei filtri di sicurezza vengono inoltrati a `GlobalExceptionHandler`, quindi hanno lo stesso formato ProblemDetail di tutti gli altri errori. Il `401` ha anche l'header `WWW-Authenticate: Bearer`.
- CORS: le origini del frontend ammesse (es. `http://localhost:3000`) sono in `reversky.security.allowed-origins`.

## Swagger

In Swagger UI il pulsante **Authorize** accetta l'access token (solo il token, senza `Bearer `). Lo schema si chiama `bearerAuth` ed è dichiarato in `common/config/OpenApiConfig.java`; gli endpoint protetti lo richiamano con `@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)`, sul metodo (come `resend`) o sull'intero controller (come `UserController`).
