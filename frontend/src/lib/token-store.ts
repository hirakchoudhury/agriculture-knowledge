/**
 * The access token lives in a module variable, not localStorage.
 *
 * Anything written to localStorage is readable by any script on the page, so a
 * single stored-XSS bug — in a comment, say — would hand out sessions. Memory is
 * cleared on reload instead, and the refresh cookie silently restores the session.
 */

let accessToken: string | null = null;

type Listener = (token: string | null) => void;
const listeners = new Set<Listener>();

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  listeners.forEach((listener) => listener(token));
}

/** Lets the auth context react when the API layer drops an expired session. */
export function onAccessTokenChange(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
