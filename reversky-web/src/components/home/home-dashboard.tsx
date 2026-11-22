"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Brand } from "@/components/brand-mark";
import { DashboardHeader } from "@/components/home/dashboard-header";
import { PortalMobileNav } from "@/components/home/portal-mobile-nav";
import type { UserProfile } from "@/lib/api/contracts";
import { fetchSession, type SessionLoad } from "@/lib/api/session-client";
import { getOrCreateProfileAvatar } from "@/lib/profile-avatar";
import styles from "./home-dashboard.module.css";

type ProblemResponse = { message?: string; code?: string };

const sections = [
  { number: "01", title: "Catalog", text: "Discover the cards and stories that cross the veil.", href: "/catalog" },
  { number: "02", title: "Collection", text: "Collect cards and choose which ones to take with you.", href: "/collection" },
  { number: "03", title: "Decks", text: "Build a strategy around your favorite cards.", href: "/decks" },
  { number: "04", title: "Challenges", text: "Meet other players at the Reversky table.", href: "/battles" },
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
  const [attempt, setAttempt] = useState(0);

  const applySession = useCallback((result: SessionLoad) => {
    if (result.kind === "signed-out") {
      router.replace(`/login?next=${encodeURIComponent("/home")}`);
      return;
    }
    if (result.kind === "error") setLoadError(result.message);
    else {
      setAvatarIndex(getOrCreateProfileAvatar(result.user.id));
      setUser(result.user);
    }
    setLoading(false);
  }, [router]);

  useEffect(() => {
    let active = true;
    void fetchSession().then((result) => {
      if (active) applySession(result);
    });
    return () => {
      active = false;
    };
  }, [attempt, applySession]);

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
        const result = (await response.json().catch(() => ({}))) as ProblemResponse;
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
        <button className="button" type="button" onClick={() => {
          setLoading(true);
          setLoadError("");
          setAttempt((value) => value + 1);
        }}>Try again</button>
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
            {sections.map((section) => {
              const content = (
                <>
                  <div className={styles.cardTop}>
                    <span>{section.number}</span>
                    <span className={styles.cardStar} aria-hidden="true">✦</span>
                  </div>
                  <h3 className="display-type">{section.title}</h3>
                  <p>{section.text}</p>
                  <span className={styles.cardStatus}>{section.href ? <>Open <span aria-hidden="true">→</span></> : "Coming soon"}</span>
                </>
              );
              return section.href ? (
                <Link className={`${styles.sectionCard} ${styles.sectionLink}`} href={section.href} key={section.number}>{content}</Link>
              ) : (
                <article className={styles.sectionCard} key={section.number}>{content}</article>
              );
            })}
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
