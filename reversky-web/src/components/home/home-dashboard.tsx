"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { Brand } from "@/components/brand-mark";
import { DashboardHeader } from "@/components/home/dashboard-header";
import { PortalMobileNav } from "@/components/home/portal-mobile-nav";
import type { UserProfile } from "@/lib/api/contracts";
import { getOrCreateProfileAvatar } from "@/lib/profile-avatar";
import styles from "./home-dashboard.module.css";

type SessionResponse = { user?: UserProfile; message?: string; code?: string };

const sections = [
  { number: "01", title: "Catalog", text: "Discover the cards and stories that cross the veil." },
  { number: "02", title: "Collection", text: "Collect cards and choose which ones to take with you." },
  { number: "03", title: "Decks", text: "Build a strategy around your favorite cards." },
  { number: "04", title: "Challenges", text: "Meet other players at the Reversky table." },
];

export function HomeDashboard({ welcome = false }: { welcome?: boolean }) {
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [avatarIndex, setAvatarIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [resending, setResending] = useState(false);
  const [verificationMessage, setVerificationMessage] = useState("");
  const [logoutPending, setLogoutPending] = useState(false);
  const sessionRequestInFlight = useRef(false);

  const loadSession = useCallback(async () => {
    if (sessionRequestInFlight.current) return;
    sessionRequestInFlight.current = true;
    setLoading(true);
    setLoadError("");
    try {
      const response = await fetch("/api/auth/session", { cache: "no-store" });
      if (response.status === 401 || response.status === 403 || response.status === 404) {
        router.replace(`/login?next=${encodeURIComponent("/home")}`);
        return;
      }
      const result = (await response.json().catch(() => ({}))) as SessionResponse;
      if (!response.ok || !result.user) {
        setLoadError("We couldn’t load your account. Please try again.");
        return;
      }
      setAvatarIndex(getOrCreateProfileAvatar(result.user.id));
      setUser(result.user);
    } catch {
      setLoadError("Connection unavailable. Please try again shortly.");
    } finally {
      sessionRequestInFlight.current = false;
      setLoading(false);
    }
  }, [router]);

  useEffect(() => {
    void loadSession();
  }, [loadSession]);

  async function resendVerification() {
    setResending(true);
    setVerificationMessage("");
    try {
      const response = await fetch("/api/auth/resend-verification", { method: "POST" });
      if (response.ok) {
        setVerificationMessage("We’ve sent you a new verification link.");
      } else {
        if (response.status === 401 || response.status === 403) {
          router.replace(`/login?next=${encodeURIComponent("/home")}`);
          return;
        }
        const result = (await response.json().catch(() => ({}))) as SessionResponse;
        setVerificationMessage(
          result.code === "AUTH_EMAIL_ALREADY_VERIFIED"
            ? "This email address is already verified."
            : "We couldn’t send the link. Please try again.",
        );
      }
    } catch {
      setVerificationMessage("Connection unavailable. Please try again shortly.");
    } finally {
      setResending(false);
    }
  }

  async function logout() {
    setLogoutPending(true);
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {
      // Clear the local session even if the API is temporarily unreachable.
    } finally {
      router.replace("/login");
    }
  }

  if (loading) {
    return (
      <main className={styles.loading} aria-live="polite">
        <span className={styles.loadingMark} aria-hidden="true">✦</span>
        <p>The veil is lifting…</p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className={styles.loadError}>
        <Brand compact href="/home" />
        <h1 className="display-type">We can’t reach your space.</h1>
        <p>{loadError || "Please try again shortly."}</p>
        <button className="button" type="button" onClick={() => void loadSession()}>Try again</button>
      </main>
    );
  }

  return (
    <div className={styles.page}>
      <DashboardHeader
        user={user}
        avatarIndex={avatarIndex}
        logoutPending={logoutPending}
        onLogout={() => void logout()}
      />

      <main className={styles.main}>
        <div className={styles.kicker} id="overview">
          <span className="eyebrow">G01 / Your home</span>
          <span className={styles.kickerRule} aria-hidden="true" />
          <span className={styles.kickerNote}>Your personal space</span>
        </div>

        {welcome && (
          <div className={styles.welcomeNotice} role="status">
            <span aria-hidden="true">✦</span>
            <p>Welcome to Reversky, <strong>{user.username}</strong>. Your story starts now.</p>
            <a href="#sections" aria-label="Go to sections">↓</a>
          </div>
        )}

        {!user.emailVerified && (
          <section className={styles.verifyBanner} aria-label="Email verification">
            <div className={styles.verifyIcon} aria-hidden="true">✉</div>
            <div className={styles.verifyCopy}>
              <strong>Confirm your email address</strong>
              <span>This helps keep your account secure.</span>
              {verificationMessage && <span className={styles.verifyMessage} role="status">{verificationMessage}</span>}
            </div>
            <button className="button button--quiet" type="button" onClick={resendVerification} disabled={resending}>
              {resending ? "Sending…" : "Send a new link"}
            </button>
          </section>
        )}

        <section className={styles.hero}>
          <div className={styles.heroCopy}>
            <p className="eyebrow">Welcome back, {user.username}</p>
            <h1 className="display-type">The night has only just begun.</h1>
            <p>Get ready to collect the stories waiting to be told.</p>
            <a className="button" href="#sections">Explore Reversky <span aria-hidden="true">↓</span></a>
          </div>
          <div className={styles.heroSeal} aria-hidden="true"><span>R</span></div>
          <div className={styles.heroCaption}>Four Theories · One threshold</div>
        </section>

        <section className={styles.sections} id="sections" aria-labelledby="sections-title">
          <div className={styles.sectionHeading}>
            <div>
              <p className="eyebrow">Choose your path</p>
              <h2 id="sections-title" className="display-type">Your world</h2>
            </div>
            <span className={styles.headingNote}>Your journey starts here</span>
          </div>
          <div className={styles.sectionGrid}>
            {sections.map((section) => (
              <article className={styles.sectionCard} key={section.number}>
                <div className={styles.cardTop}>
                  <span>{section.number}</span>
                  <span className={styles.cardStar} aria-hidden="true">✦</span>
                </div>
                <h3 className="display-type">{section.title}</h3>
                <p>{section.text}</p>
                <span className={styles.cardStatus}>Coming soon</span>
              </article>
            ))}
          </div>
        </section>

        <section className={styles.accountStrip} id="account">
          <div>
            <p className="eyebrow">Your profile</p>
            <h2 className="display-type">{user.username}</h2>
          </div>
          <span className={styles.email}>{user.email}</span>
          <Link className="text-link" href="/">Back to home page</Link>
        </section>
      </main>

      <footer className={styles.footer}>
        <Brand compact href="/home" />
        <span>Where reality meets legend.</span>
      </footer>
      <PortalMobileNav />
    </div>
  );
}
