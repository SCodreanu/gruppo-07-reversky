"use client";

import Link from "next/link";
import { useState } from "react";
import styles from "./verification-card.module.css";

type State = "ready" | "pending" | "verified" | "invalid" | "unavailable";

export function VerificationCard({ token }: { token: string }) {
  const [state, setState] = useState<State>(token ? "ready" : "invalid");

  async function verify() {
    if (!token) return;
    setState("pending");
    try {
      const response = await fetch("/api/auth/verify-email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token }),
      });
      if (response.ok) setState("verified");
      else {
        const problem = (await response.json().catch(() => ({}))) as { code?: string };
        setState(problem.code === "API_UNAVAILABLE" ? "unavailable" : "invalid");
      }
    } catch {
      setState("unavailable");
    }
  }

  return (
    <div className={styles.card}>
      {state === "ready" || state === "pending" ? (
        <>
          <p className="notice">Confirm your email address to complete your first sign-in.</p>
          <button className="button" type="button" onClick={verify} disabled={state === "pending"}>
            {state === "pending" ? "Verifying…" : "Confirm email"}
          </button>
        </>
      ) : state === "verified" ? (
        <>
          <p className="notice" role="status">Email verified. Your account is ready.</p>
          <Link className="button" href="/home">Go to home</Link>
        </>
      ) : (
        <>
          <p className="notice notice--error" role="alert">
            {state === "unavailable"
              ? "The service is unavailable. Please try again shortly."
              : "This link is invalid or has expired. Sign in to request a new one."}
          </p>
          <Link className="button" href="/login">Go to sign in</Link>
        </>
      )}
    </div>
  );
}
