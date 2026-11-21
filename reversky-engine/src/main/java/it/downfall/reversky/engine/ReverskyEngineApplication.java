package it.downfall.reversky.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto di avvio del motore di gioco: avvia lo strato {@code web} su Spring Boot (porta 8081). Le regole stanno
 * nel package {@code core}, che non dipende da Spring.
 */
@SpringBootApplication
public class ReverskyEngineApplication {

	/**
	 * Costruttore pubblico: Spring crea questa classe come configurazione.
	 */
	public ReverskyEngineApplication() {
	}

	/**
	 * Avvia l'applicazione.
	 *
	 * @param args argomenti della riga di comando, passati a Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(ReverskyEngineApplication.class, args);
	}
}
