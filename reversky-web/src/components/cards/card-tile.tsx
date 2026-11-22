import type { ReactNode } from "react";
import type { CatalogCard } from "@/lib/api/contracts";
import { CardArt, type CardStats } from "./card-art";
import { factionLine, rarityTone, subtypeLabel, typeLabel } from "./card-meta";
import styles from "./cards.module.css";

/**
 * The inside of a card: the text face, covered by the illustration when the card has one.
 * `badge` (e.g. "×3") stays on top of both.
 */
export function CardFace({
  card,
  badge,
  sizes,
  priority,
  compact = false,
  stats,
}: {
  card: CatalogCard;
  badge?: ReactNode;
  sizes?: string;
  priority?: boolean;
  /** Small thumbnails: rarity, name and type only. */
  compact?: boolean;
  /** Current ATK/DEF (e.g. during a battle); the orbs default to the card's base values. */
  stats?: CardStats;
}) {
  const subtype = subtypeLabel(card.subtype);
  return (
    <>
      <span className={styles.cardTop}>
        <span className={styles.rarity}>{card.rarity.name}</span>
      </span>
      <span className={styles.cardMark} aria-hidden="true">✦</span>
      <strong className={`display-type ${styles.cardName}`}>{card.name}</strong>
      <span className={styles.cardType}>{typeLabel(card.type)}{subtype ? ` · ${subtype}` : ""}</span>
      {!compact && <span className={styles.cardFaction}>{factionLine(card)}</span>}
      {compact ? null : card.atk !== null && card.def !== null ? (
        <span className={styles.stats} aria-label={`ATK ${card.atk}, DEF ${card.def}`}>
          <span><small>ATK</small>{card.atk}</span>
          <span><small>DEF</small>{card.def}</span>
        </span>
      ) : (
        <span className={styles.effectPreview}>{card.effectDescription ?? ""}</span>
      )}
      {!card.active && <span className={styles.inactive}>No longer in packs</span>}
      <CardArt card={card} sizes={sizes} priority={priority} stats={stats} />
      {badge !== undefined && <span className={styles.badge}>{badge}</span>}
    </>
  );
}

export function cardFrameClass(card: CatalogCard, extra = ""): string {
  return `${styles.card} ${styles[`tone-${rarityTone(card.rarity.name)}`]} ${extra}`;
}

/** One card in a grid. `footer` goes under the card (e.g. deck controls). */
export function CardTile({
  card,
  badge,
  footer,
  onSelect,
  dimmed = false,
}: {
  card: CatalogCard;
  badge?: ReactNode;
  footer?: ReactNode;
  onSelect?: (card: CatalogCard) => void;
  dimmed?: boolean;
}) {
  const className = cardFrameClass(card, dimmed ? styles.dimmed : "");
  const face = <CardFace card={card} badge={badge} sizes="(max-width: 640px) 50vw, 200px" />;
  return (
    <article className={styles.cardCell}>
      {onSelect ? (
        <button className={className} type="button" onClick={() => onSelect(card)} aria-label={`${card.name}, ${card.rarity.name} ${typeLabel(card.type)}. Show details`}>
          {face}
        </button>
      ) : (
        <div className={className}>{face}</div>
      )}
      {footer && <div className={styles.cardFooter}>{footer}</div>}
    </article>
  );
}
