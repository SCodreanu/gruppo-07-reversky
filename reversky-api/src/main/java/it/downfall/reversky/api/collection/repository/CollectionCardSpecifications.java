package it.downfall.reversky.api.collection.repository;

import org.springframework.data.jpa.domain.Specification;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.repository.CatalogCardSpecifications;
import it.downfall.reversky.api.collection.entity.CollectionCard;

/**
 * Filtri della collezione (GET /collection e GET /users/{id}/collection), gli stessi di GET /cards applicati alla
 * carta della riga.
 * <p>
 * Ogni metodo restituisce un predicato null quando il filtro è vuoto: Spring Data lo salta con
 * {@code Specification.allOf(...)}. Il filtro {@code tag} arriva con API-12.
 */
public final class CollectionCardSpecifications {

	private CollectionCardSpecifications() {
		// Solo metodi statici
	}

	/** Solo le righe dell'utente; sempre presente. */
	public static Specification<CollectionCard> ofUser(Integer userId) {
		return (root, query, builder) -> builder.equal(root.get("id").get("userId"), userId);
	}

	/** Solo carte di quel tipo; null = nessun filtro. */
	public static Specification<CollectionCard> hasType(CardType type) {
		return (root, query, builder) -> type == null ? null : builder.equal(root.get("card").get("cardType"), type);
	}

	/** Nome che contiene il testo, senza maiuscole; vuoto = nessun filtro. */
	public static Specification<CollectionCard> nameContains(String name) {
		return (root, query, builder) -> {
			if (name == null || name.isBlank()) {
				return null;
			}

			String pattern = "%" + name.trim().toLowerCase() + "%";
			return builder.like(builder.lower(root.get("card").get("name")), pattern);
		};
	}

	/** Solo carte della rarità; null = nessun filtro, un id inesistente dà una pagina vuota. */
	public static Specification<CollectionCard> hasRarity(Integer rarityId) {
		return (root, query, builder) ->
			rarityId == null ? null : builder.equal(root.get("card").get("rarity").get("id"), rarityId);
	}

	/**
	 * Solo carte che hanno la fazione come prima o come seconda; null = nessun filtro. Spell e Hidden non hanno
	 * fazioni e non compaiono mai.
	 */
	public static Specification<CollectionCard> hasFaction(Integer factionId) {
		return (root, query, builder) -> {
			if (factionId == null) {
				return null;
			}

			return builder.or(
				builder.equal(root.get("card").get("faction").get("id"), factionId),
				builder.equal(root.get("card").get("secondaryFaction").get("id"), factionId));
		};
	}

	/**
	 * Solo carte con quel tag, propri o di una delle loro fazioni (API-12); nome vuoto = nessun filtro. Stessa regola di
	 * {@link CatalogCardSpecifications#hasTag}.
	 */
	public static Specification<CollectionCard> hasTag(String tag) {
		return (root, query, builder) -> {
			if (tag == null || tag.isBlank()) {
				return null;
			}

			return CatalogCardSpecifications.isTagged(root.get("card"), tag.trim(), query, builder);
		};
	}
}
