"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { CreditMark } from "@/components/credit-mark";
import { PackRevealDialog, SEAL_ANIMATION_MS, type PackOpening } from "@/components/packs/pack-reveal-dialog";
import { gameFetch, needsLogin, problemMessage } from "@/lib/api/browser";
import type { Inventory, InventoryPack, OpenPackResponse } from "@/lib/api/contracts";
import styles from "./inventory.module.css";

type LoadState = { inventory?: Inventory; error?: string };

export function InventoryView() {
  const router = useRouter();
  const [state, setState] = useState<LoadState | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [opening, setOpening] = useState<PackOpening | null>(null);
  const [openingPack, setOpeningPack] = useState<InventoryPack | null>(null);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const sequence = useRef(0);

  const goToLogin = useCallback(() => {
    router.replace(`/login?next=${encodeURIComponent("/inventory")}`);
  }, [router]);

  useEffect(() => {
    let active = true;
    void gameFetch<Inventory>("inventory").then((result) => {
      if (!active) return;
      if (needsLogin(result)) {
        goToLogin();
        return;
      }
      setState(result.ok ? { inventory: result.data } : { error: problemMessage(result.problem) });
    });
    return () => {
      active = false;
    };
  }, [attempt, goToLogin]);

  const openPack = useCallback(async (pack: InventoryPack) => {
    const id = sequence.current + 1;
    sequence.current = id;
    setPendingId(pack.packId);
    setOpeningPack(pack);
    setOpening({ id, packName: pack.name, phase: "sealed", cards: [], remaining: pack.quantity - 1 });

    const [result] = await Promise.all([
      gameFetch<OpenPackResponse>(`inventory/packs/${pack.packId}/open`, { method: "POST" }),
      new Promise<void>((resolve) => window.setTimeout(resolve, SEAL_ANIMATION_MS)),
    ]);
    setPendingId(null);
    if (needsLogin(result)) {
      goToLogin();
      return;
    }
    if (result.ok) {
      const remaining = result.data.remainingPacks;
      setState((current) => current?.inventory ? {
        inventory: {
          ...current.inventory,
          packs: current.inventory.packs
            .map((row) => row.packId === pack.packId ? { ...row, quantity: remaining } : row)
            .filter((row) => row.quantity > 0),
        },
      } : current);
      setOpeningPack({ ...pack, quantity: remaining });
    }
    if (sequence.current !== id) return;
    setOpening(result.ok
      ? { id, packName: pack.name, phase: "revealing", cards: result.data.cards, remaining: result.data.remainingPacks }
      : {
        id,
        packName: pack.name,
        phase: "error",
        cards: [],
        remaining: pack.quantity,
        error: `${problemMessage(result.problem)} Your pack is still in your inventory.`,
      });
  }, [goToLogin]);

  const closeOpening = useCallback(() => {
    sequence.current += 1;
    setOpening(null);
    setOpeningPack(null);
  }, []);

  const openAnother = useCallback(() => {
    if (openingPack && openingPack.quantity > 0) void openPack(openingPack);
  }, [openingPack, openPack]);

  if (!state) return <p className={styles.note} aria-live="polite">Loading your inventory…</p>;
  if (state.error || !state.inventory) {
    return (
      <div className={styles.empty} role="alert">
        <p>{state.error ?? "We can’t reach your inventory right now."}</p>
        <button className="button button--quiet" type="button" onClick={() => {
          setState(null);
          setAttempt((value) => value + 1);
        }}>Try again</button>
      </div>
    );
  }

  const { credits, packs } = state.inventory;
  const sealed = packs.reduce((total, row) => total + row.quantity, 0);

  return (
    <div className={styles.inventory}>
      <section className={styles.summary} aria-label="Inventory summary">
        <div>
          <p className="eyebrow">Sealed packs</p>
          <strong className="display-type">{sealed}</strong>
        </div>
        <div>
          <p className="eyebrow">Credits</p>
          <strong className={styles.credits}><CreditMark size={18} />{new Intl.NumberFormat("en-US").format(credits)}</strong>
        </div>
        <Link className="button button--quiet" href="/shop">Buy more packs <span aria-hidden="true">→</span></Link>
      </section>

      {packs.length === 0 ? (
        <div className={styles.empty}>
          <p>You have no sealed packs. Buy one in the shop, then come back here to open it.</p>
          <Link className="button" href="/shop">Go to the shop</Link>
        </div>
      ) : (
        <ul className={styles.packGrid}>
          {packs.map((pack) => (
            <li className={styles.pack} key={pack.packId}>
              <div className={styles.packArt}>
                <Image src="/images/reversky-card-back.png" alt="" fill sizes="160px" />
                <span className={styles.count}>×{pack.quantity}</span>
              </div>
              <div className={styles.packCopy}>
                <h2 className="display-type">{pack.name}</h2>
                <p>{pack.quantity} sealed {pack.quantity === 1 ? "pack" : "packs"}</p>
                <button className="button" type="button" disabled={pendingId !== null} onClick={() => void openPack(pack)}>
                  {pendingId === pack.packId ? "Opening…" : "Open a pack"} <span aria-hidden="true">✦</span>
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {opening && (
        <PackRevealDialog opening={opening} onClose={closeOpening} onOpenAnother={openAnother} />
      )}
    </div>
  );
}
