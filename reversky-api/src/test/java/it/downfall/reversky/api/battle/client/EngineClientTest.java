package it.downfall.reversky.api.battle.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import it.downfall.reversky.api.battle.exception.EngineInputRejectedException;
import it.downfall.reversky.api.battle.exception.EngineUnavailableException;

/**
 * {@link EngineClient} contro un server HTTP finto del JDK (API-35): il contratto JSON e gli errori.
 */
class EngineClientTest {

	private static final String RESPONSE = """
		{ "result": "CHALLENGER_WIN", "endReason": "LIFE_POINTS_ZERO", "totalRounds": 37,
		  "challenger": { "userId": 7, "finalLifePoints": 9, "creaturesDefeated": 14 },
		  "opponent": { "userId": 12, "finalLifePoints": -2, "creaturesDefeated": 11 },
		  "seed": 42, "rulesVersion": "1.0", "extraField": "ignored",
		  "log": [ { "sequence": 1, "round": 0, "type": "MATCH_STARTED", "side": null, "data": {}, "message": "Match started" } ] }
		""";

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	@DisplayName("A successful simulation is read with result, players and the log as it is")
	void simulate_ok_readsResponse() throws IOException {
		// Arrange
		AtomicReference<String> received = new AtomicReference<>();
		EngineClient client = clientFor(200, RESPONSE, received);

		// Act
		SimulationResponse response = client.simulate(request());

		// Assert
		assertThat(response.result()).isEqualTo("CHALLENGER_WIN");
		assertThat(response.totalRounds()).isEqualTo(37);
		assertThat(response.opponent().finalLifePoints()).isEqualTo(-2);
		assertThat(response.log()).hasSize(1);
		assertThat(received.get()).contains("\"seed\":42").contains("\"rulesVersion\":\"1.0\"").contains("\"effectCode\":\"X\"");
	}

	@Test
	@DisplayName("A 400 from the engine becomes ENGINE_INPUT_REJECTED: a retry would send the same data (API-71)")
	void simulate_badRequest_throwsInputRejected() throws IOException {
		EngineClient client = clientFor(400, """
			{ "title": "Invalid simulation input", "status": 400, "detail": "Unknown effect X on card 1",
			  "code": "UNKNOWN_EFFECT", "instance": "/api/v1/simulations" }
			""", new AtomicReference<>());

		assertThatThrownBy(() -> client.simulate(request()))
			.isInstanceOf(EngineInputRejectedException.class)
			.extracting("code").isEqualTo("ENGINE_INPUT_REJECTED");
	}

	@Test
	@DisplayName("A 400 whose body is not a ProblemDetail is still ENGINE_INPUT_REJECTED")
	void simulate_badRequestUnreadableBody_throwsInputRejected() throws IOException {
		EngineClient client = clientFor(400, "not json", new AtomicReference<>());

		assertThatThrownBy(() -> client.simulate(request())).isInstanceOf(EngineInputRejectedException.class);
	}

	@Test
	@DisplayName("A 500 from the engine stays ENGINE_UNAVAILABLE")
	void simulate_serverError_throwsUnavailable() throws IOException {
		EngineClient client = clientFor(500, "{\"code\": \"INTERNAL_ERROR\"}", new AtomicReference<>());

		assertThatThrownBy(() -> client.simulate(request())).isInstanceOf(EngineUnavailableException.class);
	}

	@Test
	@DisplayName("An engine that is not running becomes ENGINE_UNAVAILABLE")
	void simulate_engineDown_throwsUnavailable() throws IOException {
		// Arrange: una porta libera su cui nessuno ascolta
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		EngineClient client = new EngineClient(new EngineProperties("http://localhost:" + port, Duration.ofSeconds(2)));

		// Act + Assert
		assertThatThrownBy(() -> client.simulate(request())).isInstanceOf(EngineUnavailableException.class);
	}

	private EngineClient clientFor(int status, String body, AtomicReference<String> received) throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/api/v1/simulations", exchange -> {
			received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
		return new EngineClient(new EngineProperties("http://localhost:" + server.getAddress().getPort(), Duration.ofSeconds(5)));
	}

	private static SimulationRequest request() {
		SimulationRequest.Card card = new SimulationRequest.Card(1, "Crawler", "SPELL", "Rare", 3, null, null, null, null,
			"BURN", "X");
		return new SimulationRequest(42, new SimulationRequest.Rules("1.0", 30, 75),
			List.of(new SimulationRequest.Faction(1, "Theory of the Wound")), List.of(),
			new SimulationRequest.Player(7, List.of(card)), new SimulationRequest.Player(12, List.of(card)));
	}
}
