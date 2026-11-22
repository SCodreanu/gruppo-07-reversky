import "server-only";
import type { NextRequest, NextResponse } from "next/server";
import { apiRequest } from "./client";
import type { ApiResult, AuthTokenResponse } from "./contracts";

export const ACCESS_COOKIE = "reversky_access";
export const REFRESH_COOKIE = "reversky_refresh";
const REFRESH_MAX_AGE = 60 * 60 * 24 * 30;
const refreshRequests = new Map<string, { promise: Promise<ApiResult<AuthTokenResponse>>; expiresAt: number }>();

/** Shares one refresh call between concurrent requests that carry the same refresh token. */
export function refreshApiToken(refreshToken: string): Promise<ApiResult<AuthTokenResponse>> {
  const current = refreshRequests.get(refreshToken);
  if (current && current.expiresAt > Date.now()) return current.promise;
  if (current) refreshRequests.delete(refreshToken);

  const request = apiRequest<AuthTokenResponse>("/auth/refresh", { method: "POST", body: { refreshToken } });
  const entry = { promise: request, expiresAt: Number.POSITIVE_INFINITY };
  refreshRequests.set(refreshToken, entry);
  const removeRequest = () => {
    if (refreshRequests.get(refreshToken) === entry) refreshRequests.delete(refreshToken);
  };
  void request.then(
    () => {
      entry.expiresAt = Date.now() + 2000;
      setTimeout(removeRequest, 2000);
    },
    removeRequest,
  );
  return request;
}

export function asTokenSet(body: unknown): AuthTokenResponse | null {
  if (!body || typeof body !== "object") return null;
  const value = body as Partial<AuthTokenResponse>;
  if (
    typeof value.accessToken !== "string" ||
    typeof value.refreshToken !== "string" ||
    typeof value.expiresIn !== "number"
  ) {
    return null;
  }
  return value as AuthTokenResponse;
}

/** Secure in production unless REVERSKY_COOKIE_SECURE says otherwise (e.g. docker compose serves plain http). */
function secureCookies(): boolean {
  const setting = process.env.REVERSKY_COOKIE_SECURE;
  if (setting === "true" || setting === "false") return setting === "true";
  return process.env.NODE_ENV === "production";
}

const baseCookieOptions = () => ({
  httpOnly: true,
  secure: secureCookies(),
  sameSite: "lax" as const,
  path: "/",
});

export function setSessionCookies(response: NextResponse, tokenSet: AuthTokenResponse): void {
  response.cookies.set(ACCESS_COOKIE, tokenSet.accessToken, {
    ...baseCookieOptions(),
    maxAge: Math.max(1, tokenSet.expiresIn),
  });
  response.cookies.set(REFRESH_COOKIE, tokenSet.refreshToken, {
    ...baseCookieOptions(),
    maxAge: REFRESH_MAX_AGE,
  });
}

export function clearSessionCookies(response: NextResponse): void {
  response.cookies.set(ACCESS_COOKIE, "", { ...baseCookieOptions(), maxAge: 0 });
  response.cookies.set(REFRESH_COOKIE, "", { ...baseCookieOptions(), maxAge: 0 });
}

export type SessionCall<T> = {
  result: ApiResult<T>;
  /** New tokens to store on the response, when the access token had to be refreshed. */
  refreshedTokens: AuthTokenResponse | null;
  /** True when the session is gone (no valid refresh token): the response should clear the cookies. */
  sessionEnded: boolean;
};

const authRequired: ApiResult = {
  ok: false,
  status: 401,
  body: { status: 401, code: "AUTH_REQUIRED", title: "Unauthorized", detail: "Sign in to continue." },
};

/**
 * Runs a Spring call with the access token from the cookies. On 401 it refreshes the tokens once and retries.
 * With `allowAnonymous`, a request without cookies is sent without a token (public endpoints).
 */
export async function callWithSession<T>(
  request: NextRequest,
  call: (accessToken: string | undefined) => Promise<ApiResult<T>>,
  allowAnonymous = false,
): Promise<SessionCall<T>> {
  const accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;

  if (accessToken) {
    const result = await call(accessToken);
    if (result.status !== 401) return { result, refreshedTokens: null, sessionEnded: false };
  }

  if (!refreshToken) {
    if (allowAnonymous) {
      return { result: await call(undefined), refreshedTokens: null, sessionEnded: Boolean(accessToken) };
    }
    return { result: authRequired as ApiResult<T>, refreshedTokens: null, sessionEnded: true };
  }

  const refreshed = await refreshApiToken(refreshToken);
  // API down: keep the cookies and report the outage instead of signing the user out.
  if (refreshed.status >= 500) return { result: refreshed as ApiResult<T>, refreshedTokens: null, sessionEnded: false };
  const tokens = refreshed.ok ? asTokenSet(refreshed.body) : null;
  if (!tokens) {
    if (allowAnonymous) return { result: await call(undefined), refreshedTokens: null, sessionEnded: true };
    return { result: authRequired as ApiResult<T>, refreshedTokens: null, sessionEnded: true };
  }
  return { result: await call(tokens.accessToken), refreshedTokens: tokens, sessionEnded: false };
}
