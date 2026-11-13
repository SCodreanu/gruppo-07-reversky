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
 * Fazione del gioco (tabella {@code factions}). Le fazioni non si cancellano: le carte le usano.
 */
@Entity
@Table(name = "factions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "emblem_path", nullable = false)
	private String emblemPath;

	private String description;

	public Faction(String name, String emblemPath, String description) {
		this.name = name;
		this.emblemPath = emblemPath;
		this.description = description;
	}

	/**
	 * Modifica dal backoffice (PUT /admin/factions/{id}): si salva da sola alla fine della transazione.
	 */
	public void update(String name, String emblemPath, String description) {
		this.name = name;
		this.emblemPath = emblemPath;
		this.description = description;
	}
}
