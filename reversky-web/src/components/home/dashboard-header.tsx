"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Brand } from "@/components/brand-mark";
import type { UserProfile } from "@/lib/api/contracts";
import { ProfileAvatar } from "./profile-avatar";
import styles from "./dashboard-header.module.css";

export function DashboardHeader({
  user,
  avatarIndex,
  logoutPending,
  onLogout,
  variant = "dashboard",
}: {
  user: UserProfile;
  avatarIndex: number;
  logoutPending: boolean;
  onLogout: () => void;
  variant?: "dashboard" | "public";
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!menuOpen) return;

    function handlePointerDown(event: PointerEvent) {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) {
        setMenuOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMenuOpen(false);
        triggerRef.current?.focus();
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [menuOpen]);

  return (
    <header className={`${styles.header} ${variant === "public" ? styles.publicVariant : ""}`}>
      <Brand compact href={variant === "public" ? "/" : "/home"} />
      <nav className={styles.nav} aria-label={variant === "public" ? "Public navigation" : "Home navigation"}>
        {variant === "public" ? (
          <>
            <Link href="/rules">The game</Link>
            <Link href="/lore">The lore</Link>
          </>
        ) : (
          <>
            <Link href="/home#overview">Overview</Link>
            <Link href="/home#sections">Your world</Link>
            <Link href="/guide">Portal guide</Link>
          </>
        )}
      </nav>
      <div className={`${styles.account} ${variant === "public" ? styles.publicAccount : ""}`}>
        {variant === "public" && (
          <Link className={`button ${styles.homeCta}`} href="/home">
            Game home <span aria-hidden="true">→</span>
          </Link>
        )}
        <div className={styles.accountMenu} ref={menuRef}>
          <button
            ref={triggerRef}
            className={styles.trigger}
            type="button"
            aria-label={`Account menu for ${user.username}`}
            aria-expanded={menuOpen}
            aria-controls="account-actions"
            onClick={() => setMenuOpen((open) => !open)}
          >
            <ProfileAvatar index={avatarIndex} className={styles.avatar} />
            <span className={styles.username}>{user.username}</span>
            <span className={styles.chevron} aria-hidden="true">{menuOpen ? "⌃" : "⌄"}</span>
          </button>
          {menuOpen && (
            <div className={styles.menu} id="account-actions">
              <div className={styles.menuIdentity}>
                <ProfileAvatar index={avatarIndex} className={styles.menuAvatar} />
                <span>
                  <strong>{user.username}</strong>
                  <small>{user.email}</small>
                </span>
              </div>
              <div className={styles.menuLinks}>
                <Link href="/account" onClick={() => setMenuOpen(false)}>
                  <span aria-hidden="true">◉</span> Manage account
                </Link>
                <Link href="/guide" onClick={() => setMenuOpen(false)}>
                  <span aria-hidden="true">⌘</span> Portal guide
                </Link>
                <Link href="/rules" onClick={() => setMenuOpen(false)}>
                  <span aria-hidden="true">✦</span> How to play
                </Link>
              </div>
              <button
                className={styles.signOut}
                type="button"
                onClick={() => {
                  setMenuOpen(false);
                  onLogout();
                }}
                disabled={logoutPending}
              >
                <span aria-hidden="true">↗</span>{logoutPending ? "Signing out…" : "Log out"}
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
