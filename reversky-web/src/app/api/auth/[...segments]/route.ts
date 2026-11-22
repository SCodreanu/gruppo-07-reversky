import { NextRequest, NextResponse } from "next/server";
import { apiRequest } from "@/lib/api/client";
import type { ApiResult, AuthTokenResponse, UserProfile } from "@/lib/api/contracts";
import {
  REFRESH_COOKIE,
  asTokenSet,
  callWithSession,
  clearSessionCookies,
  setSessionCookies,
} from "@/lib/api/session";

type JsonObject = Record<string, unknown>;
type FieldViolation = { field: string; message: string };
type Problem = { code?: string; detail?: string; title?: string; errors?: FieldViolation[] };

function callApi<T = unknown>(
  path: string,
  body?: unknown,
  accessToken?: string,
  method: "GET" | "POST" = body === undefined ? "GET" : "POST",
): Promise<ApiResult<T>> {
  return apiRequest<T>(path, { method, body, accessToken });
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
  refreshedTokens: AuthTokenResponse | null;
};

async function getAuthenticatedProfile(request: NextRequest): Promise<
  | { ok: true; value: AuthenticatedProfile }
  | { ok: false; result: ApiResult }
> {
  let usedToken = "";
  const { result, refreshedTokens } = await callWithSession(request, (accessToken) => {
    usedToken = accessToken ?? "";
    return callApi<UserProfile>("/users/me", undefined, accessToken);
  });
  if (!result.ok) return { ok: false, result };
  return { ok: true, value: { user: result.body as UserProfile, accessToken: usedToken, refreshedTokens } };
}

async function handleGet(request: NextRequest, segments: string[]): Promise<NextResponse> {
  const route = segments.join("/");

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

  if (!body && route !== "resend-verification") {
    return NextResponse.json(
      { code: "INVALID_JSON", message: "Check the information you entered and try again.", errors: [] },
      { status: 400 },
    );
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
