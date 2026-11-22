import type { Metadata } from "next";
import { AuthPage } from "@/components/auth/auth-page";
import { VerificationCard } from "@/components/auth/verification-card";
import styles from "./page.module.css";

export const metadata: Metadata = { title: "Verify email" };

export default async function VerifyEmailPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token = "" } = await searchParams;

  return (
    <AuthPage
      eyebrow="P01 / Email verification"
      title="One last step."
      description="Verify your email address to help keep your account secure."
    >
      <div className={styles.heading}>
        <span className="eyebrow">Verify your account</span>
        <h2 className={`display-type ${styles.title}`}>Confirm your email</h2>
        <p className="muted">Open the link we sent to your email.</p>
      </div>
      <VerificationCard token={token} />
    </AuthPage>
  );
}
