"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { CodeInput } from "@/components/code-input";
import { PasswordRules } from "@/components/password-rules";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

function ResetForm() {
  const { resetPassword } = useAuth();
  const router = useRouter();
  const params = useSearchParams();

  const [email, setEmail] = useState(params.get("email") ?? "");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await resetPassword(email.trim(), code, password);
      setDone(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not reach the server.");
      setBusy(false);
    }
  }

  if (done) {
    return (
      <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
        <h1 className="text-2xl font-semibold tracking-tight">Password changed</h1>
        <p className="mt-3 text-sm text-muted">
          Every device that was signed in has been signed out, including this one.
          That is deliberate: if someone else had access, they no longer do.
        </p>
        <button
          type="button"
          onClick={() => router.replace("/login")}
          className="mt-6 rounded-md bg-accent px-4 py-2 text-sm font-medium text-background"
        >
          Sign in with your new password
        </button>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
      <h1 className="text-2xl font-semibold tracking-tight">Set a new password</h1>
      <p className="mt-2 text-sm text-muted">
        Enter the code from your email and choose a new password.
      </p>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        {!params.get("email") && (
          <div className="flex flex-col gap-1.5">
            <label htmlFor="email" className="text-sm font-medium">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
            />
          </div>
        )}

        <div className="flex flex-col gap-1.5">
          <label htmlFor="code" className="text-sm font-medium">
            Reset code
          </label>
          <CodeInput value={code} onChange={setCode} disabled={busy} />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-sm font-medium">
            New password
          </label>
          <input
            id="password"
            type="password"
            required
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
          <PasswordRules password={password} />
        </div>

        {error && (
          <p role="alert" className="text-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={busy || code.length !== 6 || email.trim() === ""}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Changing…" : "Change password"}
        </button>
      </form>

      <Link
        href="/forgot-password"
        className="mt-6 inline-block text-sm text-muted underline underline-offset-4"
      >
        Need a new code?
      </Link>
    </main>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<main className="flex-1 px-6 py-16 text-sm text-muted">Loading…</main>}>
      <ResetForm />
    </Suspense>
  );
}
