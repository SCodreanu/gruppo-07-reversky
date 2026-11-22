package it.downfall.reversky.api.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Genera i token casuali (refresh, reset password, verifica email) e ne calcola l'hash.
 * <p>
 * Nel database si salva solo l'hash SHA-256: chi legge il database non può usare i token.
 * Non si usa BCrypt come per le password perché il token va <b>cercato</b> per hash,
 * e BCrypt produce un hash diverso a ogni chiamata (docs/autenticazione).
 */
@Component
public class TokenHasher {

	// 32 byte = 256 bit di casualità: impossibile da indovinare
	private static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * Genera un nuovo token casuale da inviare all'utente.
	 *
	 * @return 32 byte casuali in Base64 URL-safe, senza padding (43 caratteri)
	 */
	public String generateToken() {

		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * Calcola l'hash da salvare (o da cercare) nel database.
	 *
	 * @param rawToken token in chiaro ricevuto dall'utente
	 * @return SHA-256 in esadecimale minuscolo (64 caratteri)
	 */
	public String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
