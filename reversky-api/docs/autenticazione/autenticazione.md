# Autenticazione e utenti

## Cosa fa

- Registrazione con accettazione di privacy e termini.
- Login con **access token JWT** (breve) + **refresh token** (lungo, salvato come hash).
- Logout, rinnovo del token, reset della password, verifica dell'email.
- Ruoli con livelli di autorità (`roles.authority_level`).

Tabelle: `roles`, `users`, `refresh_tokens`, `password_reset_tokens`, `email_verification_tokens`, `inventories`.

## Ruoli

| `name` | `authority_level` | Uso |
|---|---|---|
| `USER` | 10 | giocatore |
| `ADMIN` | 100 | backoffice |

- I livelli sono **univoci** e rappresentano la gerarchia: un controllo "almeno ADMIN" diventa `authority_level >= 100`.
- In Spring Security il ruolo diventa l'authority `ROLE_<name>`.
- Gli `ADMIN` si creano solo da migrazione o dal backoffice, mai dalla registrazione pubblica.

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
- `username`: 3–20 caratteri (lettere, numeri, `_`), **univoco**.
- `email`: formato valido, **univoca**.
- `password`: minimo 8 caratteri, almeno una lettera e un numero; salvata solo come hash **BCrypt**.
- `acceptPrivacy` e `acceptTerms` devono essere `true`: si salvano `privacy_accepted_at` e `terms_accepted_at`.

Nella **stessa transazione**:
1. crea l'utente con ruolo `USER`;
2. crea il suo `inventories` con i crediti di benvenuto (vedi [crediti](../crediti/crediti.md));
3. crea un token di verifica email e invia la mail.

Risposta `201 Created`. Username o email già usati → `409 Conflict`.

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

- Credenziali errate → `401` con messaggio generico ("Invalid credentials"), **senza dire** se è sbagliata l'email o la password.
- Utente con `is_enabled = false` → `403` ("Account disabled").

## Token

| Token | Durata | Dove vive |
|---|---|---|
| Access token (JWT) | 15 minuti | solo nel client; mai salvato nel DB |
| Refresh token | 30 giorni | client + **hash** in `refresh_tokens` |

- JWT firmato HS256 con chiave da variabile d'ambiente (`reversky.security.jwt-secret=${JWT_SECRET}`), mai committata.
- Contenuto del JWT: `sub` = id utente, `username`, `role`, scadenza.
- I token salvati nel DB (refresh, reset, verifica) sono **hash SHA-256** del valore casuale: si cerca per hash, quindi non si usa BCrypt per loro.

```java
String rawToken = generateSecureToken();          // 32 byte casuali, Base64 URL
String tokenHash = sha256(rawToken);              // questo va nel database
```

### Rinnovo

`POST /api/v1/auth/refresh` con `{ "refreshToken": "..." }`
- cerca l'hash, controlla la scadenza;
- **rotazione**: elimina il vecchio refresh token e ne crea uno nuovo;
- restituisce un nuovo access token e un nuovo refresh token.

### Logout

`POST /api/v1/auth/logout` con `{ "refreshToken": "..." }` → elimina quel refresh token. `204`.

## Reset della password

1. `POST /api/v1/auth/password/forgot` con `{ "email": "..." }`
   - risponde **sempre** `204`, anche se l'email non esiste (non rivela quali email sono registrate);
   - se l'utente esiste: crea un token (scadenza 30 minuti) e invia la mail con il link.
2. `POST /api/v1/auth/password/reset` con `{ "token": "...", "newPassword": "..." }`
   - token valido e non scaduto → aggiorna la password;
   - elimina il token usato e **tutti i refresh token** dell'utente (logout ovunque).

## Verifica dell'email

`POST /api/v1/auth/email/verify` con `{ "token": "..." }` → token valido: imposta `users.email_verified_at` ed elimina il token.
`POST /api/v1/auth/email/resend` → nuovo token per l'utente autenticato (solo se l'email non è ancora verificata).

## Pulizia

Un job schedulato (`@Scheduled`, una volta al giorno) elimina i token scaduti dalle tre tabelle (gli indici su `expires_at` servono a questo).

## Chi sono io

Nei controller l'utente corrente si ricava dal token, mai dalla richiesta:

```java
@GetMapping("/me")
public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
	Integer userId = Integer.valueOf(jwt.getSubject());
	return userService.getProfile(userId);
}
```

`GET /api/v1/users/me` restituisce: id, username, email, ruolo, crediti.

## Endpoint pubblici e protetti

| Endpoint | Accesso |
|---|---|
| `/auth/**` | pubblico |
| `GET /cards/**`, `GET /packs` | pubblico (servono anche alle pagine SEO) |
| Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) | pubblico in sviluppo |
| `/admin/**` | solo `ADMIN` |
| tutto il resto | utente autenticato |

## Configurazione di Spring Security (schema)

```java
http
	.csrf(csrf -> csrf.disable())                 // API stateless con token
	.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
	.authorizeHttpRequests(auth -> auth
		.requestMatchers("/api/v1/auth/**").permitAll()
		.requestMatchers(HttpMethod.GET, "/api/v1/cards/**", "/api/v1/packs").permitAll()
		.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
		.anyRequest().authenticated())
	.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
```

Implementazione suggerita: `spring-boot-starter-oauth2-resource-server` per validare i JWT.
CORS: abilitare l'origine del frontend (es. `http://localhost:3000`) da configurazione.
