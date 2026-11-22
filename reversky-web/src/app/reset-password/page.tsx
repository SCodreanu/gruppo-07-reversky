import type { Metadata } from "next";
import { AuthForm } from "@/components/auth/auth-form";
import { AuthPage } from "@/components/auth/auth-page";

export const metadata: Metadata = { title: "New password" };

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token = "" } = await searchParams;

  return (
    <AuthPage
      eyebrow="P03 / New password"
      title="A new key."
      description="Set a new password for your Reversky account."
    >
      <AuthForm mode="reset" token={token} />
    </AuthPage>
  );
}
