package it.downfall.reversky.api.battle.exception;

import java.util.Map;
import java.util.UUID;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * L'engine ha rifiutato la richiesta di simulazione con {@code 400} (docs/sfide, API-71): l'API ha mandato dati che
 * l'engine non accetta, quindi è un errore nostro e non un engine spento.
 * <p>
 * La partita resta preparata e senza risultato, ma il retry manda la stessa fotografia dei mazzi e riceve lo stesso
 * rifiuto: per questo è {@code 500} e non {@code 503} ("riprova più tardi"). Il codice e il messaggio dell'engine
 * vanno solo nel log, con il seed per riprodurre la partita; quando la partita è già preparata, la risposta porta
 * anche {@code battleId}.
 */
public class EngineInputRejectedException extends BusinessException {

	private final UUID battleId;

	public EngineInputRejectedException() {
		this(null);
	}

	private EngineInputRejectedException(UUID battleId) {
		super(Category.INTERNAL_ERROR, "ENGINE_INPUT_REJECTED", "Battle rejected by the game engine",
			"The game engine could not simulate this battle. Please report it to the staff");
		this.battleId = battleId;
	}

	/**
	 * La stessa eccezione con l'id della partita rimasta senza risultato.
	 */
	public EngineInputRejectedException withBattleId(UUID battleId) {
		return new EngineInputRejectedException(battleId);
	}

	@Override
	public Map<String, Object> getExtraProperties() {
		return battleId == null ? Map.of() : Map.of("battleId", battleId);
	}
}
