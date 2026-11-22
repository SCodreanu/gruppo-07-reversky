package it.downfall.reversky.api.common.config;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Generatore casuale del server (docs/pacchetti, "Casualità"): l'apertura dei pacchetti lo riceve iniettato, così i
 * test possono sostituirlo con uno a seed fisso. Il client non invia mai seed né risultati.
 */
@Configuration
public class RandomConfig {

	@Bean
	public RandomGenerator randomGenerator() {
		return new SecureRandom();
	}
}
