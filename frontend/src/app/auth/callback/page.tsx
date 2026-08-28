"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/lib/auth-context";

/**
 * Landing point after Google sign-in.
 *
 * The backend redirects here with the access token in the URL *fragment*
 * (#token=...). Fragments are never sent to servers, so the token stays out of
 * access logs, proxy logs and Referer headers — unlike a query string.
 */
export default function OAuthCallbackPage() {
  const { adoptToken } = useAuth();
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const handled = useRef(false);

  useEffect(() => {
    // React runs effects twice in development; adopting the token twice would
    // fire a second /users/me for no reason.
    if (handled.current) return;
    handled.current = true;

    const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ""));
    const token = fragment.get("token");

    if (!token) {
      setError("That sign-in link was missing its token.");
      return;
    }

    // Scrub the token from the address bar so it is not left in history or
    // shoulder-surfed from the URL.
    window.history.replaceState(null, "", window.location.pathname);

    adoptToken(token)
      .then(() => router.replace("/me"))
      .catch(() => setError("That sign-in link is no longer valid. Please try again."));
  }, [adoptToken, router]);

  return (
    <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
      {error ? (
        <>
          <h1 className="text-xl font-semibold">Sign-in failed</h1>
          <p className="mt-2 text-sm text-danger">{error}</p>
          <a href="/login" className="mt-6 inline-block text-sm text-accent underline underline-offset-4">
            Back to sign in
          </a>
        </>
      ) : (
        <p className="text-sm text-muted">Completing sign-in…</p>
      )}
    </main>
  );
}
