import type { UserProfile } from "./contracts";

export type SessionLoad =
  | { kind: "signed-in"; user: UserProfile }
  | { kind: "signed-out" }
  | { kind: "error"; message: string };

/** Reads the signed-in user from the BFF. Never throws. */
export async function fetchSession(): Promise<SessionLoad> {
  try {
    const response = await fetch("/api/auth/session", { cache: "no-store" });
    if (response.status === 401 || response.status === 403 || response.status === 404) return { kind: "signed-out" };
    const result = (await response.json().catch(() => ({}))) as { user?: UserProfile };
    if (!response.ok || !result.user) return { kind: "error", message: "We couldn’t load your account. Please try again." };
    return { kind: "signed-in", user: result.user };
  } catch {
    return { kind: "error", message: "Connection unavailable. Please try again shortly." };
  }
}
