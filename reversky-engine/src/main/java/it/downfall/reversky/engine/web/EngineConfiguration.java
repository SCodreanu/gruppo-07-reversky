package it.downfall.reversky.engine.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.simulation.MatchSimulator;

/**
 * Crea i bean del core: il core non conosce Spring, quindi le sue classi non hanno {@code @Component}
 * (CodingRequirements §4).
 */
@Configuration
public class EngineConfiguration {

	/**
	 * Registro con tutti gli effetti implementati. Si crea all'avvio e poi si legge soltanto, quindi una sola istanza
	 * serve tutte le richieste.
	 *
	 * @return il registro del gioco
	 */
	@Bean
	EffectRegistry effectRegistry() {
		return EffectRegistry.withAllEffects();
	}

	/**
	 * Il simulatore non conserva stato tra le partite: una sola istanza serve tutte le richieste.
	 *
	 * @param effects registro degli effetti
	 * @return il simulatore
	 */
	@Bean
	MatchSimulator matchSimulator(EffectRegistry effects) {
		return new MatchSimulator(effects);
	}
}
