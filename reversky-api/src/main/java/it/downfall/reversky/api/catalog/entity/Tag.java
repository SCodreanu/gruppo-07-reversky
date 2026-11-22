package it.downfall.reversky.api.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Etichetta libera usata dai filtri dei pacchetti (tabella {@code tags}, docs/backoffice e docs/pacchetti), es.
 * {@code DREAM} o {@code CHRISTMAS_2026}. Si collega a carte, fazioni e pacchetti con {@code card_tags},
 * {@code faction_tags} e {@code pack_tags} (API-12, API-13, API-21). Effetto e sottotipo non sono tag: sono colonne di
 * {@code catalog_cards}.
 * <p>
 * Il nome è univoco e rispetta {@code ^[A-Z0-9_]+$} ({@code uq_tags_name}, {@code chk_tags_name_format}).
 */
@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

	/** Formato del nome, lo stesso di {@code chk_tags_name_format}: maiuscole, cifre e trattino basso. */
	public static final String NAME_PATTERN = "^[A-Z0-9_]+$";

	/** Lunghezza massima del nome ({@code VARCHAR(100)}). */
	public static final int NAME_MAX_LENGTH = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = NAME_MAX_LENGTH)
	private String name;

	public Tag(String name) {
		this.name = name;
	}

	/**
	 * Rinomina dal backoffice (PUT /admin/tags/{id}): carte, fazioni e pacchetti collegati restano collegati, perché
	 * le tabelle di collegamento usano l'id.
	 */
	public void rename(String name) {
		this.name = name;
	}
}
