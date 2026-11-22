package it.downfall.reversky.api.catalog.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Carta del catalogo (tabella {@code catalog_cards}, docs/backoffice "Carte").
 * <p>
 * Le carte non si cancellano mai: si disattivano. Una variante (versione evento) ha
 * {@code variantOf} valorizzato e copia dall'originale tutti i campi di gioco
 * (vedi {@link #copyRulesFrom(CatalogCard)}), rarità e fazioni comprese; cambia solo nome, immagine e testo di sapore.
 * <p>
 * Fazioni: {@code faction} è obbligatoria per Creature, Terreni e Oggetti e vuota per Spell e Hidden;
 * {@code secondaryFaction} è facoltativa (es. Mutant) e diversa dalla prima (docs/backoffice, "Carte").
 * La modifica dal backoffice arriva con #81.
 */
@Entity
@Table(name = "catalog_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogCard {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "variant_of_id", nullable = true)
	private CatalogCard variantOf;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rarity_id", nullable = false)
	private Rarity rarity;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "faction_id", nullable = true)
	private Faction faction;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "secondary_faction_id", nullable = true)
	private Faction secondaryFaction;

	// Niente unique = true: il nome è univoco solo tra le originali (indice parziale uq_catalog_cards_base_name in V3)
	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "type", nullable = false)
	private CardType cardType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "subtype")
	private CardSubtype cardSubtype;

	@Column(name = "effect_code")
	private String effectCode;

	@Column(name = "image_path", nullable = false)
	private String imagePath;

	@Column(name = "effect_description")
	private String effectDescription;

	@Column(name = "flavor_text")
	private String flavorText;

	private Integer atk;
	private Integer def;

	@Column(name = "copy_limit")
	private Integer copyLimit;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	/**
	 * Tag propri della carta ({@code card_tags}, API-12): ognuna ha i suoi, anche una variante, e non si copiano
	 * dall'originale. Nei filtri per tag contano insieme a quelli delle fazioni; nei pacchetti una variante esce solo
	 * con i propri (docs/pacchetti). Si caricano quando servono, a gruppi per le pagine del backoffice.
	 */
	@ManyToMany
	@JoinTable(name = "card_tags", joinColumns = @JoinColumn(name = "card_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id"))
	@BatchSize(size = 50)
	@Getter(AccessLevel.NONE)
	private Set<Tag> tags = new HashSet<>();

	/**
	 * Crea una carta. Oggi la usano solo i test (le carte arrivano da migrazione),
	 * da API-14 la userà POST /admin/cards. Una carta nuova è sempre attiva.
	 * <p>
	 * Non controlla che rarità e fazioni siano ammesse per il tipo: lo fa il service.
	 */
	public CatalogCard(CatalogCard variantOf, Rarity rarity, Faction faction, Faction secondaryFaction, String name, CardType type, CardSubtype subtype, String effectCode, String imagePath, String effectDescription, String flavorText, Integer atk, Integer def, Integer copyLimit) {
		this.variantOf = variantOf;
		this.rarity = rarity;
		this.faction = faction;
		this.secondaryFaction = secondaryFaction;
		this.name = name;
		this.cardType = type;
		this.cardSubtype = subtype;
		this.effectCode = effectCode;
		this.imagePath = imagePath;
		this.effectDescription = effectDescription;
		this.flavorText = flavorText;
		this.atk = atk;
		this.def = def;
		this.copyLimit = copyLimit;
		this.isActive = true;
	}

	/**
	 * È una versione evento di un'altra carta?
	 */
	public boolean isVariant() {
		return variantOf != null;
	}

	/**
	 * Campi propri della carta, che una variante non copia dall'originale.
	 */
	public void updateDetails(String name, String flavorText) {
		this.name = name;
		this.flavorText = flavorText;
	}

	/**
	 * Nuova illustrazione (POST /admin/cards/{id}/image, API-14): è propria della carta, anche per una variante.
	 *
	 * @param imagePath percorso relativo restituito da {@code ImageStorage}
	 */
	public void changeImage(String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * Campi di gioco: rarità, tipo, sottotipo, effetto (codice e testo), statistiche e limite di copie.
	 * Le fazioni cambiano a parte, con {@link #changeFactions(Faction, Faction)}.
	 * Su una variante non si chiama direttamente: si usa {@link #copyRulesFrom(CatalogCard)}.
	 * <p>
	 * I controlli (rarità e fazioni ammesse per il tipo) li fa il service.
	 */
	public void updateRules(Rarity rarity, CardType type, CardSubtype subtype, String effectCode, String effectDescription, Integer atk, Integer def, Integer copyLimit) {
		this.rarity = rarity;
		this.cardType = type;
		this.cardSubtype = subtype;
		this.effectCode = effectCode;
		this.effectDescription = effectDescription;
		this.atk = atk;
		this.def = def;
		this.copyLimit = copyLimit;
	}

	/**
	 * Cambia le fazioni della carta. I controlli (fazione presente solo se il tipo la prevede,
	 * seconda fazione diversa dalla prima) li fa il service.
	 */
	public void changeFactions(Faction faction, Faction secondaryFaction) {
		this.faction = faction;
		this.secondaryFaction = secondaryFaction;
	}

	/**
	 * Copia i campi di gioco dall'originale, fazioni comprese (docs/backoffice, regola 8).
	 * <p>
	 * Usa i getter e non i campi: original può essere un proxy LAZY di Hibernate,
	 * i cui campi sono vuoti finché non si chiama un suo metodo. La rarità copiata può essere a sua
	 * volta un proxy: va bene, la variante salva solo il suo id.
	 */
	public void copyRulesFrom(CatalogCard original) {
		updateRules(original.getRarity(), original.getCardType(), original.getCardSubtype(), original.getEffectCode(), original.getEffectDescription(), original.getAtk(), original.getDef(), original.getCopyLimit());
		changeFactions(original.getFaction(), original.getSecondaryFaction());
	}

	/**
	 * Rende la carta variante di {@code original}, oppure di nuovo originale se null.
	 * I controlli (original esiste, non è una variante, questa carta non ha varianti) li fa il service.
	 */
	public void changeVariantOf(CatalogCard original) {
		this.variantOf = original;
	}

	/**
	 * Rimette la carta nei pacchetti.
	 */
	public void activate() {
		this.isActive = true;
	}

	/**
	 * Toglie la carta dai pacchetti; resta nelle collezioni di chi la possiede.
	 */
	public void deactivate() {
		this.isActive = false;
	}

	/** I tag propri della carta, in sola lettura: si cambiano con {@link #replaceTags}. */
	public Set<Tag> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	/**
	 * Sostituisce tutti i tag propri della carta (PUT /admin/cards/{id}/tags); una lista vuota li toglie tutti. Non
	 * tocca le varianti: i tag sono ciò che distingue una versione evento (deciso il 22 novembre).
	 */
	public void replaceTags(Collection<Tag> newTags) {
		tags.clear();
		tags.addAll(newTags);
	}
}
