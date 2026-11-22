package it.downfall.reversky.api.pack.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import it.downfall.reversky.api.catalog.entity.CatalogCard;
import it.downfall.reversky.api.catalog.entity.Rarity;
import it.downfall.reversky.api.pack.entity.Pack;
import it.downfall.reversky.api.pack.entity.PackBonusRule;
import it.downfall.reversky.api.pack.entity.PackRarityRule;
import it.downfall.reversky.api.pack.exception.PackSlotEmptyException;

/**
 * Estrazione delle carte di un pacchetto (docs/pacchetti, "Apertura"), senza database: riceve pacchetto, carte e
 * generatore casuale, così i test la ripetono con un seed fisso.
 * <ol>
 * <li>Gli slot base, dalla rarità più comune.</li>
 * <li>I bonus si tirano in modo indipendente, saltando le rarità senza carte; quelli riusciti, dal più raro,
 * sostituiscono uno slot ciascuno partendo dall'ultimo (il più raro). Quelli oltre il numero di slot si perdono.</li>
 * <li>Per ogni slot una carta a caso tra quelle della sua rarità, con il ripiego di {@link CardPool}.</li>
 * </ol>
 */
public final class PackDrawer {

	private final RandomGenerator random;

	public PackDrawer(RandomGenerator random) {
		this.random = random;
	}

	/**
	 * @return le carte estratte nell'ordine degli slot; sono sempre tante quante la somma degli slot base
	 * @throws PackSlotEmptyException se uno slot non trova nessuna carta, neanche con il ripiego
	 */
	public List<CatalogCard> draw(Pack pack, CardPool pool) {
		List<Rarity> slots = baseSlots(pack);
		applyBonuses(slots, successfulBonuses(pack, pool));

		List<CatalogCard> cards = new ArrayList<>(slots.size());
		for (Rarity slot : slots) {
			List<CatalogCard> candidates = pool.candidatesFor(slot);
			if (candidates.isEmpty()) {
				throw new PackSlotEmptyException(pack.getId(), slot.getName());
			}
			cards.add(candidates.get(random.nextInt(candidates.size())));
		}
		return cards;
	}

	private static List<Rarity> baseSlots(Pack pack) {
		List<Rarity> slots = new ArrayList<>(pack.getCardCount());
		for (PackRarityRule rule : pack.getSlots()) {
			for (int i = 0; i < rule.getQuantity(); i++) {
				slots.add(rule.getRarity());
			}
		}
		return slots;
	}

	// Ogni bonus si tira anche se ne è già riuscito un altro; 0 <= tiro < 10.000, quindi 1.000 unità escono nel 10%
	private List<Rarity> successfulBonuses(Pack pack, CardPool pool) {
		List<Rarity> successes = new ArrayList<>();
		pack.getBonuses().stream()
			.sorted(Comparator.comparing((PackBonusRule bonus) -> bonus.getRarity().getSortingOrder()).reversed())
			.filter(bonus -> pool.hasCards(bonus.getRarity()))
			.forEach(bonus -> {
				if (random.nextInt(PackBonusRule.UNITS) < bonus.getProbabilityUnits()) {
					successes.add(bonus.getRarity());
				}
			});
		return successes;
	}

	private static void applyBonuses(List<Rarity> slots, List<Rarity> rarestFirst) {
		int replaced = Math.min(slots.size(), rarestFirst.size());
		for (int i = 0; i < replaced; i++) {
			slots.set(slots.size() - 1 - i, rarestFirst.get(i));
		}
	}
}
