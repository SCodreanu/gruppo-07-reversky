package it.downfall.reversky.engine.web;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.web.dto.EffectResponse;
import lombok.RequiredArgsConstructor;

/**
 * Elenco degli effetti implementati (docs/api-engine, {@code GET /api/v1/effects}): legge il registro degli effetti,
 * lo stesso che la simulazione usa per il controllo {@code UNKNOWN_EFFECT}.
 */
@Tag(name = "Effects", description = "Effects implemented by the engine")
@RestController
@RequestMapping("/api/v1/effects")
@RequiredArgsConstructor
public class EffectController {

	/**
	 * Ordine stabile per il backoffice, che non dipende da come è scritto il registro: per tipo di carta (l'ordine
	 * dell'enum) e poi per codice.
	 */
	private static final Comparator<EffectResponse> ORDER = Comparator.comparing(EffectResponse::appliesTo)
			.thenComparing(EffectResponse::code);

	private final EffectRegistry effects;

	/**
	 * Elenca gli effetti implementati.
	 *
	 * @return codice e tipo di carta di ogni effetto, per tipo di carta e poi per codice
	 */
	@Operation(summary = "List the implemented effects", description = "Every effect code the engine knows, with the "
			+ "card type that can carry it. A card whose effectCode is not in this list, or is listed for another "
			+ "card type, is rejected by POST /api/v1/simulations with UNKNOWN_EFFECT. Sorted by card type, then code.")
	@ApiResponse(responseCode = "200", description = "Implemented effects", content = @Content(
			mediaType = MediaType.APPLICATION_JSON_VALUE,
			array = @ArraySchema(schema = @Schema(implementation = EffectResponse.class)),
			examples = @ExampleObject(value = """
					[{"code": "BITE_OF_REVERSE", "appliesTo": "SPELL"},
					 {"code": "LAST_SANCTUARY", "appliesTo": "HIDDEN"}]""")))
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<EffectResponse> list() {
		return effects.codes().entrySet().stream()
				.map(entry -> new EffectResponse(entry.getKey(), entry.getValue()))
				.sorted(ORDER)
				.toList();
	}
}
