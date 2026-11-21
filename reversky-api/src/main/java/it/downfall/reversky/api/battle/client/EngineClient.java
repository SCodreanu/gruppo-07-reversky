package it.downfall.reversky.api.battle.client;

import java.net.http.HttpClient;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import it.downfall.reversky.api.battle.exception.EngineUnavailableException;
import lombok.extern.slf4j.Slf4j;

/**
 * L'unico punto dell'API che conosce l'engine (docs/sfide, "La chiamata all'engine", API-35).
 * <p>
 * Qualsiasi problema (engine spento, timeout, risposta di errore) diventa {@link EngineUnavailableException}: la
 * partita resta senza risultato e si completa con il retry. Nei test questo bean si sostituisce con un mock.
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
	 * @throws EngineUnavailableException se l'engine non risponde, va in timeout o risponde con un errore
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
		} catch (RestClientException e) {
			// Il seed nel log permette di riprodurre la partita sull'engine
			log.error("Engine simulation failed for seed {}: {}", request.seed(), e.getMessage());
			throw new EngineUnavailableException("The game engine is not available, try again later");
		}
	}
}
