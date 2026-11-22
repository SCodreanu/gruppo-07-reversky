package it.downfall.reversky.api.catalog.repository;

import org.springframework.data.jpa.domain.Specification;

import it.downfall.reversky.api.catalog.entity.CardType;
import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Faction;
import it.downfall.reversky.api.catalog.entity.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtri facoltativi per la ricerca delle carte (GET /cards e GET /admin/cards).
 * <p>
 * Ogni metodo restituisce un predicato null quando il filtro è vuoto: Spring Data lo salta
 * quando li combina con {@code Specification.allOf(...)}. #82 aggiunge qui il tag.
 */
public final class CatalogCardSpecifications {

	private CatalogCardSpecifications() {
		// Solo metodi statici
	}

	/**
	 * Solo carte di quel tipo; tipo null = nessun filtro.
	 */
	public static Specification<CatalogCard> hasType(CardType type) {
		return (root, query, builder) -> {
			if (type == null) {
				return null;
			}

			return builder.equal(root.get("cardType"), type);
		};
	}

	/**
	 * Nome che contiene il testo, senza distinguere maiuscole e minuscole; testo vuoto = nessun filtro.
	 */
	public static Specification<CatalogCard> nameContains(String name) {
		return (root, query, builder) -> {
			if (name == null || name.isBlank()) {
				return null;
			}

			String pattern = "%" + name.trim().toLowerCase() + "%";
			return builder.like(builder.lower(root.get("name")), pattern);
		};
	}

	/**
	 * Solo carte della rarità indicata; id null = nessun filtro.
	 * <p>
	 * Un id che non esiste non è un errore: la pagina è vuota, come per gli altri filtri.
	 * {@code rarity.id} si legge dalla colonna rarity_id di catalog_cards, quindi non serve una join.
	 */
	public static Specification<CatalogCard> hasRarity(Integer rarityId) {
		return (root, query, builder) -> {
			if (rarityId == null) {
				return null;
			}

			return builder.equal(root.get("rarity").get("id"), rarityId);
		};
	}

	/**
	 * Solo carte che hanno la fazione come prima o come seconda; id null = nessun filtro.
	 * <p>
	 * Spell e Hidden non hanno fazioni, quindi non compaiono mai. Un id che non esiste dà una pagina vuota, come
	 * per la rarità. Come {@code rarity.id}, {@code faction.id} e {@code secondaryFaction.id} si leggono dalle
	 * colonne di catalog_cards senza join, quindi una carta senza seconda fazione resta nel risultato.
	 */
	public static Specification<CatalogCard> hasFaction(Integer factionId) {
		return (root, query, builder) -> {
			if (factionId == null) {
				return null;
			}

			return builder.or(
				builder.equal(root.get("faction").get("id"), factionId),
				builder.equal(root.get("secondaryFaction").get("id"), factionId));
		};
	}

	/**
	 * Solo carte attive o solo disattivate; null = tutte.
	 */
	public static Specification<CatalogCard> isActive(Boolean active) {
		return (root, query, builder) -> {
			if (active == null) {
				return null;
			}

			return builder.equal(root.get("isActive"), active);
		};
	}

	/**
	 * Solo carte con quel tag (API-12), tra i tag propri o quelli di una delle sue fazioni, anche la seconda; nome vuoto
	 * = nessun filtro. Il nome si confronta così com'è (i tag sono maiuscoli, es. {@code DREAM}): un tag che non esiste
	 * dà una pagina vuota, come gli altri filtri. Spell e Hidden non hanno fazione, quindi passano solo con i tag propri.
	 */
	public static Specification<CatalogCard> hasTag(String tag) {
		return (root, query, builder) -> {
			if (tag == null || tag.isBlank()) {
				return null;
			}

			return isTagged(root, tag.trim(), query, builder);
		};
	}

	/**
	 * La carta ha il tag tra i propri ({@code card_tags}) o tra quelli di una delle sue fazioni ({@code faction_tags})?
	 * Due sottoquery {@code EXISTS}, così la pagina non ha righe doppie. La usano anche i filtri della collezione.
	 *
	 * @param card la carta della query principale (la radice, o la carta di una riga della collezione)
	 */
	public static Predicate isTagged(Path<CatalogCard> card, String tag, CriteriaQuery<?> query,
			CriteriaBuilder builder) {
		Subquery<Integer> own = query.subquery(Integer.class);
		Root<CatalogCard> tagged = own.from(CatalogCard.class);
		Join<CatalogCard, Tag> ownTag = tagged.join("tags");
		own.select(tagged.get("id"))
			.where(builder.equal(tagged, card), builder.equal(ownTag.get("name"), tag));

		Subquery<Integer> ofFaction = query.subquery(Integer.class);
		Root<Faction> faction = ofFaction.from(Faction.class);
		Join<Faction, Tag> factionTag = faction.join("tags");
		ofFaction.select(faction.get("id"))
			.where(builder.or(builder.equal(faction, card.get("faction")), builder.equal(faction, card.get("secondaryFaction"))),
				builder.equal(factionTag.get("name"), tag));

		return builder.or(builder.exists(own), builder.exists(ofFaction));
	}
}
