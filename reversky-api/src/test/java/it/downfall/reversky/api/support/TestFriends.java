package it.downfall.reversky.api.support;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import it.downfall.reversky.api.friendship.service.FriendshipService;
import it.downfall.reversky.api.user.entity.User;

/**
 * Helper per i test delle amicizie (docs/amicizie).
 * <p>
 * Come {@link TestRuleSets} non è un {@code @TestComponent}: si crea nel test con
 * {@code new TestFriends(friendshipService, jdbcTemplate)}, così {@code @Import} resta
 * {@code {TestcontainersConfiguration.class, TestUsers.class}} come negli altri IT e Spring riusa
 * lo stesso contesto (e lo stesso container).
 * <p>
 * Le amicizie passano per {@link FriendshipService}, così i test restano fedeli al comportamento di
 * produzione. Mazzi e inviti di sfida si scrivono con SQL finché non esistono le entità
 * {@code Deck} e {@code BattleInvite}: servono solo a verificare la regola 7 (rimuovere un amico
 * elimina gli inviti {@code PENDING}).
 */
public class TestFriends {

	private final FriendshipService friendshipService;
	private final JdbcTemplate jdbcTemplate;

	public TestFriends(FriendshipService friendshipService, JdbcTemplate jdbcTemplate) {
		this.friendshipService = friendshipService;
		this.jdbcTemplate = jdbcTemplate;
	}

	/** Crea un'amicizia ACCEPTED: {@code a} invia la richiesta, {@code b} la accetta. */
	public void makeFriends(User a, User b) {
		friendshipService.sendRequest(a.getId(), b.getUsername());
		friendshipService.accept(b.getId(), a.getId());
	}

	/** Invia una richiesta PENDING da {@code requester} a {@code recipient}. */
	public void sendRequest(User requester, User recipient) {
		friendshipService.sendRequest(requester.getId(), recipient.getUsername());
	}

	/** Numero di righe in {@code friendships} tra i due utenti, in qualunque direzione. */
	public int countRowsBetween(User a, User b) {
		Integer count = jdbcTemplate.queryForObject("""
			SELECT COUNT(*) FROM friendships
			WHERE (requester_id = ? AND recipient_id = ?) OR (requester_id = ? AND recipient_id = ?)
			""", Integer.class, a.getId(), b.getId(), b.getId(), a.getId());
		return count == null ? 0 : count;
	}

	/**
	 * Crea un invito di sfida da {@code challenger} a {@code opponent} con lo stato indicato
	 * ({@code PENDING} o {@code ACCEPTED}) e un mazzo vuoto dello sfidante.
	 *
	 * @return l'id dell'invito
	 */
	public UUID createBattleInvite(User challenger, User opponent, String status) {
		Integer deckId = jdbcTemplate.queryForObject(
			"INSERT INTO decks (user_id, name) VALUES (?, ?) RETURNING id",
			Integer.class, challenger.getId(), "Deck " + TestUsers.uniqueSuffix());

		UUID inviteId = UUID.randomUUID();
		jdbcTemplate.update("""
			INSERT INTO battle_invites (id, challenger_id, challenger_deck_id, opponent_id, status)
			VALUES (?, ?, ?, ?, CAST(? AS battle_invite_status))
			""", inviteId, challenger.getId(), deckId, opponent.getId(), status);
		return inviteId;
	}

	/** {@code true} se l'invito di sfida esiste ancora. */
	public boolean battleInviteExists(UUID inviteId) {
		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM battle_invites WHERE id = ?", Integer.class, inviteId);
		return count != null && count > 0;
	}
}
