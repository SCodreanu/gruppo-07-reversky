import type { Metadata } from "next";
import { AuthForm } from "@/components/auth/auth-form";
import { AuthPage } from "@/components/auth/auth-page";

export const metadata: Metadata = { title: "Sign in" };

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string; registered?: string }>;
}) {
  const params = await searchParams;
  const nextUrl = params.next?.startsWith("/") && !params.next.startsWith("//") ? params.next : "/home";

  return (
    <AuthPage
      eyebrow="P02 / Sign in"
      title="The threshold is still open."
      description="Your deck, your stories, and your next encounter are waiting on the other side."
    >
      <AuthForm mode="login" nextUrl={nextUrl} registered={params.registered === "1"} />
    </AuthPage>
  );
}
