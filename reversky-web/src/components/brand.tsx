"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { DashboardHeader } from "@/components/home/dashboard-header";
import { Brand } from "@/components/brand-mark";
import type { UserProfile } from "@/lib/api/contracts";
import { getOrCreateProfileAvatar } from "@/lib/profile-avatar";

export { Brand } from "@/components/brand-mark";

export function PublicHeader({ active = "login" }: { active?: "login" | "register" | "none" }) {
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [avatarIndex, setAvatarIndex] = useState(0);
  const [sessionState, setSessionState] = useState<"checking" | "guest" | "signed-in">("checking");
  const [logoutPending, setLogoutPending] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function loadSession() {
      try {
        const response = await fetch("/api/auth/session", { cache: "no-store" });
        const result = (await response.json().catch(() => ({}))) as { user?: UserProfile };
        if (cancelled) return;
        if (response.ok && result.user) {
          setAvatarIndex(getOrCreateProfileAvatar(result.user.id));
          setUser(result.user);
          setSessionState("signed-in");
        } else {
          setSessionState("guest");
        }
      } catch {
        if (!cancelled) setSessionState("guest");
      }
    }

    void loadSession();
    return () => { cancelled = true; };
  }, []);

  async function logout() {
    setLogoutPending(true);
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {
      // The local session is cleared by the BFF response whenever it is reachable.
    } finally {
      router.replace("/login");
    }
  }

  if (sessionState === "checking") {
    return (
      <header className="public-header">
        <Brand />
        <span className="public-header__session-loading" role="status" aria-label="Checking account" />
      </header>
    );
  }

  if (sessionState === "signed-in" && user) {
    return (
      <DashboardHeader
        user={user}
        avatarIndex={avatarIndex}
        logoutPending={logoutPending}
        onLogout={() => void logout()}
        variant="public"
      />
    );
  }

  return (
    <header className="public-header">
      <Brand />
      <nav className="public-header__nav" aria-label="Main navigation">
        <a className={active === "login" ? "public-header__current" : ""} href="/login">
          Sign in
        </a>
        <a className="button public-header__cta" href="/register">
          Create account
        </a>
      </nav>
    </header>
  );
}
