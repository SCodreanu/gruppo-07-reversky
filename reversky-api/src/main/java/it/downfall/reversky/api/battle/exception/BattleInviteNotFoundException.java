package it.downfall.reversky.api.battle.exception;

import java.util.UUID;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'invito non esiste, non riguarda l'utente o non è più in attesa (docs/sfide): nei tre casi la risposta è la stessa.
 */
public class BattleInviteNotFoundException extends BusinessException {

	public BattleInviteNotFoundException(UUID inviteId) {
		super(Category.NOT_FOUND, "BATTLE_INVITE_NOT_FOUND", "Challenge not found",
			"Challenge " + inviteId + " does not exist or is no longer pending");
	}
}
