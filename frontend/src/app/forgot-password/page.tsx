"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

export default function ForgotPasswordPage() {
  const { forgotPassword } = useAuth();
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await forgotPassword(email.trim());
      setSent(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not reach the server.");
    } finally {
      setBusy(false);
    }
  }

  if (sent) {
    return (
      <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
        <h1 className="text-2xl font-semibold tracking-tight">Check your email</h1>
        {/*
          Says "if" rather than "we sent". The server answers identically whether
          or not the address exists, so that this page cannot be used to find out
          who has an account. Claiming we sent it would give that away.
        */}
        <p className="mt-3 text-sm text-muted">
          If there is an account for{" "}
          <span className="font-medium text-foreground">{email}</span>, a 6-digit
          reset code is on its way. It expires in 15 minutes.
        </p>
        <button
          type="button"
          onClick={() => router.push(`/reset-password?email=${encodeURIComponent(email.trim())}`)}
          className="mt-6 rounded-md bg-accent px-4 py-2 text-sm font-medium text-background"
        >
          I have the code
        </button>
        <p className="mt-4 text-xs text-muted">
          Nothing arrived? Check spam, or{" "}
          <button
            type="button"
            onClick={() => setSent(false)}
            className="text-accent underline underline-offset-4"
          >
            try a different address
          </button>
          .
        </p>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
      <h1 className="text-2xl font-semibold tracking-tight">Forgot your password</h1>
      <p className="mt-2 text-sm text-muted">
        Enter the address you signed up with and we will send a code to reset it.
      </p>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="email" className="text-sm font-medium">
            Email
          </label>
          <input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
        </div>

        {error && (
          <p role="alert" className="text-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={busy || email.trim() === ""}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Sending…" : "Send reset code"}
        </button>
      </form>

      <p className="mt-6 text-sm text-muted">
        Signed up with Google? Your password lives with Google, so{" "}
        <Link href="/login" className="text-accent underline underline-offset-4">
          sign in with Google
        </Link>{" "}
        instead.
      </p>

      <Link
        href="/login"
        className="mt-6 inline-block text-sm text-muted underline underline-offset-4"
      >
        Back to sign in
      </Link>
    </main>
  );
}
