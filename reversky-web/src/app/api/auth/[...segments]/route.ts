import { NextRequest, NextResponse } from "next/server";
import { apiRequest } from "@/lib/api/client";
import type { ApiResult, AuthTokenResponse, UserProfile } from "@/lib/api/contracts";

const ACCESS_COOKIE = "reversky_access";
const REFRESH_COOKIE = "reversky_refresh";
const REFRESH_MAX_AGE = 60 * 60 * 24 * 30;
const refreshRequests = new Map<string, { promise: Promise<ApiResult<AuthTokenResponse>>; expiresAt: number }>();

type JsonObject = Record<string, unknown>;
type FieldViolation = { field: string; message: string };
type Problem = { code?: string; detail?: string; title?: string; errors?: FieldViolation[] };
type TokenSet = AuthTokenResponse;

function callApi<T = unknown>(
  path: string,
  body?: unknown,
  accessToken?: string,
  method: "GET" | "POST" = body === undefined ? "GET" : "POST",
): Promise<ApiResult<T>> {
  return apiRequest<T>(path, { method, body, accessToken });
}

function refreshApiToken(refreshToken: string): Promise<ApiResult<AuthTokenResponse>> {
  const current = refreshRequests.get(refreshToken);
  if (current && current.expiresAt > Date.now()) return current.promise;
  if (current) refreshRequests.delete(refreshToken);

  const request = callApi<AuthTokenResponse>("/auth/refresh", { refreshToken });
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

function problemResponse(result: ApiResult): NextResponse {
  const source = result.body && typeof result.body === "object" ? (result.body as Problem) : {};
  const response = NextResponse.json(
    {
      code: source.code ?? "API_ERROR",
      message: source.detail ?? source.title ?? "We couldn’t complete your request.",
      errors: source.errors ?? [],
    },
    { status: result.status },
  );
  response.headers.set("Cache-Control", "private, no-store, max-age=0");
  return response;
}

function setSessionCookies(response: NextResponse, tokenSet: TokenSet): void {
  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
  };

  response.cookies.set(ACCESS_COOKIE, tokenSet.accessToken, {
    ...cookieOptions,
    maxAge: Math.max(1, tokenSet.expiresIn),
  });
  response.cookies.set(REFRESH_COOKIE, tokenSet.refreshToken, {
    ...cookieOptions,
    maxAge: REFRESH_MAX_AGE,
  });
}

function clearSessionCookies(response: NextResponse): void {
  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
    maxAge: 0,
  };
  response.cookies.set(ACCESS_COOKIE, "", cookieOptions);
  response.cookies.set(REFRESH_COOKIE, "", cookieOptions);
}

function asTokenSet(body: unknown): TokenSet | null {
  if (!body || typeof body !== "object") return null;
  const value = body as Partial<TokenSet>;
  if (
    typeof value.accessToken !== "string" ||
    typeof value.refreshToken !== "string" ||
    typeof value.expiresIn !== "number"
  ) {
    return null;
  }
  return value as TokenSet;
}

async function readJson(request: NextRequest): Promise<JsonObject | null> {
  try {
    const value: unknown = await request.json();
    return value && typeof value === "object" && !Array.isArray(value) ? (value as JsonObject) : null;
  } catch {
    return null;
  }
}

type AuthenticatedProfile = {
  user: UserProfile;
  accessToken: string;
  refreshedTokens: TokenSet | null;
};

async function getAuthenticatedProfile(request: NextRequest): Promise<
  | { ok: true; value: AuthenticatedProfile }
  | { ok: false; result: ApiResult }
