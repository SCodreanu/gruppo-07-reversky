"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { CreditMark } from "@/components/credit-mark";
import styles from "./shop-experience.module.css";

type ShopPack = {
  id: string;
  name: string;
  cost: number;
  cardCount: number;
  purchaseLimit: number | null;
  remainingPurchases: number | null;
  contents: string[];
  bonusOdds: string[];
  note?: string;
};

type ShopSnapshot = {
  packs?: unknown;
  inventory?: unknown;
  catalogueUnavailable?: boolean;
};

type OpeningCard = { id?: number | string; name: string; rarity: string; isNew?: boolean };
type OpeningExperience = {
  id: number;
  packName: string;
  phase: "opening" | "revealed" | "error";
  preview: boolean;
  cards: OpeningCard[];
  error?: string;
};

const previewReveal: OpeningCard[] = [
  { name: "Common slot", rarity: "Common" },
  { name: "Uncommon slot", rarity: "Uncommon" },
  { name: "Rare slot", rarity: "Rare" },
];

const previewPacks: ShopPack[] = [
  {
    id: "preview-mini",
    name: "Mini",
    cost: 300,
    cardCount: 3,
    purchaseLimit: null,
    remainingPurchases: null,
    contents: ["1 Common", "1 Uncommon", "1 Rare slot"],
    bonusOdds: [],
    note: "Format under review",
  },
  {
    id: "preview-standard",
    name: "Standard",
    cost: 600,
    cardCount: 7,
    purchaseLimit: null,
    remainingPurchases: null,
    contents: ["3 Common", "2 Uncommon", "1 Rare", "1 flexible slot"],
    bonusOdds: [],
  },
  {
    id: "preview-premium",
    name: "Premium",
    cost: 1200,
    cardCount: 15,
    purchaseLimit: null,
    remainingPurchases: null,
    contents: ["5 Common", "4 Uncommon", "3 Rare", "3 flexible slots"],
    bonusOdds: [],
    note: "Format under review",
  },
];

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

function pickNumber(record: Record<string, unknown>, keys: string[]): number | null {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "number" && Number.isFinite(value)) return value;
    if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value);
  }
  return null;
}

function rarityName(value: unknown): string | null {
  if (typeof value === "string") return value;
  const record = asRecord(value);
  return record && typeof record.name === "string" ? record.name : null;
}

function packRows(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload;
  const record = asRecord(payload);
  if (!record) return [];
  for (const key of ["packs", "items", "content"]) {
    if (Array.isArray(record[key])) return record[key] as unknown[];
  }
  return [];
}

function normalisePack(value: unknown, index: number): ShopPack | null {
  const record = asRecord(value);
  if (!record) return null;
  const name = typeof record.name === "string" && record.name.trim() ? record.name : `Pack ${index + 1}`;
  const cost = pickNumber(record, ["cost", "price", "creditsCost"]);
  const rulesValue = record.slots ?? record.rarityRules ?? record.contents ?? record.composition;
  const rules = Array.isArray(rulesValue) ? rulesValue : [];
  const parsedContents = rules.flatMap((rule) => {
    const row = asRecord(rule);
    const rarity = row ? rarityName(row.rarity) : null;
    if (!row || !rarity) return [];
    const quantity = pickNumber(row, ["quantity", "count"]);
    return quantity === null ? [] : [`${quantity} ${rarity}`];
  });
  const cardCountFromRules = rules.reduce<number>((total, rule) => {
    const row = asRecord(rule);
    return total + (row ? pickNumber(row, ["quantity", "count"]) ?? 0 : 0);
  }, 0);
  const bonuses = Array.isArray(record.bonuses) ? record.bonuses : [];
  const bonusOdds = bonuses.flatMap((bonus) => {
    const row = asRecord(bonus);
    const rarity = row ? rarityName(row.rarity) : null;
    if (!row || !rarity) return [];
    const percentage = pickNumber(row, ["percentage"]) ?? ((pickNumber(row, ["probabilityUnits"]) ?? 0) / 100);
    return percentage > 0 ? [`${rarity} · ${new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(percentage)}%`] : [];
  });
  const id = record.id;
  const cardCount = pickNumber(record, ["cardCount", "totalCards", "numberOfCards"]) ?? cardCountFromRules;
  if (
    (typeof id !== "number" && typeof id !== "string") ||
    !/^\d+$/.test(String(id)) ||
    cost === null || cost < 0 ||
    cardCount <= 0
  ) {
    return null;
  }

  return {
    id: String(id),
    name,
    cost,
    cardCount,
    purchaseLimit: pickNumber(record, ["purchaseLimit", "purchase_limit"]) ?? null,
    remainingPurchases: pickNumber(record, ["remainingPurchases", "remainingPurchaseLimit", "remainingQuantity"]) ?? null,
    contents: parsedContents,
    bonusOdds,
  };
}

