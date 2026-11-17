package it.downfall.reversky.engine.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.downfall.reversky.engine.core.simulation.MatchSimulator;

/**
 * Crea i bean del core: il core non conosce Spring, quindi le sue classi non hanno {@code @Component}
 * (CodingRequirements §4). L'{@code EffectRegistry} si aggiunge qui con {@code ENG-22}.
 */
@Configuration
public class EngineConfiguration {

	/**
	 * Il simulatore non conserva stato tra le partite: una sola istanza serve tutte le richieste.
	 *
	 * @return il simulatore
	 */
	@Bean
	MatchSimulator matchSimulator() {
		return new MatchSimulator();
	}
}