> {
  const accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;

  if (accessToken) {
    const profile = await callApi<UserProfile>("/users/me", undefined, accessToken);
    if (profile.ok) {
      return {
        ok: true,
        value: { user: profile.body as UserProfile, accessToken, refreshedTokens: null },
      };
    }
    if (profile.status !== 401) return { ok: false, result: profile };
  }

  if (!refreshToken) {
    return {
      ok: false,
      result: {
        status: 401,
        ok: false,
        body: { code: "AUTH_REQUIRED", detail: "Sign in to continue." },
      },
    };
  }

  const refreshed = await refreshApiToken(refreshToken);
  const tokens = refreshed.ok ? asTokenSet(refreshed.body) : null;
  if (!tokens) {
    return {
      ok: false,
      result: refreshed.ok
        ? {
            status: 502,
            ok: false,
            body: { code: "AUTH_REFRESH_FAILED", detail: "We couldn’t refresh your session." },
          }
        : refreshed,
    };
  }

  const profile = await callApi<UserProfile>("/users/me", undefined, tokens.accessToken);
  if (!profile.ok) return { ok: false, result: profile };
  return {
    ok: true,
    value: { user: profile.body as UserProfile, accessToken: tokens.accessToken, refreshedTokens: tokens },
  };
}

async function handleGet(request: NextRequest, segments: string[]): Promise<NextResponse> {
  const route = segments.join("/");

  if (route === "shop") {
    const auth = await getAuthenticatedProfile(request);
    if (!auth.ok) {
      const response = problemResponse(auth.result);
      if (auth.result.status === 401 || auth.result.status === 403 || auth.result.status === 404) {
        clearSessionCookies(response);
      }
      return response;
    }

    const [packs, inventory] = await Promise.all([
      callApi("/packs", undefined, auth.value.accessToken),
      callApi("/inventory", undefined, auth.value.accessToken),
    ]);
    const response = NextResponse.json({
      packs: packs.ok ? packs.body : [],
      inventory: inventory.ok ? inventory.body : null,
      catalogueUnavailable: !packs.ok,
      inventoryUnavailable: !inventory.ok,
    });
    response.headers.set("Cache-Control", "private, no-store, max-age=0");
    if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
    return response;
  }

  if (route === "wallet") {
    const auth = await getAuthenticatedProfile(request);
    if (!auth.ok) {
      const response = problemResponse(auth.result);
      if (auth.result.status === 401 || auth.result.status === 403 || auth.result.status === 404) {
        clearSessionCookies(response);
      }
      return response;
    }

    const inventory = await callApi("/inventory", undefined, auth.value.accessToken);
    if (!inventory.ok) {
      const response = problemResponse(inventory);
      if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
      return response;
    }

    const response = NextResponse.json(inventory.body);
    response.headers.set("Cache-Control", "private, no-store, max-age=0");
    if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
    return response;
  }

  if (route !== "session") {
    return NextResponse.json({ code: "NOT_FOUND", message: "Path not found." }, { status: 404 });
  }

  const auth = await getAuthenticatedProfile(request);
  if (!auth.ok) {
    const response = problemResponse(auth.result);
    if (auth.result.status === 401 || auth.result.status === 403 || auth.result.status === 404) {
      clearSessionCookies(response);
    }
    return response;
  }

  const response = NextResponse.json({ user: auth.value.user });
  response.headers.set("Cache-Control", "private, no-store, max-age=0");
  if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
  return response;
}

