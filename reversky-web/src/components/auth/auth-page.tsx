import type { ReactNode } from "react";
import { PublicHeader } from "@/components/brand";
import { SiteFooter } from "@/components/site-footer";
import styles from "./auth-page.module.css";

export function AuthPage({
  active = "login",
  eyebrow,
  title,
  description,
  children,
}: {
  active?: "login" | "register";
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <div className={styles.page}>
      <PublicHeader active={active} />
      <main className={styles.main}>
        <section className={styles.story} aria-label="Reversky">
          <div className={styles.storyCopy}>
            <p className="eyebrow">{eyebrow}</p>
            <h1 className={`display-type ${styles.storyTitle}`}>{title}</h1>
            <p className={styles.storyText}>{description}</p>
          </div>
          <div className={styles.storyFoot}>
            <span>Four theories. One threshold.</span>
            <span className={styles.storyFootMark} aria-hidden="true">✦</span>
          </div>
        </section>
        <section className={styles.panelWrap}>
          <div className={styles.panel}>{children}</div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
