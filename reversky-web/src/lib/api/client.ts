import "server-only";
import type { ApiResult, SpringProblemDetail } from "./contracts";

const apiBase = (process.env.REVERSKY_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

export type ApiRequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  accessToken?: string;
  headers?: HeadersInit;
};

/** Server-side Spring API request helper. Browser code should call the Next BFF routes. */
export async function apiRequest<T = unknown>(path: string, options: ApiRequestOptions = {}): Promise<ApiResult<T>> {
  const method = options.method ?? "GET";
  const headers = new Headers(options.headers);
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (options.accessToken) headers.set("Authorization", `Bearer ${options.accessToken}`);

  try {
    const response = await fetch(`${apiBase}/api/v1${path.startsWith("/") ? path : `/${path}`}`, {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      cache: "no-store",
    });
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    return { ok: response.ok, status: response.status, body: body as T | SpringProblemDetail | null };
  } catch {
    const problem: SpringProblemDetail = {
      code: "API_UNAVAILABLE",
      title: "Service unavailable",
      detail: "The service is unavailable. Please try again shortly.",
    };
    return { ok: false, status: 503, body: problem };
  }
}
