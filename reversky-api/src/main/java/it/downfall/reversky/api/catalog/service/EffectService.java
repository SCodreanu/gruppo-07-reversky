package it.downfall.reversky.api.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.catalog.dto.EffectResponse;
import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.Effect;
import it.downfall.reversky.api.catalog.exception.InvalidCardEffectException;
import it.downfall.reversky.api.catalog.repository.EffectRepository;
import lombok.RequiredArgsConstructor;

/**
 * Effetti implementati dall'engine, letti dalla tabella {@code effects} (docs/backoffice, regola 2, API-36).
 * <p>
 * L'API non chiede mai l'elenco all'engine, che si chiama solo per simulare: le righe arrivano da migrazione insieme
 * al codice dell'effetto nell'engine.
 */
@Service
@RequiredArgsConstructor
public class EffectService {

	private final EffectRepository effectRepository;

	/**
	 * Tutti gli effetti (GET /admin/effects), per tipo di carta e poi per codice.
	 */
	@Transactional(readOnly = true)
	public List<EffectResponse> findAll() {
		return effectRepository.findAllByOrderByAppliesToAscCodeAsc().stream()
			.map(EffectResponse::from)
			.toList();
	}

	/**
	 * Controlla che l'effetto esista e sia del tipo della carta: la stessa regola della chiave esterna
	 * {@code fk_catalog_cards_effect}, controllata prima per rispondere 422 invece di un errore del database.
	 *
	 * @param type       tipo della carta
	 * @param effectCode codice da assegnare, mai null
	 * @throws InvalidCardEffectException se il codice non esiste o è di un altro tipo di carta
	 */
	@Transactional(readOnly = true)
	public void validateForType(CardType type, String effectCode) {
		Effect effect = effectRepository.findById(effectCode)
			.orElseThrow(() -> InvalidCardEffectException.unknown(effectCode));

		if (effect.getAppliesTo() != type) {
			throw InvalidCardEffectException.wrongType(effectCode, effect.getAppliesTo(), type);
		}
	}
}
