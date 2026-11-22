package it.downfall.reversky.api.ruleset.entity;

import java.time.Instant;

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
 * Regole di gioco versionate (tabella {@code rule_sets}, docs/backoffice "Regole di gioco").
 * <p>
 * Una sola riga è attiva, garantito dall'indice parziale {@code uq_rule_sets_one_active}.
 * Un rule set già usato da una partita non si modifica: si crea una nuova versione e la si attiva.
 */
@Entity
@Table(name = "rule_sets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleSet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = 20)
	private String version;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	@Column(name = "starting_life_points", nullable = false)
	private Integer startingLifePoints;

	@Column(name = "max_rounds", nullable = false)
	private Integer maxRounds;

	@Column(name = "deck_size", nullable = false)
	private Integer deckSize;

	@Column(name = "deck_creatures", nullable = false)
	private Integer deckCreatures;

	@Column(name = "deck_spells", nullable = false)
	private Integer deckSpells;

	@Column(name = "deck_hidden", nullable = false)
	private Integer deckHidden;

	@Column(name = "deck_items", nullable = false)
	private Integer deckItems;

	@Column(name = "deck_terrains", nullable = false)
	private Integer deckTerrains;

	@Column(name = "max_hidden_survival", nullable = false)
	private Integer maxHiddenSurvival;

	@Column(name = "max_hidden_damage", nullable = false)
	private Integer maxHiddenDamage;

	@Column(name = "signup_bonus", nullable = false)
	private Integer signupBonus;

	@Column(name = "win_reward", nullable = false)
	private Integer winReward;

	@Column(name = "draw_reward", nullable = false)
	private Integer drawReward;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/**
	 * Admin che ha creato la versione: resta nullo finché il backoffice non registra l'autore.
	 */
	@Column(name = "created_by_id", updatable = false)
	private Integer createdById;

	/**
	 * Nuova versione, sempre disattivata: si attiva con {@link #activate()}.
	 *
	 * @param createdAt istante di creazione, dal {@code Clock} del service
	 */
	public RuleSet(
			String version,
			Integer startingLifePoints,
			Integer maxRounds,
			Integer deckSize,
			Integer deckCreatures,
			Integer deckSpells,
			Integer deckHidden,
			Integer deckItems,
			Integer deckTerrains,
			Integer maxHiddenSurvival,
			Integer maxHiddenDamage,
			Integer signupBonus,
			Integer winReward,
			Integer drawReward,
			Instant createdAt,
			Integer createdById) {
		update(version, startingLifePoints, maxRounds, deckSize, deckCreatures, deckSpells, deckHidden, deckItems,
			deckTerrains, maxHiddenSurvival, maxHiddenDamage, signupBonus, winReward, drawReward);
		this.isActive = false;
		this.createdAt = createdAt;
		this.createdById = createdById;
	}

	/**
	 * Modifica dal backoffice (PUT /admin/rule-sets/{id}): il service la permette solo su una versione
	 * mai usata da una partita. Si salva da sola alla fine della transazione.
	 */
	public void update(
			String version,
			Integer startingLifePoints,
			Integer maxRounds,
			Integer deckSize,
			Integer deckCreatures,
			Integer deckSpells,
			Integer deckHidden,
			Integer deckItems,
			Integer deckTerrains,
			Integer maxHiddenSurvival,
			Integer maxHiddenDamage,
			Integer signupBonus,
			Integer winReward,
			Integer drawReward) {
		this.version = version;
		this.startingLifePoints = startingLifePoints;
		this.maxRounds = maxRounds;
		this.deckSize = deckSize;
		this.deckCreatures = deckCreatures;
		this.deckSpells = deckSpells;
		this.deckHidden = deckHidden;
		this.deckItems = deckItems;
		this.deckTerrains = deckTerrains;
		this.maxHiddenSurvival = maxHiddenSurvival;
		this.maxHiddenDamage = maxHiddenDamage;
		this.signupBonus = signupBonus;
		this.winReward = winReward;
		this.drawReward = drawReward;
	}

	/**
	 * Va chiamato solo dopo aver disattivato e salvato la versione attiva, altrimenti il flush
	 * viola {@code uq_rule_sets_one_active}.
	 */
	public void activate() {
		this.isActive = true;
	}

	public void deactivate() {
		this.isActive = false;
	}
}
