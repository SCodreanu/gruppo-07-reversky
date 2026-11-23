"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { gameFetch, problemMessage } from "@/lib/api/browser";
import type { CatalogCard } from "@/lib/api/contracts";
import { factionLine, rarityTone, subtypeLabel, typeLabel } from "./card-meta";
import { CardFace, cardFrameClass } from "./card-tile";
import styles from "./cards.module.css";

/** "Furia del ferito: la prima volta…" -> the effect's name and its rule (the catalog writes them together). */
export function splitEffect(description: string): { name: string | null; rule: string } {
  const match = description.match(/^([^:]{2,60}):\s*([\s\S]+)$/);
  if (!match) return { name: null, rule: description };
  const rule = match[2].trim();
  return { name: match[1].trim(), rule: rule.charAt(0).toLocaleUpperCase("it") + rule.slice(1) };
}

function EffectBlock({ card }: { card: CatalogCard }) {
  if (!card.effectDescription) {
    return (
      <div className={styles.detailBlock}>
        <p className="eyebrow">Effect</p>
        <p>{card.type === "CREATURE"
          ? "No special effect: this creature fights with its ATK and DEF only."
          : "No effect text in the catalog for this card yet."}</p>
      </div>
    );
  }
  const effect = splitEffect(card.effectDescription);
  return (
    <div className={styles.detailBlock}>
      <p className="eyebrow">Effect</p>
      {effect.name && <h3 className={`display-type ${styles.effectName}`}>{effect.name}</h3>}
      <p>{effect.rule}</p>
    </div>
  );
}

/** Card details from GET /cards/{id} (adds the tags), shown over the page. */
export function CardDetailDialog({
  card,
  onClose,
  children,
}: {
  card: CatalogCard;
  onClose: () => void;
  children?: ReactNode;
}) {
  const [detail, setDetail] = useState<{ id: number; card?: CatalogCard; error?: string } | null>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const loaded = detail?.id === card.id ? detail : null;
  const shown = loaded?.card ?? card;

  useEffect(() => {
    let active = true;
    void gameFetch<CatalogCard>(`cards/${card.id}`).then((result) => {
      if (!active) return;
      setDetail(result.ok ? { id: card.id, card: result.data } : { id: card.id, error: problemMessage(result.problem) });
    });
    return () => {
      active = false;
    };
  }, [card.id]);

  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    closeRef.current?.focus();
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      opener?.focus();
    };
  }, [onClose]);

  const subtype = subtypeLabel(shown.subtype);
  return (
    <div className={styles.backdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className={`${styles.detail} ${styles[`tone-${rarityTone(shown.rarity.name)}`]}`} role="dialog" aria-modal="true" aria-labelledby="card-detail-title">
        <button ref={closeRef} className={styles.dialogClose} type="button" aria-label="Close card details" onClick={onClose}>×</button>
        <div className={styles.detailLayout}>
          <div className={`${cardFrameClass(shown)} ${styles.detailCard}`}>
            <CardFace card={shown} sizes="240px" priority />
          </div>
          <div className={styles.detailBody}>
            <p className="eyebrow">{shown.rarity.name} · {typeLabel(shown.type)}{subtype ? ` · ${subtype}` : ""}</p>
            <h2 id="card-detail-title" className="display-type">{shown.name}</h2>
            <p className={styles.detailFaction}>{factionLine(shown)}</p>
            {shown.atk !== null && shown.def !== null && (
              <div className={styles.detailStats}>
                <div><span>ATK</span><strong>{shown.atk}</strong></div>
                <div><span>DEF</span><strong>{shown.def}</strong></div>
                {shown.copyLimit !== null && <div><span>Copies per deck</span><strong>{shown.copyLimit}</strong></div>}
              </div>
            )}
            <EffectBlock card={shown} />
            {shown.flavorText && <blockquote className={styles.flavor}>{shown.flavorText}</blockquote>}
            {loaded?.card?.tags && loaded.card.tags.length > 0 && (
              <ul className={styles.tags} aria-label="Tags">
                {loaded.card.tags.map((tag) => <li key={tag.id}>{tag.name}</li>)}
              </ul>
            )}
            {!shown.active && <p className={styles.inactiveNote}>This card no longer drops from packs and can’t be used in a deck.</p>}
            {loaded?.error && <p className="input-error" role="alert">{loaded.error}</p>}
            {children}
          </div>
        </div>
      </section>
    </div>
  );
}
