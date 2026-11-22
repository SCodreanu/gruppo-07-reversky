import { NextRequest, NextResponse } from "next/server";
import { apiRequest, type ApiRequestOptions } from "@/lib/api/client";
import { callWithSession, clearSessionCookies, setSessionCookies } from "@/lib/api/session";

type Method = NonNullable<ApiRequestOptions["method"]>;
type AllowedRoute = { method: Method; pattern: RegExp; anonymous?: boolean };

/**
 * Spring endpoints the browser may reach through this BFF route, with the token from the HttpOnly cookies.
 * Anything not listed here answers 404 without calling the API.
 */
const ALLOWED_ROUTES: AllowedRoute[] = [
  // Catalog (public: sent with the token when there is a session)
  { method: "GET", pattern: /^cards$/, anonymous: true },
  { method: "GET", pattern: /^cards\/\d+$/, anonymous: true },
  { method: "GET", pattern: /^factions$/, anonymous: true },
  { method: "GET", pattern: /^rarities$/, anonymous: true },
  { method: "GET", pattern: /^rules$/, anonymous: true },
  { method: "GET", pattern: /^packs$/, anonymous: true },
  // Shop and inventory
  { method: "GET", pattern: /^inventory$/ },
  { method: "POST", pattern: /^packs\/\d+\/purchase$/ },
  { method: "POST", pattern: /^inventory\/packs\/\d+\/open$/ },
  // Collection
  { method: "GET", pattern: /^collection$/ },
  { method: "GET", pattern: /^collection\/summary$/ },
  // Decks
  { method: "GET", pattern: /^decks$/ },
  { method: "GET", pattern: /^decks\/rules$/ },
  { method: "GET", pattern: /^decks\/\d+$/ },
  { method: "POST", pattern: /^decks$/ },
  { method: "POST", pattern: /^decks\/validate$/ },
  { method: "PUT", pattern: /^decks\/\d+$/ },
  { method: "DELETE", pattern: /^decks\/\d+$/ },
  // Friends, challenges, matchmaking and battle history
  { method: "GET", pattern: /^friends$/ },
  { method: "GET", pattern: /^rankings\/me$/ },
  { method: "POST", pattern: /^battle-invites$/ },
  { method: "GET", pattern: /^battle-invites$/ },
  { method: "POST", pattern: /^battle-invites\/[0-9a-fA-F-]{36}\/accept$/ },
  { method: "POST", pattern: /^battle-invites\/[0-9a-fA-F-]{36}\/reject$/ },
  { method: "DELETE", pattern: /^battle-invites\/[0-9a-fA-F-]{36}$/ },
  { method: "POST", pattern: /^matchmaking\/queue$/ },
  { method: "GET", pattern: /^matchmaking\/queue$/ },
  { method: "DELETE", pattern: /^matchmaking\/queue$/ },
  { method: "GET", pattern: /^battles$/ },
  { method: "GET", pattern: /^battles\/[0-9a-fA-F-]{36}$/ },
  { method: "POST", pattern: /^battles\/[0-9a-fA-F-]{36}\/retry$/ },
];

function noStore(response: NextResponse): NextResponse {
  response.headers.set("Cache-Control", "private, no-store, max-age=0");
  return response;
}

function problem(status: number, code: string, detail: string): NextResponse {
  return noStore(NextResponse.json({ status, code, detail }, { status }));
}

/** Mutations must come from this site: SameSite=Lax already blocks most cross-site posts, this closes the rest. */
function isCrossSite(request: NextRequest): boolean {
  const origin = request.headers.get("origin");
  if (origin === null) return false;
  // Compare with the Host the browser used: behind Docker, nextUrl.origin is the bind address (0.0.0.0).
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host");
  try {
    return new URL(origin).host !== host;
  } catch {
    return true;
  }
}

async function readBody(request: NextRequest): Promise<{ ok: true; value: unknown } | { ok: false }> {
  const text = await request.text();
  if (!text) return { ok: true, value: undefined };
  try {
    return { ok: true, value: JSON.parse(text) as unknown };
  } catch {
    return { ok: false };
  }
}

async function proxy(request: NextRequest, method: Method, segments: string[]): Promise<NextResponse> {
  const path = segments.map(encodeURIComponent).join("/");
  const route = ALLOWED_ROUTES.find((entry) => entry.method === method && entry.pattern.test(path));
  if (!route) return problem(404, "NOT_FOUND", "Path not found.");
  if (method !== "GET" && isCrossSite(request)) return problem(403, "CROSS_SITE_REQUEST", "Request refused.");

  let body: unknown;
  if (method === "POST" || method === "PUT") {
    const parsed = await readBody(request);
    if (!parsed.ok) return problem(400, "INVALID_JSON", "The request body is not valid JSON.");
    body = parsed.value;
  }

  const target = `/${path}${request.nextUrl.search}`;
  const { result, refreshedTokens, sessionEnded } = await callWithSession(
    request,
    (accessToken) => apiRequest(target, { method, body, accessToken }),
    route.anonymous,
  );

  const response = result.body === null
    ? new NextResponse(null, { status: result.status })
    : NextResponse.json(result.body, { status: result.status });
  if (refreshedTokens) setSessionCookies(response, refreshedTokens);
  else if (sessionEnded) clearSessionCookies(response);
  return noStore(response);
}

type Context = { params: Promise<{ path: string[] }> };

export async function GET(request: NextRequest, context: Context) {
  return proxy(request, "GET", (await context.params).path);
}

export async function POST(request: NextRequest, context: Context) {
  return proxy(request, "POST", (await context.params).path);
}

export async function PUT(request: NextRequest, context: Context) {
  return proxy(request, "PUT", (await context.params).path);
}

export async function DELETE(request: NextRequest, context: Context) {
  return proxy(request, "DELETE", (await context.params).path);
}
