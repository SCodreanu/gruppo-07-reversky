package it.downfall.reversky.api.pack.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.catalog.entity.Tag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un tipo di pacchetto del negozio con slot base e bonus (tabelle {@code packs}, {@code pack_rarity_rules},
 * {@code pack_bonus_rules}, docs/pacchetti). Nasce disattivato: si attiva solo se i suoi slot possono essere riempiti.
 */
@Entity
@Table(name = "packs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pack {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "cost", nullable = false)
	private Integer cost;

	/** Massimo di pacchetti che un utente può comprare in totale; null = nessun limite. */
	@Column(name = "purchase_limit")
	private Integer purchaseLimit;

	@Column(name = "image_path", nullable = false)
	private String imagePath;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	// SUBSELECT: per una lista di pacchetti carica slot e bonus di tutti con una query ciascuno, senza N+1
	@Getter(AccessLevel.NONE)
	@Fetch(FetchMode.SUBSELECT)
	@OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PackRarityRule> slots = new ArrayList<>();

	@Getter(AccessLevel.NONE)
	@Fetch(FetchMode.SUBSELECT)
	@OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PackBonusRule> bonuses = new ArrayList<>();

	/**
	 * Tag del pacchetto ({@code pack_tags}, API-21): senza tag esce qualsiasi carta attiva originale, con i tag solo le
	 * carte che ne hanno almeno uno (docs/pacchetti, "Filtro delle carte"). Come slot e bonus, per una lista di
	 * pacchetti si caricano con una query sola.
	 */
	@Getter(AccessLevel.NONE)
	@Fetch(FetchMode.SUBSELECT)
	@ManyToMany
	@JoinTable(name = "pack_tags", joinColumns = @JoinColumn(name = "pack_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();

	/**
	 * Pacchetto nuovo, disattivato.
	 */
	public Pack(String name, int cost, Integer purchaseLimit, String imagePath) {
		update(name, cost, purchaseLimit, imagePath);
		this.active = false;
	}

	public void update(String name, int cost, Integer purchaseLimit, String imagePath) {
		this.name = name;
		this.cost = cost;
		this.purchaseLimit = purchaseLimit;
		this.imagePath = imagePath;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

	/** Slot base dalla rarità più comune, in sola lettura. */
	public List<PackRarityRule> getSlots() {
		return sortedByRarity(slots, PackRarityRule::getRarity);
	}

	/** Bonus dalla rarità più comune, in sola lettura. */
	public List<PackBonusRule> getBonuses() {
		return sortedByRarity(bonuses, PackBonusRule::getRarity);
	}

	/** I tag del pacchetto, in sola lettura: si cambiano con {@link #replaceTags}. */
	public Set<Tag> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	/** I nomi dei tag del pacchetto, per il filtro delle carte ({@code CardPool}); vuoto se non ne ha. */
	public Set<String> getTagNames() {
		return tags.stream().map(Tag::getName).collect(Collectors.toUnmodifiableSet());
	}

	/** Sostituisce tutti i tag del pacchetto; una lista vuota li toglie. Le righe si aggiornano alla fine della transazione. */
	public void replaceTags(Collection<Tag> newTags) {
		tags.clear();
		tags.addAll(newTags);
	}

	/** Numero di carte del pacchetto: la somma degli slot base, anche quando escono bonus. */
	public int getCardCount() {
		return slots.stream().mapToInt(PackRarityRule::getQuantity).sum();
	}

	/**
	 * Sostituisce gli slot aggiornando le righe esistenti, come i mazzi: svuotare e reinserire violerebbe la chiave
	 * primaria, perché Hibernate esegue gli insert prima dei delete.
	 */
	public void replaceSlots(Map<Rarity, Integer> quantities) {
		replace(slots, quantities, PackRarityRule::getRarityId, PackRarityRule::changeQuantity,
			(rarity, quantity) -> new PackRarityRule(this, rarity, quantity));
	}

	/** Sostituisce i bonus aggiornando le righe esistenti. */
	public void replaceBonuses(Map<Rarity, Integer> probabilities) {
		replace(bonuses, probabilities, PackBonusRule::getRarityId, PackBonusRule::changeProbability,
			(rarity, units) -> new PackBonusRule(this, rarity, units));
	}

	private static <T> void replace(List<T> rows, Map<Rarity, Integer> values, Function<T, Integer> rarityIdOf,
			ObjIntConsumer<T> change, BiFunction<Rarity, Integer, T> create) {
		Map<Integer, Rarity> remaining = new LinkedHashMap<>();
		Map<Integer, Integer> valueById = new LinkedHashMap<>();
		values.forEach((rarity, value) -> {
			remaining.put(rarity.getId(), rarity);
			valueById.put(rarity.getId(), value);
		});

		Iterator<T> iterator = rows.iterator();
		while (iterator.hasNext()) {
			T row = iterator.next();
			Integer value = valueById.get(rarityIdOf.apply(row));
			if (value == null) {
				iterator.remove();
			} else {
				change.accept(row, value);
				remaining.remove(rarityIdOf.apply(row));
			}
		}

		remaining.values().forEach(rarity -> rows.add(create.apply(rarity, valueById.get(rarity.getId()))));
	}

	private static <T> List<T> sortedByRarity(List<T> rows, Function<T, Rarity> rarityOf) {
		List<T> sorted = new ArrayList<>(rows);
		sorted.sort(Comparator.comparing(row -> rarityOf.apply(row).getSortingOrder()));
		return Collections.unmodifiableList(sorted);
	}
}
