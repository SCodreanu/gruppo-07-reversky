"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { rarityTone } from "@/components/cards/card-meta";
import { CardFace, cardFrameClass } from "@/components/cards/card-tile";
import type { OpenPackResponse } from "@/lib/api/contracts";
import cardStyles from "@/components/cards/cards.module.css";
import styles from "./pack-reveal.module.css";

/** How long the seal breaks before the first card. */
export const SEAL_ANIMATION_MS = 1500;

export type PackOpening = {
  /** Changes for every opening, so the reveal restarts from the first card. */
  id: number;
  packName: string;
  phase: "sealed" | "revealing" | "error";
  cards: OpenPackResponse["cards"];
  /** Packs of the same type still sealed after this one. */
  remaining: number;
  error?: string;
};

const HIGH_RARITIES = new Set(["rare", "mythic", "legendary", "mutant"]);

/**
 * Opens a pack one card at a time: the seal breaks, each card flips in on "Next card",
 * then the whole pack is shown with the choice to open another pack or go to the shop.
 */
export function PackRevealDialog({
  opening,
  onClose,
  onOpenAnother,
}: {
  opening: PackOpening;
  onClose: () => void;
  onOpenAnother: () => void;
}) {
  const [step, setStep] = useState({ openingId: opening.id, index: 0 });
  const index = step.openingId === opening.id ? step.index : 0;
  const dialogRef = useRef<HTMLElement>(null);
  const primaryRef = useRef<HTMLButtonElement>(null);
  const total = opening.cards.length;
  const finished = opening.phase === "revealing" && index >= total;
  const current = opening.phase === "revealing" && !finished ? opening.cards[index] : null;

  // Keep the keyboard on the main action as the reveal moves on.
  useEffect(() => {
    primaryRef.current?.focus();
  }, [opening.phase, index]);

  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = dialogRef.current?.querySelectorAll<HTMLElement>("button:not([disabled]), a[href]");
      if (!focusable?.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      opener?.focus();
    };
  }, [onClose]);

  const goTo = (next: number) => setStep({ openingId: opening.id, index: next });
  const newCount = opening.cards.filter((row) => row.isNew).length;

  return (
    <div className={styles.backdrop}>
      <section aria-labelledby="reveal-title" aria-modal="true" className={styles.dialog} ref={dialogRef} role="dialog">
        <button aria-label="Close" className={styles.close} onClick={onClose} type="button">×</button>
        <p className="eyebrow">{opening.packName}</p>

        {opening.phase === "sealed" && (
          <>
            <h2 className="display-type" id="reveal-title">The seal is breaking.</h2>
            <div className={styles.sealStage} aria-live="polite">
              <span aria-hidden="true" className={styles.spark}>✦</span>
              <div className={styles.sealedPack}>
                <Image src="/images/reversky-card-back.png" alt="" fill sizes="150px" priority />
              </div>
              <p>Revealing the cards inside…</p>
            </div>
          </>
        )}

        {opening.phase === "error" && (
          <>
            <h2 className="display-type" id="reveal-title">The pack stayed sealed.</h2>
            <p className={styles.error} role="alert">{opening.error}</p>
            <button className="button" onClick={onClose} ref={primaryRef} type="button">Close</button>
          </>
        )}

        {current && (
          <>
            <h2 className="display-type" id="reveal-title">Card {index + 1} of {total}</h2>
            <div className={styles.progress} aria-hidden="true">
              {opening.cards.map((row, position) => (
                <span key={position} className={`${position < index ? styles.seen : ""} ${position === index ? styles.now : ""}`} />
              ))}
            </div>
            <div className={styles.stage} aria-live="polite">
              <div
                className={`${styles.flip} ${HIGH_RARITIES.has(rarityTone(current.card.rarity.name)) ? styles.glow : ""} ${cardStyles[`tone-${rarityTone(current.card.rarity.name)}`]}`}
                key={`${opening.id}-${index}`}
              >
                <div className={cardFrameClass(current.card, styles.bigCard)}>
                  <CardFace card={current.card} sizes="280px" priority />
                </div>
              </div>
              <p className={styles.caption}>
                <span className={cardStyles[`tone-${rarityTone(current.card.rarity.name)}`]}>{current.card.rarity.name}</span>
                {current.isNew && <strong className={styles.newBadge}>New to your collection</strong>}
              </p>
            </div>
            <div className={styles.actions}>
              <button className="button" onClick={() => goTo(index + 1)} ref={primaryRef} type="button">
                {index + 1 < total ? <>Next card <span aria-hidden="true">›</span></> : <>See the whole pack <span aria-hidden="true">›</span></>}
              </button>
              {index + 1 < total && (
                <button className={styles.skip} onClick={() => goTo(total)} type="button">Skip to the whole pack</button>
              )}
            </div>
          </>
        )}

        {finished && (
          <>
            <h2 className="display-type" id="reveal-title">Your pack</h2>
            <p className={styles.summaryLine}>
              {total} cards added to your collection{newCount > 0 ? `, ${newCount} new` : ""}. <Link className="text-link" href="/collection">See your collection</Link>
            </p>
            <div className={styles.summaryGrid}>
              {opening.cards.map((row, position) => (
                <div className={cardFrameClass(row.card)} key={`${row.card.id}-${position}`}>
                  <CardFace card={row.card} badge={row.isNew ? "New" : undefined} sizes="120px" compact />
                </div>
              ))}
            </div>
            <div className={styles.next}>
              {opening.remaining > 0 ? (
                <>
                  <p>Still sealed in your inventory: {opening.remaining} × {opening.packName}. Open another one?</p>
                  <div className={styles.actions}>
                    <button className="button" onClick={onOpenAnother} ref={primaryRef} type="button">Open another <span aria-hidden="true">✦</span></button>
                    <button className="button button--quiet" onClick={onClose} type="button">Not now</button>
                  </div>
                </>
              ) : (
                <>
                  <p>That was your last {opening.packName}. Want another one?</p>
                  <div className={styles.actions}>
                    <Link className="button" href="/shop">Buy another in the shop <span aria-hidden="true">→</span></Link>
                    <button className="button button--quiet" onClick={onClose} ref={primaryRef} type="button">Not now</button>
                  </div>
                </>
              )}
            </div>
          </>
        )}
      </section>
    </div>
  );
}
