package it.downfall.reversky.api.battle.exception;

import java.util.UUID;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * La partita non esiste o l'utente non l'ha giocata (docs/sfide): nei due casi la risposta è la stessa.
 */
public class BattleNotFoundException extends BusinessException {

	public BattleNotFoundException(UUID battleId) {
		super(Category.NOT_FOUND, "BATTLE_NOT_FOUND", "Battle not found",
			"Battle " + battleId + " does not exist");
	}
}
