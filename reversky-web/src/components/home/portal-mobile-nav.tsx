"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import styles from "./portal-mobile-nav.module.css";

const tabs = [
  { label: "Home", href: "/home", symbol: "⌂" },
  { label: "Cards", href: "/collection", symbol: "✦" },
  { label: "Decks", href: "/decks", symbol: "▤" },
  { label: "Shop", href: "/shop", symbol: "◈" },
  { label: "Account", href: "/account", symbol: "◉" },
];

export function PortalMobileNav() {
  const pathname = usePathname();

  return (
    <nav className={styles.nav} aria-label="Portal navigation">
      {tabs.map((tab) => {
        const active = pathname === tab.href || (tab.href !== "/home" && pathname.startsWith(`${tab.href}/`));
        return (
          <Link
            aria-current={active ? "page" : undefined}
            className={active ? styles.active : undefined}
            href={tab.href}
            key={tab.href}
          >
            <span aria-hidden="true">{tab.symbol}</span>
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