function readCredits(value: unknown): number | null {
  const record = asRecord(value);
  if (!record) return null;
  const direct = pickNumber(record, ["credits", "balance", "creditBalance", "currentCredits"]);
  if (direct !== null) return direct;
  const nested = asRecord(record.inventory ?? record.wallet ?? record.data);
  return nested ? pickNumber(nested, ["credits", "balance", "creditBalance"]) : null;
}

function readUnopenedPacks(value: unknown): Record<string, number> {
  const record = asRecord(value);
  if (!record) return {};
  const nested = asRecord(record.inventory ?? record.data) ?? record;
  const rows = nested.packInventory ?? nested.unopenedPacks ?? nested.packs;
  const result: Record<string, number> = {};
  if (Array.isArray(rows)) {
    for (const value of rows) {
      const row = asRecord(value);
      if (!row) continue;
      const pack = asRecord(row.pack);
      const rawId = row.packId ?? pack?.id ?? row.id;
      const quantity = pickNumber(row, ["quantity", "count", "remainingPacks"]);
      if ((typeof rawId === "number" || typeof rawId === "string") && quantity !== null && quantity > 0) {
        result[String(rawId)] = quantity;
      }
    }
  } else {
    const map = asRecord(rows);
    if (map) {
      for (const [id, value] of Object.entries(map)) {
        const row = asRecord(value);
        const quantity = typeof value === "number" ? value : row ? pickNumber(row, ["quantity", "count"]) : null;
        if (/^\d+$/.test(id) && quantity !== null && quantity > 0) result[id] = quantity;
      }
    }
  }
  return result;
}

function formatCredits(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}

async function responseMessage(response: Response, action: "purchase" | "open" = "purchase"): Promise<string> {
  const result = (await response.json().catch(() => ({}))) as { message?: string; detail?: string };
  if (response.status === 404) return action === "open" ? "Pack openings are not available yet." : "Pack purchases are not available yet.";
  return result.message ?? result.detail ?? "We couldn’t complete that request. Please try again.";
}

