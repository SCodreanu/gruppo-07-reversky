package it.downfall.reversky.api.battle.exception;

import java.util.UUID;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'invito è già stato accettato e ha già la sua partita (uq_battles_invite_id, docs/sfide).
 */
public class InviteAlreadyAcceptedException extends BusinessException {

	public InviteAlreadyAcceptedException(UUID inviteId) {
		super(Category.CONFLICT, "BATTLE_INVITE_ALREADY_ACCEPTED", "Challenge already accepted",
			"Challenge " + inviteId + " has already been accepted");
	}
}
