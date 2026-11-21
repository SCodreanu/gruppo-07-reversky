package it.downfall.reversky.api.battle.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * C'è già un invito in attesa verso lo stesso avversario (uq_battle_invites_pending_pair, docs/sfide).
 */
public class InviteAlreadyPendingException extends BusinessException {

	public InviteAlreadyPendingException() {
		super(Category.CONFLICT, "INVITE_ALREADY_PENDING", "Challenge already pending",
			"You already have a pending challenge to this player");
	}
}
