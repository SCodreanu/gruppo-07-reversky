import type { Metadata } from "next";
import { AuthForm } from "@/components/auth/auth-form";
import { AuthPage } from "@/components/auth/auth-page";

export const metadata: Metadata = { title: "Forgot password" };

export default function ForgotPasswordPage() {
  return (
    <AuthPage
      eyebrow="P03 / Account recovery"
      title="Find your way back."
      description="Enter the email address linked to your account. We’ll send instructions to help you get back in the game."
    >
      <AuthForm mode="forgot" />
    </AuthPage>
  );
}
