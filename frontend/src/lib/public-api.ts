import { API_BASE_URL } from "./api";

/**
 * Reads public endpoints from server components.
 *
 * Kept separate from apiFetch on purpose: that one attaches an access token and
 * refreshes on 401, neither of which means anything on the server, where there is
 * no browser memory holding a token and no cookie jar to refresh against.
 */
export async function fetchPublic<T>(path: string): Promise<T | null> {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: { Accept: "application/json" },
      // The catalogue changes whenever an admin edits it, and these pages are
      // already dynamic, so serving a stale tree would just be confusing.
      cache: "no-store",
    });
    if (!response.ok) {
      return null;
    }
    return (await response.json()) as T;
  } catch {
    // The API being down should degrade the page, not crash the whole render.
    return null;
  }
}
