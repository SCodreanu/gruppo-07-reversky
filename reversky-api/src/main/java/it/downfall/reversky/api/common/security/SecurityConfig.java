package it.downfall.reversky.api.common.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import it.downfall.reversky.api.user.entity.Role;
import lombok.RequiredArgsConstructor;

/**
 * Configurazione di Spring Security: API stateless protetta da JWT (docs/autenticazione).
 * <p>
 * Gli errori 401/403 non vengono scritti qui: sono inoltrati a {@code GlobalExceptionHandler},
 * così hanno lo stesso formato ProblemDetail di tutti gli altri errori.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final SecurityProperties securityProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {

		// Inoltrano l'errore a GlobalExceptionHandler, come se fosse stato lanciato da un controller
		AuthenticationEntryPoint entryPoint = (request, response, ex) -> exceptionResolver.resolveException(request, response, null, ex);
		AccessDeniedHandler accessDeniedHandler = (request, response, ex) -> exceptionResolver.resolveException(request, response, null, ex);

		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> { })
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// Il reinvio vale per l'utente del token, quindi senza token risponde 401.
				.requestMatchers("/api/v1/auth/email/resend").authenticated()
				.requestMatchers("/api/v1/auth/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/cards/**", "/api/v1/packs", "/api/v1/factions").permitAll()
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN)
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.exceptionHandling(handling -> handling
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler));

		return http.build();
	}

	/**
	 * Legge il claim "role" del JWT e lo trasforma nell'authority ROLE_USER / ROLE_ADMIN,
	 * quella che controllano hasRole(...) e @PreAuthorize.
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

		authoritiesConverter.setAuthoritiesClaimName(JwtService.CLAIM_ROLE);
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

		return converter;
	}

	/** Firma i JWT (usato da JwtService). */
	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey()));
	}

	/** Valida firma e scadenza dei JWT in arrivo. */
	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey()).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** Hash delle password (docs/autenticazione: solo BCrypt). */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Origini del frontend ammesse (reversky.security.allowed-origins). */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(securityProperties.allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	// Stessa chiave per firmare e per validare: il segreto (almeno 32 byte) da JWT_SECRET
	private SecretKey jwtSecretKey() {
		byte[] keyBytes = securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}
}