export function ShopExperience() {
  const router = useRouter();
  const [packs, setPacks] = useState(previewPacks);
  const [selectedId, setSelectedId] = useState("preview-standard");
  const [credits, setCredits] = useState<number | null>(null);
  const [ownedPacks, setOwnedPacks] = useState<Record<string, number>>({});
  const [catalogLive, setCatalogLive] = useState(false);
  const [balanceLive, setBalanceLive] = useState(false);
  const [previewNote, setPreviewNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [pending, setPending] = useState(false);
  const [openPending, setOpenPending] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [opening, setOpening] = useState<OpeningExperience | null>(null);
  const openingSequence = useRef(0);
  const openingDialogRef = useRef<HTMLElement>(null);
  const openingCloseRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLElement>(null);
  const dialogCloseRef = useRef<HTMLButtonElement>(null);

  const selectedPack = useMemo(
    () => packs.find((pack) => pack.id === selectedId) ?? packs[0],
    [packs, selectedId],
  );

  const loadShop = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch("/api/auth/shop", { cache: "no-store" });
      if (response.status === 401 || response.status === 403) {
        router.replace(`/login?next=${encodeURIComponent("/shop")}`);
        return;
      }
      if (!response.ok) {
        setCatalogLive(false);
        setBalanceLive(false);
        setCredits(null);
        setOwnedPacks({});
        setPacks(previewPacks);
        setSelectedId("preview-standard");
        setPreviewNote(
          response.status === 404
            ? "The live shop catalogue is still being connected. These pack formats and prices are a preview from the design."
            : "We can’t reach the live shop right now. These pack formats and prices are a preview from the design.",
        );
        return;
      }

      const result = (await response.json()) as ShopSnapshot;
      const livePacks = packRows(result.packs).map(normalisePack).filter((pack): pack is ShopPack => Boolean(pack));
      if (livePacks.length) {
        setPacks(livePacks);
        setCatalogLive(true);
        const preferredPack = livePacks.find((pack) => pack.name.toLowerCase() === "standard") ?? livePacks[0];
        setSelectedId(preferredPack.id);
        setPreviewNote("");
      } else {
        setCatalogLive(false);
        setPacks(previewPacks);
        setSelectedId("preview-standard");
        setPreviewNote(result.catalogueUnavailable
          ? "The live pack catalogue is still connecting. These formats and prices are a preview; your inventory balance remains live."
          : "The live catalogue has no available packs yet. These formats and prices are a design preview.");
      }
      const currentCredits = readCredits(result.inventory);
      setCredits(currentCredits);
      setBalanceLive(currentCredits !== null);
      setOwnedPacks(readUnopenedPacks(result.inventory));
    } catch {
      setCatalogLive(false);
      setBalanceLive(false);
      setCredits(null);
      setOwnedPacks({});
      setPacks(previewPacks);
      setSelectedId("preview-standard");
      setPreviewNote("We can’t reach the live shop right now. These pack formats and prices are a preview from the design.");
    } finally {
      setLoading(false);
    }
  }, [router]);

  useEffect(() => {
    void loadShop();
  }, [loadShop]);

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
    if (!selectedPack || !catalogLive || pending) return;
    setPending(true);
    setError("");
    setMessage("");
    try {
      const response = await fetch(`/api/auth/shop/purchase/${encodeURIComponent(selectedPack.id)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity }),
      });
      if (response.status === 401 || response.status === 403) {
        router.replace(`/login?next=${encodeURIComponent("/shop")}`);
        return;
      }
      if (!response.ok) {
        setError(await responseMessage(response));
        return;
      }

      const receipt = (await response.json().catch(() => null)) as Record<string, unknown> | null;
      const updatedCredits = receipt ? readCredits(receipt) : null;
      const nextCredits = updatedCredits ?? (credits === null ? null : credits - selectedPack.cost * quantity);
      setCredits(nextCredits);
      if (nextCredits !== null) window.dispatchEvent(new CustomEvent("reversky:credits-updated", { detail: nextCredits }));
      setMessage(`${quantity} ${selectedPack.name} ${quantity === 1 ? "pack is" : "packs are"} now in your inventory.`);
      setDialogOpen(false);
      setQuantity(1);
      void loadShop();
    } catch {
      setError("Connection unavailable. Please try again shortly.");
    } finally {
      setPending(false);
    }
  }

  async function previewOpening() {
    const sequence = openingSequence.current + 1;
    openingSequence.current = sequence;
    setOpening({ id: sequence, packName: selectedPack.name, phase: "opening", preview: true, cards: [] });
    await new Promise<void>((resolve) => window.setTimeout(resolve, 1650));
    setOpening((current) => current?.id === sequence ? { ...current, phase: "revealed" } : current);
  }

  async function openOwnedPack() {
    if (!selectedPack || !ownedPacks[selectedPack.id] || openPending) return;
    setOpenPending(true);
    const sequence = openingSequence.current + 1;
    openingSequence.current = sequence;
    setOpening({ id: sequence, packName: selectedPack.name, phase: "opening", preview: false, cards: [] });

    const [request] = await Promise.allSettled([
      fetch(`/api/auth/shop/open/${encodeURIComponent(selectedPack.id)}`, { method: "POST" }),
      new Promise<void>((resolve) => window.setTimeout(resolve, 1650)),
    ] as const);
    setOpenPending(false);
    if (request.status === "fulfilled" && request.value.ok) void loadShop();
    if (openingSequence.current !== sequence) return;
    if (request.status !== "fulfilled") {
      setOpening({ id: sequence, packName: selectedPack.name, phase: "error", preview: false, cards: [], error: "Connection unavailable. Your pack is still in your inventory." });
      return;
    }

    if (request.value.status === 401 || request.value.status === 403) {
      router.replace(`/login?next=${encodeURIComponent("/shop")}`);
      return;
    }
    if (!request.value.ok) {
      setOpening({ id: sequence, packName: selectedPack.name, phase: "error", preview: false, cards: [], error: await responseMessage(request.value, "open") });
      return;
    }

    const result = asRecord(await request.value.json().catch(() => null));
    const cards = Array.isArray(result?.cards) ? result.cards.flatMap((value) => {
      const row = asRecord(value);
      const card = row ? asRecord(row.card) : null;
      const cardName = card?.name;
      const cardId = card?.id;
      const name = typeof cardName === "string" ? cardName : typeof row?.name === "string" ? row.name : null;
      if (!row || !name) return [];
      return [{
        id: typeof cardId === "number" || typeof cardId === "string"
          ? cardId
          : typeof row.id === "number" || typeof row.id === "string" ? row.id : undefined,
        name,
        rarity: rarityName(card?.rarity ?? row.rarity) ?? "Card",
        isNew: typeof row.isNew === "boolean" ? row.isNew : undefined,
      } satisfies OpeningCard];
    }) : [];
    setOpening({ id: sequence, packName: selectedPack.name, phase: "revealed", preview: false, cards });
  }

  function closeOpening() {
    openingSequence.current += 1;
    setOpening(null);
  }

  useEffect(() => {
    if (opening?.id === undefined) return;
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    openingCloseRef.current?.focus();
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        openingSequence.current += 1;
        setOpening(null);
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = openingDialogRef.current?.querySelectorAll<HTMLElement>("button:not([disabled])");
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
  }, [opening?.id]);

  if (!selectedPack) return null;
  const totalCost = selectedPack.cost * quantity;
  const insufficientCredits = balanceLive && credits !== null && totalCost > credits;
  const maxQuantity = Math.min(10, selectedPack.remainingPurchases ?? 10);
  const selectedPackCount = catalogLive ? ownedPacks[selectedPack.id] ?? 0 : 0;

  return (
    <div className={styles.shop}>
      <section className={styles.wallet} aria-label="Credit balance">
        <div className={styles.walletMark} aria-hidden="true"><CreditMark className={styles.walletCoin} size={24} /></div>
        <div className={styles.walletText}>
          <p className="eyebrow">Your credits</p>
          <strong>{loading ? "Loading…" : balanceLive && credits !== null ? formatCredits(credits) : "Unavailable"}</strong>
          <span>{balanceLive ? "Available to spend" : "Live balance will appear here"}</span>
        </div>
        <Link href="/guide" className={styles.walletLink}>How to earn credits <span aria-hidden="true">→</span></Link>
      </section>

      {previewNote && <p className={styles.previewNote} role="status"><span aria-hidden="true">✦</span>{previewNote}</p>}
      {message && <p className={styles.successNote} role="status"><span aria-hidden="true">✓</span>{message}</p>}

      <div className={styles.sectionHeading}>
        <div>
          <p className="eyebrow">Sealed for the crossing</p>
          <h2 className="display-type">Choose your pack</h2>
        </div>
        <span className={styles.draftLabel}>{catalogLive ? "Live catalogue" : "Formats and prices to validate"}</span>
      </div>

      <div className={styles.layout}>
        <div className={styles.packList} aria-label="Available packs" role="group">
          {packs.map((pack, index) => {
            const active = selectedPack.id === pack.id;
            const thumbnailTone = index % 3 === 1 ? styles.packThumbnail1 : index % 3 === 2 ? styles.packThumbnail2 : "";
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
                {pack.note && <span className={styles.reviewChip}>{pack.note}</span>}
              </button>
            );
          })}
          {!packs.length && <p className={styles.emptyState}>There are no packs available right now.</p>}
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
            <div className={styles.detailStats}>
              <div><span>Cards</span><strong>{selectedPack.cardCount}</strong></div>
              <div><span>Price</span><strong><CreditMark className={styles.inlineCoin} size={12} />{formatCredits(selectedPack.cost)} <small>credits</small></strong></div>
            </div>
            <div className={styles.contents}>
              <p className="eyebrow">Expected contents</p>
              {selectedPack.contents.length ? (
                <ul>
                  {selectedPack.contents.map((item) => <li key={item}>{item}</li>)}
                </ul>
              ) : <p className={styles.oddsNote}>The live catalogue has not published the pack composition yet.</p>}
              {selectedPack.bonusOdds.length > 0 ? (
                <>
                  <p className="eyebrow">Bonus chances</p>
                  <ul>{selectedPack.bonusOdds.map((item) => <li key={item}>{item}</li>)}</ul>
                </>
              ) : (
                <p className={styles.oddsNote}>{catalogLive ? "No bonus cards are configured for this pack." : "Bonus cards and exact odds appear when the catalogue is connected."}</p>
              )}
            </div>
            {selectedPack.remainingPurchases !== null && (
              <p className={styles.limitNote}>{selectedPack.remainingPurchases} purchases remaining for this pack.</p>
            )}
            {selectedPackCount > 0 && <p className={styles.ownedCopy}>{selectedPackCount} unopened {selectedPackCount === 1 ? "pack" : "packs"} in your inventory.</p>}
            {selectedPackCount > 0 && (
              <button className={styles.openButton} disabled={openPending} onClick={() => void openOwnedPack()} type="button">
                {openPending ? "Opening pack…" : "Open one from your inventory"} <span aria-hidden="true">✦</span>
              </button>
            )}
            <button className={styles.previewButton} onClick={() => void previewOpening()} type="button">
              Preview the opening animation <span aria-hidden="true">↗</span>
            </button>
            <button
              className={`button ${styles.purchaseButton}`}
              disabled={!catalogLive || selectedPack.remainingPurchases === 0}
              onClick={() => {
                setError("");
                setQuantity(1);
                setDialogOpen(true);
              }}
              type="button"
            >
              {catalogLive ? `Choose ${formatCredits(selectedPack.cost)} credits` : "Preview pack"}
              <span aria-hidden="true">›</span>
            </button>
            {!catalogLive && <p className={styles.unavailableCopy}>Purchases will open when the shop catalogue is connected.</p>}
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
            <p className={styles.dialogSummary}>{selectedPack.name} pack <span>·</span> {selectedPack.cardCount} cards</p>
            <div className={styles.quantityRow}>
              <span>Quantity</span>
              <div className={styles.quantityControl}>
                <button aria-label="Remove one pack" disabled={quantity <= 1} onClick={() => setQuantity((current) => Math.max(1, current - 1))} type="button">−</button>
                <strong>{quantity}</strong>
                <button aria-label="Add one pack" disabled={quantity >= maxQuantity} onClick={() => setQuantity((current) => Math.min(maxQuantity, current + 1))} type="button">+</button>
              </div>
            </div>
            <div className={styles.totalRow}><span>Total</span><strong><CreditMark className={styles.inlineCoin} size={14} />{formatCredits(totalCost)} credits</strong></div>
            {balanceLive && credits !== null && <p className={styles.balanceAfter}>Balance after purchase <span>{formatCredits(credits - totalCost)} credits</span></p>}
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

      {opening && (
        <div className={styles.openingBackdrop} onMouseDown={(event) => { if (event.target === event.currentTarget) closeOpening(); }}>
          <section aria-labelledby="opening-title" aria-live="polite" aria-modal="true" className={styles.openingDialog} ref={openingDialogRef} role="dialog">
            <button aria-label="Close pack opening" className={styles.dialogClose} onClick={closeOpening} ref={openingCloseRef} type="button">×</button>
            <p className="eyebrow">{opening.preview ? "Visual preview" : "Pack opening"}</p>
            <h2 className="display-type" id="opening-title">{opening.phase === "opening" ? "The seal is breaking." : opening.phase === "error" ? "The pack stayed sealed." : "Cards revealed."}</h2>
            <p className={styles.openingSubtitle}>{opening.packName} / {opening.preview ? "opening preview" : "your pack"}</p>
            {opening.phase === "opening" && (
              <div className={styles.openingStage}>
                <span aria-hidden="true" className={styles.openingSpark}>✦</span>
                <div className={styles.openingPack}>
                  <Image src="/images/reversky-card-back.png" alt="" fill sizes="145px" />
                  <span aria-hidden="true" className={styles.tearLine} />
                </div>
                <p>{opening.preview ? "A glimpse of what waits inside…" : "Revealing the cards inside…"}</p>
              </div>
            )}
            {opening.phase === "revealed" && (
              <>
                <div className={styles.revealGrid}>
                  {(opening.preview ? previewReveal : opening.cards).map((card, index) => (
                    <article className={styles.revealCard} key={card.id ?? `${card.rarity}-${card.name}-${index}`}>
                      <span className={styles.revealCardMark} aria-hidden="true">✦</span>
                      <span className={styles.revealRarity}>{card.rarity}</span>
                      <strong>{card.name}</strong>
                      {card.isNew && <small>New to collection</small>}
                    </article>
                  ))}
                </div>
                {opening.preview
                  ? <p className={styles.previewOnly}>Animation preview only. No cards are added to your collection.</p>
                  : <p className={styles.openingResultCount}>{opening.cards.length} {opening.cards.length === 1 ? "card" : "cards"} added to your collection.</p>}
              </>
            )}
            {opening.phase === "error" && <p className={styles.errorNote} role="alert">{opening.error}</p>}
            {opening.phase !== "opening" && <button className={`button ${styles.closeOpening}`} onClick={closeOpening} type="button">Close</button>}
          </section>
        </div>
      )}
    </div>
  );
}
