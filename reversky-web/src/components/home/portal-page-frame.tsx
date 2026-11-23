"use client";

import type { ReactNode } from "react";
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { Brand } from "@/components/brand-mark";
import { DashboardHeader } from "@/components/home/dashboard-header";
import { PortalMobileNav } from "@/components/home/portal-mobile-nav";
import type { UserProfile } from "@/lib/api/contracts";
import { fetchSession, type SessionLoad } from "@/lib/api/session-client";
import { getOrCreateProfileAvatar, saveProfileAvatar } from "@/lib/profile-avatar";
import styles from "./portal-page-frame.module.css";

type PortalProfileContextValue = {
  user: UserProfile;
  avatarIndex: number;
  chooseAvatar: (index: number) => void;
};

const PortalProfileContext = createContext<PortalProfileContextValue | null>(null);

export function usePortalProfile(): PortalProfileContextValue {
  const value = useContext(PortalProfileContext);
  if (!value) throw new Error("usePortalProfile must be used inside PortalPageFrame");
  return value;
}

export function PortalPageFrame({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow: string;
  /** Without a title the page has no heading block (the content brings its own, as in the result screen). */
  title?: string;
  description?: string;
  children: ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [avatarIndex, setAvatarIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [logoutPending, setLogoutPending] = useState(false);
  const [attempt, setAttempt] = useState(0);

  const applySession = useCallback((result: SessionLoad) => {
    if (result.kind === "signed-out") {
      router.replace(`/login?next=${encodeURIComponent(pathname || "/account")}`);
      return;
    }
    if (result.kind === "error") setLoadError(result.message);
    else {
      setAvatarIndex(getOrCreateProfileAvatar(result.user.id));
      setUser(result.user);
    }
    setLoading(false);
  }, [pathname, router]);

  useEffect(() => {
    let active = true;
    void fetchSession().then((result) => {
      if (active) applySession(result);
    });
    return () => {
      active = false;
    };
  }, [attempt, applySession]);

  const chooseAvatar = useCallback((index: number) => {
    if (!user) return;
    setAvatarIndex(saveProfileAvatar(user.id, index));
  }, [user]);

  const contextValue = useMemo(
    () => user ? { user, avatarIndex, chooseAvatar } : null,
    [user, avatarIndex, chooseAvatar],
  );

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
    return <main className={styles.state} aria-live="polite"><span className={styles.stateMark}>✦</span><p>Opening your account…</p></main>;
  }

  if (!user || !contextValue) {
    return (
      <main className={styles.state}>
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
    <PortalProfileContext.Provider value={contextValue}>
      <div className={styles.page}>
        <DashboardHeader
          user={user}
          avatarIndex={avatarIndex}
          logoutPending={logoutPending}
          onLogout={() => void logout()}
        />
        <main className={styles.main}>
          <a className={styles.backLink} href="/home">← Back to your home</a>
          {title && <header className={styles.heading}>
            <p className="eyebrow">{eyebrow}</p>
            <h1 className="display-type">{title}</h1>
            {description && <p>{description}</p>}
          </header>}
          <div className={styles.content}>{children}</div>
        </main>
        <footer className={styles.footer}>
          <Brand compact href="/home" />
          <span>Where reality meets legend.</span>
        </footer>
        <PortalMobileNav />
      </div>
    </PortalProfileContext.Provider>
  );
}
