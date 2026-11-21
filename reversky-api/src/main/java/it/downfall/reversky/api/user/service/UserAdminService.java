package it.downfall.reversky.api.user.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.auth.service.AuthService;
import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.inventory.service.InventoryService;
import it.downfall.reversky.api.user.dto.AdminUserResponse;
import it.downfall.reversky.api.user.entity.Role;
import it.downfall.reversky.api.user.entity.User;
import it.downfall.reversky.api.user.exception.AdminSelfActionException;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.repository.RoleRepository;
import it.downfall.reversky.api.user.repository.UserRepository;
import it.downfall.reversky.api.user.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestione degli utenti dal backoffice (docs/backoffice, "Utenti", task API-03).
 * <p>
 * È separato da {@link UserService} perché deve chiudere le sessioni con {@link AuthService}, che a sua volta usa
 * {@link UserService}: nello stesso service le due dipendenze formerebbero un ciclo. Le azioni sono idempotenti:
 * disattivare un account già disattivato o assegnare lo stesso ruolo non cambia niente e restituisce l'utente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

	private static final Sort USER_ORDER = Sort.by("id");

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final AuthService authService;
	private final InventoryService inventoryService;

	/**
	 * Utenti filtrati e paginati in ordine di id, con ruolo e saldo (tre query in tutto: utenti, conteggio, saldi).
	 *
	 * @param text    testo cercato in username o email, senza distinguere maiuscole e minuscole
	 * @param role    nome del ruolo; un ruolo inesistente dà una pagina vuota
	 * @param enabled true solo attivi, false solo disattivati, null tutti
	 */
	@Transactional(readOnly = true)
	public PageResponse<AdminUserResponse> findAll(String text, String role, Boolean enabled, Pageable pageable) {
		Specification<User> specification = Specification.allOf(
			UserSpecifications.usernameOrEmailContains(text),
			UserSpecifications.hasRole(role),
			UserSpecifications.isEnabled(enabled));

		Page<User> users = userRepository.findAll(specification,
			PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), USER_ORDER));
		Map<Integer, Integer> credits = inventoryService.getCreditsByUserIds(users.map(User::getId).getContent());

		return PageResponse.from(users.map(user -> AdminUserResponse.from(user, credits.getOrDefault(user.getId(), 0))));
	}

	/**
	 * Disattiva un account e chiude tutte le sue sessioni (refresh token cancellati).
	 *
	 * @param adminId id dell'admin che fa la richiesta, dal token
	 * @throws AdminSelfActionException se l'admin prova a disattivare se stesso
	 * @throws UserNotFoundException    se l'utente non esiste
	 */
	@Transactional
	public AdminUserResponse disable(Integer adminId, Integer userId) {
		if (adminId.equals(userId)) {
			throw new AdminSelfActionException("You cannot disable your own account");
		}

		User user = getUser(userId);
		user.disable();
		authService.revokeAllSessions(userId);
		// TODO(@antoninoconsolato-its): cancellare anche il ticket in matchmaking_queue con API-47 (#117)

		log.info("User {} disabled by admin {}", userId, adminId);
		return toResponse(user);
	}

	/**
	 * Riattiva un account.
	 *
	 * @throws UserNotFoundException se l'utente non esiste
	 */
	@Transactional
	public AdminUserResponse enable(Integer adminId, Integer userId) {
		User user = getUser(userId);
		user.enable();

		log.info("User {} enabled by admin {}", userId, adminId);
		return toResponse(user);
	}

	/**
	 * Cambia il ruolo di un utente. Vale dal prossimo access token: quelli già emessi tengono il ruolo vecchio fino
	 * alla scadenza.
	 *
	 * @param roleName {@code USER} o {@code ADMIN} (lo controlla la richiesta)
	 * @throws AdminSelfActionException se l'admin prova a togliersi il ruolo ADMIN
	 * @throws UserNotFoundException    se l'utente non esiste
	 */
	@Transactional
	public AdminUserResponse changeRole(Integer adminId, Integer userId, String roleName) {
		if (adminId.equals(userId) && !Role.ADMIN.equals(roleName)) {
			throw new AdminSelfActionException("You cannot remove the ADMIN role from your own account");
		}

		User user = getUser(userId);
		Role role = roleRepository.findByName(roleName)
			.orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found: check migration V2"));
		user.changeRole(role);

		log.info("Role of user {} set to {} by admin {}", userId, roleName, adminId);
		return toResponse(user);
	}

	private User getUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException());
	}

	private AdminUserResponse toResponse(User user) {
		return AdminUserResponse.from(user, inventoryService.getCredits(user.getId()));
	}
}
