"use client";

import type { ReactNode } from "react";
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { Brand } from "@/components/brand-mark";
import { DashboardHeader } from "@/components/home/dashboard-header";
import type { UserProfile } from "@/lib/api/contracts";
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

type SessionResponse = { user?: UserProfile; message?: string };

export function PortalPageFrame({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow: string;
  title: string;
  description: string;
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

  const loadSession = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const response = await fetch("/api/auth/session", { cache: "no-store" });
      if (response.status === 401 || response.status === 403 || response.status === 404) {
        router.replace(`/login?next=${encodeURIComponent(pathname || "/account")}`);
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
      setLoading(false);
    }
  }, [pathname, router]);

  useEffect(() => {
    void loadSession();
  }, [attempt, loadSession]);

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
        <button className="button" type="button" onClick={() => setAttempt((value) => value + 1)}>Try again</button>
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
          <header className={styles.heading}>
            <p className="eyebrow">{eyebrow}</p>
            <h1 className="display-type">{title}</h1>
            <p>{description}</p>
          </header>
          <div className={styles.content}>{children}</div>
        </main>
        <footer className={styles.footer}>
          <Brand compact href="/home" />
          <span>Where reality meets legend.</span>
        </footer>
      </div>
    </PortalProfileContext.Provider>
  );
}
