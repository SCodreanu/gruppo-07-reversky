package it.downfall.reversky.api.support;

import org.springframework.stereotype.Component;

import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.user.entity.User;
import lombok.RequiredArgsConstructor;

/**
 * Helper per i test delle amicizie.
 * <p>
 * Si aggiunge al test con {@code @Import({TestcontainersConfiguration.class, TestUsers.class, TestFriends.class})}.
 * Passa per {@link FriendshipService}, così i test restano fedeli al comportamento di produzione.
 */
@Component
@RequiredArgsConstructor
public class TestFriends {

	private final FriendshipService friendshipService;

	/** Crea un'amicizia ACCEPTED tra i due utenti. */
	public void makeFriends(User a, User b) {
		friendshipService.sendRequest(a.getId(), b.getUsername());
		friendshipService.accept(b.getId(), a.getId());
	}
}