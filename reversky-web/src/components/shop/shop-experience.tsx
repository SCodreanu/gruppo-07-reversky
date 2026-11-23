"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { CreditMark } from "@/components/credit-mark";
import { gameFetch, needsLogin, problemMessage, type GameResult } from "@/lib/api/browser";
import type { Inventory, Pack, PackPurchaseRequest } from "@/lib/api/contracts";
import styles from "./shop-experience.module.css";

const MAX_PURCHASE_QUANTITY = 10;
const percentFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 });

function formatCredits(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}

function packContents(pack: Pack): string[] {
  return pack.slots.map((slot) => `${slot.quantity} ${slot.rarity.name}`);
}

function bonusOdds(pack: Pack): string[] {
  return pack.bonuses
    .filter((bonus) => bonus.percentage > 0)
    .map((bonus) => `${bonus.rarity.name} · ${percentFormat.format(bonus.percentage)}%`);
}

function ownedCounts(inventory: Inventory): Record<number, number> {
  return Object.fromEntries(inventory.packs.map((row) => [row.packId, row.quantity]));
}

type ShopLoad = [GameResult<Pack[]>, GameResult<Inventory>];

function fetchShop(): Promise<ShopLoad> {
  return Promise.all([gameFetch<Pack[]>("packs"), gameFetch<Inventory>("inventory")]);
}

function announceCredits(credits: number) {
  window.dispatchEvent(new CustomEvent("reversky:credits-updated", { detail: credits }));
}

