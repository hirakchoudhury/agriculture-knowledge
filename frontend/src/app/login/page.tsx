"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { Field } from "@/components/field";
import { ApiError, googleSignInUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

/** Messages for the ?error= values the backend redirects with. */
const OAUTH_ERRORS: Record<string, string> = {
  google_sign_in_failed: "Google sign-in did not complete. Please try again.",
  google_email_unverified:
    "That Google account has an unverified email address, so it cannot be used to sign in.",
  google_profile_incomplete:
    "Google did not share an email address for that account.",
};

function LoginForm() {
  const { login } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(
    OAUTH_ERRORS[searchParams.get("error") ?? ""] ?? null,
  );
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(email, password);
      router.push("/me");
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "Could not reach the server. Is the API running?",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
      <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
      <p className="mt-2 text-sm text-muted">
        New here?{" "}
        <Link href="/register" className="text-accent underline underline-offset-4">
          Create an account
        </Link>
      </p>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        <Field
          id="email"
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="email"
        />
        <Field
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
        />

        {error && (
          <p role="alert" className="text-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {submitting ? "Signing in…" : "Sign in"}
        </button>
      </form>

      <div className="my-6 flex items-center gap-3 text-xs text-muted">
        <span className="h-px flex-1 bg-line" />
        or
        <span className="h-px flex-1 bg-line" />
      </div>

      {/*
        A plain link, not a fetch: the browser must follow Google's redirects,
        and an XHR cannot.
      */}
      <a
        href={googleSignInUrl}
        className="block rounded-md border border-line bg-surface px-4 py-2 text-center text-sm font-medium hover:border-accent"
      >
        Continue with Google
      </a>
    </main>
  );
}

export default function LoginPage() {
  // useSearchParams needs a Suspense boundary during prerendering.
  return (
    <Suspense fallback={<main className="flex-1 px-6 py-16 text-muted">Loading…</main>}>
      <LoginForm />
    </Suspense>
  );
}
