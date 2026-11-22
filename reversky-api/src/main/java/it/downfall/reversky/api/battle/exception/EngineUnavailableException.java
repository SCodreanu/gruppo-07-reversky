package it.downfall.reversky.api.battle.exception;

import java.util.Map;
import java.util.UUID;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'engine non ha simulato la partita (docs/sfide): non risponde, va in timeout o risponde con un errore.
 * <p>
 * Quando la partita è già stata preparata, la risposta porta anche {@code battleId}: la partita resta senza risultato
 * e si completa con {@code POST /api/v1/battles/{id}/retry}.
 */
public class EngineUnavailableException extends BusinessException {

	private final UUID battleId;

	public EngineUnavailableException(String detail) {
		this(detail, null);
	}

	private EngineUnavailableException(String detail, UUID battleId) {
		super(Category.SERVICE_UNAVAILABLE, "ENGINE_UNAVAILABLE", "Game engine unavailable", detail);
		this.battleId = battleId;
	}

	/**
	 * La stessa eccezione con l'id della partita rimasta da completare.
	 */
	public EngineUnavailableException withBattleId(UUID battleId) {
		return new EngineUnavailableException(getMessage(), battleId);
	}

	@Override
	public Map<String, Object> getExtraProperties() {
		return battleId == null ? Map.of() : Map.of("battleId", battleId);
	}
}
