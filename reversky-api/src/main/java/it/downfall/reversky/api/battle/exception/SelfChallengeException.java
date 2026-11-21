package it.downfall.reversky.api.battle.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * Un utente prova a sfidare se stesso (chk_battle_invites_no_self_challenge, docs/sfide).
 */
public class SelfChallengeException extends BusinessException {

	public SelfChallengeException() {
		super(Category.RULE_VIOLATION, "BATTLE_SELF_CHALLENGE", "You cannot challenge yourself",
			"You cannot challenge yourself");
	}
}