async function handlePost(request: NextRequest, segments: string[]): Promise<NextResponse> {
  const route = segments.join("/");
  const body = await readJson(request);

  if (route === "logout") {
    const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
    const result = refreshToken ? await callApi("/auth/logout", { refreshToken }) : null;
    const response = result && !result.ok ? problemResponse(result) : new NextResponse(null, { status: 204 });
    clearSessionCookies(response);
    return response;
  }

  const shopOpen = route.match(/^shop\/open\/(\d+)$/);
  if (shopOpen) {
    const auth = await getAuthenticatedProfile(request);
    if (!auth.ok) {
      const response = problemResponse(auth.result);
      if (auth.result.status === 401 || auth.result.status === 403 || auth.result.status === 404) {
        clearSessionCookies(response);
      }
      return response;
    }

    const result = await callApi(`/inventory/packs/${shopOpen[1]}/open`, undefined, auth.value.accessToken, "POST");
    const response = result.ok
      ? result.body === null
        ? new NextResponse(null, { status: 204 })
        : NextResponse.json(result.body, { status: result.status })
      : problemResponse(result);
    response.headers.set("Cache-Control", "private, no-store, max-age=0");
    if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
    return response;
  }

  if (!body && route !== "resend-verification") {
    return NextResponse.json(
      { code: "INVALID_JSON", message: "Check the information you entered and try again.", errors: [] },
      { status: 400 },
    );
  }

  const shopPurchase = route.match(/^shop\/purchase\/(\d+)$/);
  if (shopPurchase) {
    const auth = await getAuthenticatedProfile(request);
    if (!auth.ok) {
      const response = problemResponse(auth.result);
      if (auth.result.status === 401 || auth.result.status === 403 || auth.result.status === 404) {
        clearSessionCookies(response);
      }
      return response;
    }

    const result = await callApi(`/packs/${shopPurchase[1]}/purchase`, body, auth.value.accessToken);
    const response = result.ok
      ? result.body === null
        ? new NextResponse(null, { status: 204 })
        : NextResponse.json(result.body, { status: result.status })
      : problemResponse(result);
    response.headers.set("Cache-Control", "private, no-store, max-age=0");
    if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
    return response;
  }

  if (route === "login") {
    const result = await callApi<AuthTokenResponse>("/auth/login", body);
    const tokens = result.ok ? asTokenSet(result.body) : null;
    if (!tokens) {
      return result.ok
        ? NextResponse.json({ code: "AUTH_SESSION_FAILED", message: "Sign in failed. Please try again.", errors: [] }, { status: 502 })
        : problemResponse(result);
    }
    const response = NextResponse.json({ ok: true });
    setSessionCookies(response, tokens);
    return response;
  }

  if (route === "register") {
    const registration = await callApi("/auth/register", body);
    if (!registration.ok) return problemResponse(registration);

    // Registration intentionally has no session response in the API contract. Use the same
    // credentials for a normal login so the first-entry flow can continue to Home.
    const login = await callApi<AuthTokenResponse>("/auth/login", {
      email: body?.email,
      password: body?.password,
    });
    const tokens = login.ok ? asTokenSet(login.body) : null;
    const response = NextResponse.json({ sessionStarted: Boolean(tokens) }, { status: 201 });
    if (tokens) setSessionCookies(response, tokens);
    return response;
  }

  if (route === "forgot-password") return noContentOrProblem(await callApi("/auth/password/forgot", body));
  if (route === "reset-password") {
    const result = await callApi("/auth/password/reset", body);
    const response = noContentOrProblem(result);
    if (result.ok) clearSessionCookies(response);
    return response;
  }
  if (route === "verify-email") return noContentOrProblem(await callApi("/auth/email/verify", body));

  if (route === "resend-verification") {
    const auth = await getAuthenticatedProfile(request);
    if (!auth.ok) {
      const response = problemResponse(auth.result);
      if (auth.result.status === 401 || auth.result.status === 403 || auth.result.status === 404) {
        clearSessionCookies(response);
      }
      return response;
    }

    const result = await callApi("/auth/email/resend", undefined, auth.value.accessToken, "POST");
    if (!result.ok) {
      const response = problemResponse(result);
      if (result.status === 401 || result.status === 403 || result.status === 404) clearSessionCookies(response);
      if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
      return response;
    }

    const response = new NextResponse(null, { status: 204 });
    if (auth.value.refreshedTokens) setSessionCookies(response, auth.value.refreshedTokens);
    return response;
  }

  return NextResponse.json({ code: "NOT_FOUND", message: "Path not found." }, { status: 404 });
}

function noContentOrProblem(result: ApiResult): NextResponse {
  return result.ok ? new NextResponse(null, { status: 204 }) : problemResponse(result);
}

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ segments: string[] }> },
): Promise<NextResponse> {
  const { segments } = await context.params;
  return handleGet(request, segments);
}

export async function POST(
  request: NextRequest,
  context: { params: Promise<{ segments: string[] }> },
): Promise<NextResponse> {
  const { segments } = await context.params;
  return handlePost(request, segments);
}
