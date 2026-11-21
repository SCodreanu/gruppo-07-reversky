package it.downfall.reversky.api.collection.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Completamento della collezione (GET /collection/summary, docs/collezione): carte originali attive possedute sul
 * totale del catalogo, in tutto e per rarità. Le varianti non contano nel totale e possederne una vale come
 * possedere la sua originale (Q-08); le carte disattivate non contano (deciso il 20 novembre).
 */
public record CollectionSummaryResponse(

	@Schema(example = "57", description = "Active original cards I own, in every rarity")
	int owned,

	@Schema(example = "120", description = "Active original cards in the catalog")
	int total,

	@Schema(description = "One entry per rarity that has active original cards, by rarity order")
	List<RarityProgressResponse> rarities
) {
}
