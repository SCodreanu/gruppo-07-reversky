package it.downfall.reversky.api.common.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Legge l'access token dall'header {@code Authorization}, ma sugli endpoint pubblici ignora un token scaduto o non
 * valido (API-58, docs/autenticazione).
 * <p>
 * Spring Security autentica ogni richiesta che ha l'header, anche su un endpoint pubblico, e un token scaduto dà
 * {@code 401}: un frontend che manda sempre l'header non potrebbe più rinnovare il token né fare login. Sugli
 * endpoint pubblici il token si usa solo se è valido (per esempio {@code GET /packs}, che con il token mostra i
 * pacchetti che restano da comprare); altrimenti la richiesta continua come se l'header non ci fosse. Sugli endpoint
 * protetti non cambia niente: un token scaduto resta un {@code 401}.
 */
public class PublicPathBearerTokenResolver implements BearerTokenResolver {

	private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();
	private final RequestMatcher publicPaths;
	private final JwtDecoder jwtDecoder;

	/**
	 * @param publicPaths endpoint che non richiedono il token, gli stessi di {@code permitAll()}
	 * @param jwtDecoder  il decoder dei JWT dell'applicazione, per sapere se il token è valido
	 */
	public PublicPathBearerTokenResolver(RequestMatcher publicPaths, JwtDecoder jwtDecoder) {
		this.publicPaths = publicPaths;
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public String resolve(HttpServletRequest request) {
		boolean isPublic = publicPaths.matches(request);
		String token;
		try {
			token = delegate.resolve(request);
		} catch (OAuth2AuthenticationException e) {
			// Header malformato (es. due token o caratteri non ammessi): sugli endpoint pubblici si ignora anche questo
			if (isPublic) {
				return null;
			}
			throw e;
		}
		if (token == null || !isPublic) {
			return token;
		}
		try {
			jwtDecoder.decode(token);
			return token;
		} catch (JwtException e) {
			// Scaduto, firmato con un'altra chiave o non è un JWT: su un endpoint pubblico vale come nessun token
			return null;
		}
	}
}
