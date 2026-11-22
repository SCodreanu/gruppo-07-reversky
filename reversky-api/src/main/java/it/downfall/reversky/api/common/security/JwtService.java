package it.downfall.reversky.api.common.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import it.downfall.reversky.api.user.entity.User;
import lombok.RequiredArgsConstructor;

/**
 * Crea gli access token JWT (durata 15 minuti, firmati HS256).
 * <p>
 * Contenuto del token (docs/autenticazione): {@code sub} = id utente, {@code username}, {@code role},
 * {@code iat} e {@code exp}. La validazione dei token in arrivo la fa il resource server di Spring
 * (vedi {@link SecurityConfig}).
 */
@Service
@RequiredArgsConstructor
public class JwtService {

	// Nomi dei claim: li legge anche SecurityConfig per ricavare il ruolo
	public static final String CLAIM_USERNAME = "username";
	public static final String CLAIM_ROLE = "role";

	private final JwtEncoder jwtEncoder;
	private final SecurityProperties securityProperties;
	private final Clock clock;

	/**
	 * Crea un access token per l'utente.
	 * <p>
	 * Il ruolo deve essere già caricato (lo è con l'@EntityGraph dei repository): il metodo va
	 * chiamato dentro la transazione del service.
	 *
	 * @param user utente autenticato
	 * @return il JWT firmato, da inviare nell'header {@code Authorization: Bearer ...}
	 */
	public String createAccessToken(User user) {
		Instant now = clock.instant();

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(String.valueOf(user.getId()))
			.issuedAt(now)
			.expiresAt(now.plus(securityProperties.accessTokenTtl()))
			.claim(CLAIM_USERNAME, user.getUsername())
			.claim(CLAIM_ROLE, user.getRole().getName())
			.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/**
	 * @return durata dell'access token in secondi, per il campo {@code expiresIn} della risposta di login
	 */
	public long getAccessTokenExpiresInSeconds() {
		return securityProperties.accessTokenTtl().toSeconds();
	}
}
