package it.downfall.reversky.api.battle.client;

import java.net.http.HttpClient;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import it.downfall.reversky.api.battle.exception.EngineInputRejectedException;
import it.downfall.reversky.api.battle.exception.EngineUnavailableException;
import lombok.extern.slf4j.Slf4j;

/**
 * L'unico punto dell'API che conosce l'engine (docs/sfide, "La chiamata all'engine", API-35).
 * <p>
 * Engine spento, timeout o risposta {@code 5xx} diventano {@link EngineUnavailableException} ({@code 503}): la
 * partita resta senza risultato e si completa con il retry. Una risposta {@code 4xx} vuol dire che l'engine ha
 * rifiutato i dati della partita (API-71): diventa {@link EngineInputRejectedException} ({@code 500}), perché il retry
 * manderebbe gli stessi dati. Chiama solo {@code POST /api/v1/simulations}. Nei test questo bean si sostituisce con
 * un mock.
 */
@Slf4j
@Component
public class EngineClient {

	private final RestClient restClient;

	public EngineClient(EngineProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.timeout());

		this.restClient = RestClient.builder()
			.baseUrl(properties.baseUrl())
			.requestFactory(requestFactory)
			.build();
	}

	/**
	 * Chiede all'engine di simulare una partita.
	 *
	 * @throws EngineInputRejectedException se l'engine risponde {@code 4xx}: ha rifiutato i dati della partita
	 *                                      (API-71), e ripetere la stessa richiesta darebbe lo stesso rifiuto
	 * @throws EngineUnavailableException   se l'engine non risponde, va in timeout o risponde con un {@code 5xx}
	 */
	public SimulationResponse simulate(SimulationRequest request) {
		try {
			SimulationResponse response = restClient.post()
				.uri("/api/v1/simulations")
				.body(request)
				.retrieve()
				.body(SimulationResponse.class);
			if (response == null || response.result() == null) {
				throw new EngineUnavailableException("The engine returned an empty simulation");
			}
			return response;
		} catch (HttpClientErrorException e) {
			// Errore nostro: codice, messaggio e seed nel log per correggere i dati e riprodurre la partita
			EngineProblem problem = readProblem(e);
			log.error("Engine rejected the simulation for seed {}: {} {} ({})", request.seed(), e.getStatusCode().value(),
				problem.code(), problem.detail());
			throw new EngineInputRejectedException();
		} catch (RestClientException e) {
			// Il seed nel log permette di riprodurre la partita sull'engine
			log.error("Engine simulation failed for seed {}: {}", request.seed(), e.getMessage());
			throw new EngineUnavailableException("The game engine is not available, try again later");
		}
	}

	// Un corpo che non è un ProblemDetail leggibile non deve nascondere il rifiuto: si logga senza codice
	private static EngineProblem readProblem(HttpClientErrorException e) {
		try {
			EngineProblem problem = e.getResponseBodyAs(EngineProblem.class);
			return problem != null ? problem : new EngineProblem(null, e.getResponseBodyAsString());
		} catch (RuntimeException unreadable) {
			return new EngineProblem(null, e.getResponseBodyAsString());
		}
	}
}
