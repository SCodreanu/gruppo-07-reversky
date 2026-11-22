package it.downfall.reversky.api.user.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import it.downfall.reversky.api.user.entity.User;

/**
 * Filtri di GET /admin/users (docs/backoffice, "Utenti").
 * <p>
 * Ogni metodo restituisce un predicato null quando il filtro è vuoto: Spring Data lo salta.
 */
public final class UserSpecifications {

	private UserSpecifications() {
		// Solo metodi statici
	}

	/**
	 * Username o email che contengono il testo, senza distinguere maiuscole e minuscole; testo vuoto = nessun filtro.
	 */
	public static Specification<User> usernameOrEmailContains(String text) {
		return (root, query, builder) -> {
			if (text == null || text.isBlank()) {
				return null;
			}

			String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
			return builder.or(
				builder.like(builder.lower(root.get("username")), pattern),
				builder.like(builder.lower(root.get("email")), pattern));
		};
	}

	/**
	 * Solo utenti con quel ruolo; ruolo vuoto = nessun filtro. Un ruolo che non esiste dà una pagina vuota.
	 */
	public static Specification<User> hasRole(String roleName) {
		return (root, query, builder) -> {
			if (roleName == null || roleName.isBlank()) {
				return null;
			}

			return builder.equal(root.get("role").get("name"), roleName.trim().toUpperCase(Locale.ROOT));
		};
	}

	/**
	 * true = solo account attivi, false = solo disattivati, null = tutti.
	 */
	public static Specification<User> isEnabled(Boolean enabled) {
		return (root, query, builder) -> {
			if (enabled == null) {
				return null;
			}

			return builder.equal(root.get("enabled"), enabled);
		};
	}
}
