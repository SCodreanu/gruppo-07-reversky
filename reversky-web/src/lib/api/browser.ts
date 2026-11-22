import type { SpringProblemDetail } from "./contracts";

/** Result of a call to the game BFF route (`/api/game/...`), which forwards to the Spring API. */
export type GameResult<T> =
  | { ok: true; status: number; data: T }
  | { ok: false; status: number; problem: SpringProblemDetail };

const unavailable: SpringProblemDetail = {
  code: "API_UNAVAILABLE",
  detail: "The service is unavailable. Please try again shortly.",
};

/** Browser-side call to `/api/game/{path}`. Never throws: network errors become an API_UNAVAILABLE problem. */
export async function gameFetch<T>(
  path: string,
  options: { method?: "GET" | "POST" | "PUT" | "DELETE"; body?: unknown; signal?: AbortSignal } = {},
): Promise<GameResult<T>> {
  try {
    const response = await fetch(`/api/game/${path.replace(/^\//, "")}`, {
      method: options.method ?? "GET",
      headers: options.body === undefined ? undefined : { "Content-Type": "application/json" },
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      cache: "no-store",
      signal: options.signal,
    });
    const body: unknown = response.status === 204 ? null : await response.json().catch(() => null);
    if (response.ok) return { ok: true, status: response.status, data: body as T };
    const problem = body && typeof body === "object" ? (body as SpringProblemDetail) : unavailable;
    return { ok: false, status: response.status, problem };
  } catch {
    // Also reached when `signal` aborts: callers check `signal.aborted` before using the result.
    return { ok: false, status: 0, problem: unavailable };
  }
}

/** Builds a query string from the defined, non-empty values. */
export function queryString(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export function problemMessage(problem: SpringProblemDetail, fallback = "We couldn’t complete that request. Please try again."): string {
  return problem.detail ?? problem.title ?? fallback;
}

/** True when the session is gone and the page should send the user to the login. */
export function needsLogin(result: { ok: boolean; status: number }): boolean {
  return !result.ok && result.status === 401;
}
