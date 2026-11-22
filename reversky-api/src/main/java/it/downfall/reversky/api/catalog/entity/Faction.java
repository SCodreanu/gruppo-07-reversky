package it.downfall.reversky.api.catalog.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

	/**
	 * Tag della fazione ({@code faction_tags}, API-13): valgono per tutte le carte della fazione nei filtri per tag
	 * (API-12, API-21), ma non per Spell e Hidden, che non hanno fazione. Si caricano solo quando servono.
	 */
	@ManyToMany
	@JoinTable(name = "faction_tags", joinColumns = @JoinColumn(name = "faction_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id"))
	@Getter(AccessLevel.NONE)
	private Set<Tag> tags = new HashSet<>();

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

	/** I tag della fazione, in sola lettura: si cambiano con {@link #replaceTags}. */
	public Set<Tag> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	/**
	 * Sostituisce tutti i tag della fazione (PUT /admin/factions/{id}/tags); una lista vuota li toglie tutti. Le righe di
	 * {@code faction_tags} si aggiornano alla fine della transazione.
	 */
	public void replaceTags(Collection<Tag> newTags) {
		tags.clear();
		tags.addAll(newTags);
	}
}
