package it.downfall.reversky.api.catalog.entity;

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
 * (vedi {@link #copyRulesFrom(CatalogCard)}); cambia solo nome, immagine e testo di sapore.
 * <p>
 * Rarità e fazioni arrivano con #80 e #81.
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

	// Niente unique = true: il nome è univoco solo tra le originali (indice parziale in V5)
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
	 * Crea una carta. Oggi la usano solo i test (le carte arrivano da migrazione),
	 * da API-14 la userà POST /admin/cards. Una carta nuova è sempre attiva.
	 */
	public CatalogCard(CatalogCard variantOf, String name, CardType type, CardSubtype subtype, String effectCode, String imagePath, String effectDescription, String flavorText, Integer atk, Integer def, Integer copyLimit) {
		this.variantOf = variantOf;
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
	 * Campi di gioco: tipo, sottotipo, effetto (codice e testo), statistiche e limite di copie.
	 * Su una variante non si chiama direttamente: si usa {@link #copyRulesFrom(CatalogCard)}.
	 */
	public void updateRules(CardType type, CardSubtype subtype, String effectCode, String effectDescription, Integer atk, Integer def, Integer copyLimit) {
		this.cardType = type;
		this.cardSubtype = subtype;
		this.effectCode = effectCode;
		this.effectDescription = effectDescription;
		this.atk = atk;
		this.def = def;
		this.copyLimit = copyLimit;
	}

	/**
	 * Copia i campi di gioco dall'originale (docs/backoffice, regola 8).
	 * <p>
	 * Usa i getter e non i campi: original può essere un proxy LAZY di Hibernate,
	 * i cui campi sono vuoti finché non si chiama un suo metodo.
	 */
	public void copyRulesFrom(CatalogCard original) {
		updateRules(original.getCardType(), original.getCardSubtype(), original.getEffectCode(), original.getEffectDescription(), original.getAtk(), original.getDef(), original.getCopyLimit());
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

}
