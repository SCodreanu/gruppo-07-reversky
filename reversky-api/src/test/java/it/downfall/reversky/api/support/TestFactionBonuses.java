package it.downfall.reversky.api.support;

import java.util.List;
import java.util.stream.Collectors;

import it.downfall.reversky.api.catalog.dto.FactionRequest;
import it.downfall.reversky.api.catalog.dto.FactionResponse;
import it.downfall.reversky.api.catalog.service.FactionService;
import it.downfall.reversky.api.factionbonus.dto.FactionBonusRequest;
import it.downfall.reversky.api.factionbonus.service.FactionBonusService;

/**
 * Helper per i test dei bonus di fazione (docs/backoffice, "Dati di riferimento").
 * <p>
 * Come {@link TestRuleSets} non è un {@code @TestComponent}: si crea nel test con
 * {@code new TestFactionBonuses(factionBonusService, factionService)}, così {@code @Import} resta
 * uguale agli altri IT e Spring riusa lo stesso contesto.
 * <p>
 * La matrice dei bonus è una sola per tutto il database e GET /rules la legge: un test che la
 * modifica chiama {@link #rememberMatrix()} in {@code @BeforeEach} e {@link #restoreMatrix()} in
 * {@code @AfterEach}, così la matrice di prima (il seed, quando ci sarà) torna com'era.
 */
public class TestFactionBonuses {

	private final FactionBonusService factionBonusService;
	private final FactionService factionService;

	private List<FactionBonusRequest> rememberedMatrix;

	public TestFactionBonuses(FactionBonusService factionBonusService, FactionService factionService) {
		this.factionBonusService = factionBonusService;
		this.factionService = factionService;
	}

	/** Ricorda la matrice attuale, da ripristinare con {@link #restoreMatrix()}. */
	public void rememberMatrix() {
		rememberedMatrix = currentMatrix();
	}

	/** Rimette la matrice ricordata da {@link #rememberMatrix()}. */
	public void restoreMatrix() {
		factionBonusService.replaceAll(rememberedMatrix);
	}

	/** La matrice attuale nella forma del corpo di PUT /admin/faction-bonuses, ordinata per origine e bersaglio. */
	public List<FactionBonusRequest> currentMatrix() {
		return factionBonusService.findAllSummaries()
			.stream()
			.map(b -> new FactionBonusRequest(b.originFactionId(), b.targetFactionId(), b.atkBonus(), b.defBonus()))
			.toList();
	}

	/**
	 * Crea una fazione con nome univoco. Le fazioni non si cancellano, quindi i test non contano
	 * sulle fazioni esistenti e usano solo le proprie.
	 */
	public FactionResponse createFaction() {
		return factionService.create(new FactionRequest("Faction " + TestUsers.uniqueSuffix(), "factions/test.svg", null));
	}

	/** Il JSON che manderebbe il backoffice per queste righe. */
	public static String toJson(List<FactionBonusRequest> rows) {
		return rows.stream()
			.map(r -> """
				{"originFactionId": %s, "targetFactionId": %s, "atkBonus": %s, "defBonus": %s}"""
				.formatted(r.originFactionId(), r.targetFactionId(), r.atkBonus(), r.defBonus()))
			.collect(Collectors.joining(", ", "[", "]"));
	}
}
