import type { ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { PublicHeader } from "@/components/brand";
import { SiteFooter } from "@/components/site-footer";
import styles from "./public-info-page.module.css";

export function PublicInfoPage({
  eyebrow,
  title,
  summary,
  children,
}: {
  eyebrow: string;
  title: string;
  summary: string;
  children: ReactNode;
}) {
  return (
    <div className={styles.page}>
      <PublicHeader active="none" />
      <main className={styles.main}>
        <header className={styles.hero}>
          <Image
            className={styles.banner}
            src="/images/reverse-rain-banner.png"
            alt=""
            fill
            sizes="(max-width: 760px) 100vw, 1360px"
            priority
          />
          <div className={styles.heroCopy}>
            <p className="eyebrow">{eyebrow}</p>
            <h1 className={`display-type ${styles.title}`}>{title}</h1>
            <p className={styles.summary}>{summary}</p>
            <Link className={styles.backLink} href="/">← Back to Reversky</Link>
          </div>
        </header>
        <div className={styles.content}>{children}</div>
      </main>
      <SiteFooter />
    </div>
  );
}
