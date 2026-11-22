import type { Metadata } from "next";
import { AuthForm } from "@/components/auth/auth-form";
import { AuthPage } from "@/components/auth/auth-page";

export const metadata: Metadata = { title: "Create account" };

export default function RegisterPage() {
  return (
    <AuthPage
      active="register"
      eyebrow="P01 / Create account"
      title="The veil is waiting."
      description="Choose a name, build your first collection, and find out what waits beyond the Reverse Rain."
    >
      <AuthForm mode="register" />
    </AuthPage>
  );
}
