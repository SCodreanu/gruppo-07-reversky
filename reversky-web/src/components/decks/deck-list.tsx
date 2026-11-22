"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { gameFetch, problemMessage } from "@/lib/api/browser";
import type { DeckSummary } from "@/lib/api/contracts";
import cardStyles from "@/components/cards/cards.module.css";
import styles from "./decks.module.css";

type ListState = { decks?: DeckSummary[]; error?: string };

export function DeckList() {
  const router = useRouter();
  const [state, setState] = useState<ListState | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    let active = true;
    void gameFetch<DeckSummary[]>("decks").then((result) => {
      if (!active) return;
      if (!result.ok && result.status === 401) {
        router.replace(`/login?next=${encodeURIComponent("/decks")}`);
        return;
      }
      setState(result.ok ? { decks: result.data } : { error: problemMessage(result.problem) });
    });
    return () => {
      active = false;
    };
  }, [attempt, router]);

  async function deleteDeck(deck: DeckSummary) {
    setDeletingId(deck.id);
    setActionError("");
    const result = await gameFetch<null>(`decks/${deck.id}`, { method: "DELETE" });
    setDeletingId(null);
    setConfirmingId(null);
    if (!result.ok) {
      setActionError(problemMessage(result.problem));
      return;
    }
    setState((current) => ({ decks: current?.decks?.filter((item) => item.id !== deck.id) ?? [] }));
  }

  if (!state) return <div className={cardStyles.state} aria-live="polite"><p>Loading your decks…</p></div>;
  if (state.error) {
    return (
      <div className={cardStyles.state} role="alert">
        <p>{state.error}</p>
        <button className="button button--quiet" type="button" onClick={() => {
          setState(null);
          setAttempt((value) => value + 1);
        }}>Try again</button>
      </div>
    );
  }

  const decks = state.decks ?? [];
  return (
    <>
      <div className={styles.listHeading}>
        <span className={styles.muted}>{decks.length} {decks.length === 1 ? "deck" : "decks"}</span>
        <Link className="button" href="/decks/new">New deck <span aria-hidden="true">+</span></Link>
      </div>
      {actionError && <p className="input-error" role="alert">{actionError}</p>}
      {decks.length === 0 ? (
        <div className={cardStyles.state}><p>You have no decks yet. Build one from your collection.</p></div>
      ) : (
        <ul className={styles.deckGrid}>
          {decks.map((deck) => (
            <li className={styles.deckCard} key={deck.id}>
              <Link className={styles.deckLink} href={`/decks/${deck.id}`}>
                <span className={styles.deckMark} aria-hidden="true">▤</span>
                <strong className="display-type">{deck.name}</strong>
                <span className={styles.muted}>{deck.cardCount} cards</span>
                <span className={deck.valid ? styles.valid : styles.invalid}>{deck.valid ? "Ready to play" : "Not valid"}</span>
              </Link>
              <div className={styles.deckActions}>
                <Link className="text-link" href={`/decks/${deck.id}`}>Edit</Link>
                {confirmingId === deck.id ? (
                  <span className={styles.confirm}>
                    <button type="button" className={styles.dangerLink} disabled={deletingId === deck.id} onClick={() => void deleteDeck(deck)}>
                      {deletingId === deck.id ? "Deleting…" : "Confirm delete"}
                    </button>
                    <button type="button" className={styles.plainLink} onClick={() => setConfirmingId(null)}>Cancel</button>
                  </span>
                ) : (
                  <button type="button" className={styles.plainLink} onClick={() => setConfirmingId(deck.id)}>Delete</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
