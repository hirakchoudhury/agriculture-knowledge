"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth-context";

/**
 * The homepage's call to action.
 *
 * A client component because the page around it is server-rendered and knows
 * nothing about the session, and "Get started" pointing at the sign-up form is
 * wrong for someone already signed in.
 *
 * It renders while the session is still loading rather than waiting: almost
 * everyone who reaches the homepage is signed out, so showing it immediately is
 * right for the common case, and only an already-signed-in visitor sees it
 * resolve away.
 */
export function HeroCta() {
  const { status } = useAuth();
  const signedIn = status === "authenticated";

  return (
    <div className="mt-8 flex flex-wrap gap-3">
      {!signedIn && (
        <Link
          href="/register"
          className="btn-grad rounded-full px-7 py-3 text-sm font-semibold"
        >
          Get started
        </Link>
      )}

      <Link
        href="/materials"
        className={
          signedIn
            ? "btn-grad rounded-full px-7 py-3 text-sm font-semibold"
            : "rounded-full border border-line bg-surface px-7 py-3 text-sm font-semibold transition-colors hover:border-accent"
        }
      >
        Browse the library
      </Link>

      <Link
        href="/materials?type=QUIZ"
        className="rounded-full border border-line bg-surface px-7 py-3 text-sm font-semibold transition-colors hover:border-accent"
      >
        Practice quizzes
      </Link>
    </div>
  );
}
