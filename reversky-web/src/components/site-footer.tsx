import Link from "next/link";
import { Brand } from "@/components/brand";
import styles from "./site-footer.module.css";

const groups = [
  {
    title: "Discover",
    links: [
      { label: "The game", href: "/rules" },
      { label: "The lore", href: "/lore" },
      { label: "Meet the Theories", href: "/lore#theories" },
    ],
  },
  {
    title: "Your account",
    links: [
      { label: "Sign in", href: "/login" },
      { label: "Create account", href: "/register" },
    ],
  },
  {
    title: "Information",
    links: [
      { label: "Privacy policy", href: "#" },
      { label: "Terms of use", href: "#" },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div className={styles.top}>
          <div className={styles.identity}>
            <Brand compact />
            <p>Where reality meets legend.</p>
          </div>
          {groups.map((group) => (
            <nav className={styles.group} aria-label={group.title} key={group.title}>
              <h2>{group.title}</h2>
              {group.links.map((link) => (
                <Link href={link.href} key={link.label}>{link.label}</Link>
              ))}
            </nav>
          ))}
        </div>
        <div className={styles.bottom}>
          <span>© Reversky</span>
          <span>A digital horror card game.</span>
        </div>
      </div>
    </footer>
  );
}
