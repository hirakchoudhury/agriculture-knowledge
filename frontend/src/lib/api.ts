import { getAccessToken, setAccessToken } from "./token-store";
import type { ApiErrorBody, AuthResponse } from "./types";

/**
 * Base URL of the Spring Boot API. Set NEXT_PUBLIC_API_URL in .env.local for
 * local work and in the Vercel project settings for deployments. No trailing slash.
 */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly status: number;
  readonly body?: ApiErrorBody;

  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

/** Endpoints that must never trigger the refresh-and-retry path. */
const AUTH_PATHS = ["/api/v1/auth/refresh", "/api/v1/auth/login", "/api/v1/auth/register"];

/**
 * Only ever one refresh in flight. Without this, a page that fires five requests
 * at once would send five refreshes, and token rotation means four of them would
 * arrive with an already-rotated token and log the user out.
 */
let refreshInFlight: Promise<string | null> | null = null;

async function rawFetch(path: string, init: RequestInit, token: string | null) {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    // Carries the refresh cookie to /api/v1/auth/*.
    credentials: "include",
  });
}

async function parseError(response: Response, path: string): Promise<ApiError> {
  let body: ApiErrorBody | undefined;
  try {
    body = (await response.json()) as ApiErrorBody;
  } catch {
    // A non-JSON body means the request never reached the application — a proxy
    // or platform error page. Fall through to a status-only message.
  }
  return new ApiError(
    response.status,
    body?.message ?? `Request to ${path} failed with ${response.status}`,
    body,
  );
}

/** Exchanges the refresh cookie for a new access token. Returns null if the session is over. */
export function refreshSession(): Promise<string | null> {
  refreshInFlight ??= (async () => {
    try {
      const response = await rawFetch("/api/v1/auth/refresh", { method: "POST" }, null);
      if (!response.ok) {
        setAccessToken(null);
        return null;
      }
      const data = (await response.json()) as AuthResponse;
      setAccessToken(data.accessToken);
      return data.accessToken;
    } catch {
      setAccessToken(null);
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();

  return refreshInFlight;
}

/**
 * Fetch wrapper that attaches the access token and, on a 401, refreshes once and
 * retries. Callers only see the outcome, never the token dance.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await rawFetch(path, init, getAccessToken());

  if (response.status === 401 && !AUTH_PATHS.includes(path)) {
    const token = await refreshSession();
    if (token) {
      response = await rawFetch(path, init, token);
    }
  }

  if (!response.ok) {
    throw await parseError(response, path);
  }

  // 204 No Content has no body to parse.
  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: "POST",
    headers: body === undefined ? {} : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/** Where the browser goes to start Google sign-in. A full navigation, not a fetch. */
export const googleSignInUrl = `${API_BASE_URL}/oauth2/authorization/google`;