export function ShopExperience() {
  const router = useRouter();
  const [packs, setPacks] = useState<Pack[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [credits, setCredits] = useState<number | null>(null);
  const [ownedPacks, setOwnedPacks] = useState<Record<number, number>>({});
  const [loadError, setLoadError] = useState("");
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [pending, setPending] = useState(false);
  const [purchased, setPurchased] = useState<{ quantity: number; packName: string } | null>(null);
  const [error, setError] = useState("");
  const dialogRef = useRef<HTMLElement>(null);
  const dialogCloseRef = useRef<HTMLButtonElement>(null);

  const selectedPack = useMemo(
    () => packs.find((pack) => pack.id === selectedId) ?? packs[0],
    [packs, selectedId],
  );

  const goToLogin = useCallback(() => {
    router.replace(`/login?next=${encodeURIComponent("/shop")}`);
  }, [router]);

  const applyShop = useCallback(([packResult, inventoryResult]: ShopLoad) => {
    setLoading(false);
    if (needsLogin(inventoryResult)) {
      goToLogin();
      return;
    }
    if (!packResult.ok || !inventoryResult.ok) {
      const problem = !packResult.ok ? packResult.problem : !inventoryResult.ok ? inventoryResult.problem : {};
      setLoadError(problemMessage(problem, "We can’t reach the shop right now."));
      return;
    }
    setLoadError("");
    // Cheapest first (Mini, Standard, Premium); the Standard is selected first, as in the mockup
    const sorted = [...packResult.data].sort((a, b) => a.cost - b.cost || a.id - b.id);
    setPacks(sorted);
    setSelectedId((current) =>
      sorted.some((pack) => pack.id === current)
        ? current
        : (sorted.find((pack) => pack.name.toLowerCase() === "standard") ?? sorted[0])?.id ?? null,
    );
    setCredits(inventoryResult.data.credits);
    setOwnedPacks(ownedCounts(inventoryResult.data));
  }, [goToLogin]);

  const loadShop = useCallback(() => fetchShop().then(applyShop), [applyShop]);

  useEffect(() => {
    let active = true;
    void fetchShop().then((result) => {
      if (active) applyShop(result);
    });
    return () => {
      active = false;
    };
  }, [applyShop]);

  useEffect(() => {
    if (!dialogOpen) return;
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialogCloseRef.current?.focus();
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setDialogOpen(false);
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = dialogRef.current?.querySelectorAll<HTMLElement>("button:not([disabled])");
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
  }, [dialogOpen]);

  async function confirmPurchase() {
    if (!selectedPack || pending) return;
    setPending(true);
    setError("");
    setPurchased(null);
    const body: PackPurchaseRequest = { quantity };
    const result = await gameFetch<Inventory>(`packs/${selectedPack.id}/purchase`, { method: "POST", body });
    setPending(false);
    if (needsLogin(result)) {
      goToLogin();
      return;
    }
    if (!result.ok) {
      setError(problemMessage(result.problem));
      return;
    }

    setCredits(result.data.credits);
    setOwnedPacks(ownedCounts(result.data));
    announceCredits(result.data.credits);
    setPurchased({ quantity, packName: selectedPack.name });
    setDialogOpen(false);
    setQuantity(1);
    // remainingPurchases changes for packs with a limit: read the catalogue again.
    if (selectedPack.purchaseLimit !== null) void loadShop();
  }

  const wallet = (
    <section className={styles.wallet} aria-label="Credit balance">
      <div className={styles.walletMark} aria-hidden="true"><CreditMark className={styles.walletCoin} size={24} /></div>
      <div className={styles.walletText}>
        <p className="eyebrow">Your credits</p>
        <strong>{loading && credits === null ? "Loading…" : credits !== null ? formatCredits(credits) : "Unavailable"}</strong>
        <span>{credits !== null ? "Available to spend" : "Your balance appears when the shop is reachable"}</span>
      </div>
      <Link href="/guide" className={styles.walletLink}>How to earn credits <span aria-hidden="true">→</span></Link>
    </section>
  );

  if (loadError || !selectedPack) {
    return (
      <div className={styles.shop}>
        {wallet}
        {loading ? (
          <p className={styles.previewNote} role="status">Loading the shop…</p>
        ) : (
          <div className={styles.emptyState} role={loadError ? "alert" : "status"}>
            <p>{loadError || "There are no packs on sale right now."}</p>
            <button className="button button--quiet" onClick={() => {
              setLoading(true);
              void loadShop();
            }} type="button">Try again</button>
          </div>
        )}
      </div>
    );
  }

  const totalCost = selectedPack.cost * quantity;
  const insufficientCredits = credits !== null && totalCost > credits;
  const maxQuantity = Math.min(MAX_PURCHASE_QUANTITY, selectedPack.remainingPurchases ?? MAX_PURCHASE_QUANTITY);
  const selectedPackCount = ownedPacks[selectedPack.id] ?? 0;
  const contents = packContents(selectedPack);
  const odds = bonusOdds(selectedPack);

  return (
    <div className={styles.shop}>
      {wallet}

      {purchased && (
        <p className={styles.successNote} role="status">
          <span aria-hidden="true">✓</span>
          {purchased.quantity} × {purchased.packName} added to your inventory.
          <Link className="text-link" href="/inventory">Open {purchased.quantity === 1 ? "it" : "them"} now →</Link>
        </p>
      )}

      <div className={styles.sectionHeading}>
        <div>
          <p className="eyebrow">Sealed for the crossing</p>
          <h2 className="display-type">Choose your pack</h2>
        </div>
        <span className={styles.draftLabel}>Live catalogue</span>
      </div>

      <div className={styles.layout}>
        <div className={styles.packList} aria-label="Available packs" role="group">
          {packs.map((pack, index) => {
            const active = selectedPack.id === pack.id;
            const thumbnailTone = index % 3 === 1 ? styles.packThumbnail1 : index % 3 === 2 ? styles.packThumbnail2 : "";
            const owned = ownedPacks[pack.id] ?? 0;
            return (
              <button
                aria-pressed={active}
                className={`${styles.packOption} ${active ? styles.packOptionActive : ""}`}
                key={pack.id}
                onClick={() => {
                  setSelectedId(pack.id);
                  setError("");
                }}
                type="button"
              >
                <span className={`${styles.packThumbnail} ${thumbnailTone}`}>
                  <Image src="/images/reversky-card-back.png" alt="" fill sizes="72px" />
                </span>
                <span className={styles.packOptionCopy}>
                  <span className={styles.packName}>{pack.name}</span>
                  <span className={styles.packMeta}>{pack.cardCount} cards</span>
                  <span className={styles.packPrice}><CreditMark className={styles.inlineCoin} size={12} />{formatCredits(pack.cost)} credits</span>
                </span>
                <span className={styles.packArrow} aria-hidden="true">›</span>
                {owned > 0 && <span className={styles.reviewChip}>{owned} owned</span>}
              </button>
            );
          })}
        </div>

        <section className={styles.details} aria-labelledby="selected-pack-title">
          <div className={`${styles.detailArtwork} ${styles[`detailArtwork${packs.indexOf(selectedPack) % 3}`]}`}>
            <span className={styles.artGlow} aria-hidden="true" />
            <Image src="/images/reversky-card-back.png" alt="" fill sizes="(max-width: 640px) 150px, 220px" />
            <span className={styles.editionMark} aria-hidden="true">✦</span>
          </div>
          <div className={styles.detailCopy}>
            <p className="eyebrow">{selectedPack.name} / sealed pack</p>
            <h3 className="display-type" id="selected-pack-title">{selectedPack.name}</h3>
            <p className={styles.detailDescription}>{selectedPack.cardCount} cards to reveal. Keep the pack sealed in your inventory until the moment feels right.</p>
            {selectedPackCount > 0 && (
              <Link className={styles.openButton} href="/inventory">
                {selectedPackCount} sealed in your inventory · open {selectedPackCount === 1 ? "it" : "them"} <span aria-hidden="true">✦</span>
              </Link>
            )}
            <div className={styles.detailStats}>
              <div><span>Cards</span><strong>{selectedPack.cardCount}</strong></div>
              <div><span>Price</span><strong><CreditMark className={styles.inlineCoin} size={12} />{formatCredits(selectedPack.cost)} <small>credits</small></strong></div>
            </div>
            <div className={styles.contents}>
              <p className="eyebrow">Expected contents</p>
              <ul>
                {contents.map((item) => <li key={item}>{item}</li>)}
              </ul>
              {odds.length > 0 ? (
                <>
                  <p className="eyebrow">Bonus chances</p>
                  <ul>{odds.map((item) => <li key={item}>{item}</li>)}</ul>
                </>
              ) : (
                <p className={styles.oddsNote}>No bonus cards are configured for this pack.</p>
              )}
            </div>
            {selectedPack.remainingPurchases !== null && (
              <p className={styles.limitNote}>{selectedPack.remainingPurchases} purchases remaining for this pack.</p>
            )}
            <button
              className={`button ${styles.purchaseButton}`}
              disabled={selectedPack.remainingPurchases === 0}
              onClick={() => {
                setError("");
                setQuantity(1);
                setDialogOpen(true);
              }}
              type="button"
            >
              {`Choose ${formatCredits(selectedPack.cost)} credits`}
              <span aria-hidden="true">›</span>
            </button>
            {error && <p className={styles.errorNote} role="alert">{error}</p>}
          </div>
        </section>
      </div>

      <p className={styles.footerNote}>Credits are earned by playing. Pack purchases use in-game credits only.</p>

      {dialogOpen && (
        <div className={styles.dialogBackdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) setDialogOpen(false); }}>
          <section aria-labelledby="purchase-title" aria-modal="true" className={styles.dialog} ref={dialogRef} role="dialog">
            <button aria-label="Close purchase confirmation" className={styles.dialogClose} onClick={() => setDialogOpen(false)} ref={dialogCloseRef} type="button">×</button>
            <p className="eyebrow">Final check</p>
            <h2 className="display-type" id="purchase-title">Confirm your purchase</h2>
            <p className={styles.dialogSummary}>{selectedPack.name} <span>·</span> {selectedPack.cardCount} cards</p>
            <div className={styles.quantityRow}>
              <span>Quantity</span>
              <div className={styles.quantityControl}>
                <button aria-label="Remove one pack" disabled={quantity <= 1} onClick={() => setQuantity((current) => Math.max(1, current - 1))} type="button">−</button>
                <strong>{quantity}</strong>
                <button aria-label="Add one pack" disabled={quantity >= maxQuantity} onClick={() => setQuantity((current) => Math.min(maxQuantity, current + 1))} type="button">+</button>
              </div>
            </div>
            <div className={styles.totalRow}><span>Total</span><strong><CreditMark className={styles.inlineCoin} size={14} />{formatCredits(totalCost)} credits</strong></div>
            {credits !== null && !insufficientCredits && <p className={styles.balanceAfter}>Balance after purchase <span>{formatCredits(credits - totalCost)} credits</span></p>}
            {insufficientCredits && <p className={styles.errorNote} role="alert">You need {formatCredits(totalCost - (credits ?? 0))} more credits for this purchase.</p>}
            {error && <p className={styles.errorNote} role="alert">{error}</p>}
            <button className={`button ${styles.purchaseButton}`} disabled={pending || insufficientCredits} onClick={() => void confirmPurchase()} type="button">
              {pending ? "Confirming…" : `Confirm for ${formatCredits(totalCost)} credits`}
              {!pending && <span aria-hidden="true">›</span>}
            </button>
            <button className={styles.cancelButton} onClick={() => setDialogOpen(false)} type="button">Cancel</button>
          </section>
        </div>
      )}

    </div>
  );
}
