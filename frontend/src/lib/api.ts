import type { ApiErrorBody } from "./types";

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

/**
 * Thin wrapper over fetch that unwraps the API's JSON error shape.
 *
 * Phase 2 extends this with the access token and a single retry through
 * /api/v1/auth/refresh on a 401. It stays deliberately small until then.
 */
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...init.headers,
    },
    // Sends the refresh-token cookie once phase 2 introduces it.
    credentials: "include",
  });

  if (!response.ok) {
    let body: ApiErrorBody | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      // A non-JSON error body means the request never reached the application —
      // a proxy or platform error page. Fall through to the status-only message.
    }
    throw new ApiError(
      response.status,
      body?.message ?? `Request to ${path} failed with ${response.status}`,
      body,
    );
  }

  return (await response.json()) as T;
}
