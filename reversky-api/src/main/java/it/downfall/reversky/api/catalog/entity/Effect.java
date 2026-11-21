package it.downfall.reversky.api.catalog.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un effetto implementato dall'engine (tabella {@code effects}, V13, API-36): l'elenco dei codici che il backoffice
 * può assegnare a {@code catalog_cards.effect_code}.
 * <p>
 * Solo lettura: ogni effetto è codice Java nell'engine, quindi le righe arrivano da migrazione insieme al loro codice,
 * mai dal backoffice. Ogni codice vale per un solo tipo di carta; la chiave esterna composta
 * {@code fk_catalog_cards_effect} lo garantisce anche nel database.
 */
@Entity
@Table(name = "effects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Effect {

	@Id
	@Column(name = "code", nullable = false, length = 100)
	private String code;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "applies_to", nullable = false)
	private CardType appliesTo;
}
