"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { CodeInput } from "@/components/code-input";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

function VerifyForm() {
  const { verifyEmail, resendVerification } = useAuth();
  const router = useRouter();
  const params = useSearchParams();

  const [email, setEmail] = useState(params.get("email") ?? "");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Resending is throttled in the UI as well as the server, so the button does
  // not invite someone to hammer an endpoint that will start refusing them.
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((n) => n - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  async function handleVerify(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await verifyEmail(email.trim(), code);
      router.replace("/me");
    } catch (caught) {
      setError(
        caught instanceof ApiError ? caught.message : "Could not reach the server.",
      );
      setBusy(false);
    }
  }

  async function handleResend() {
    setBusy(true);
    setError(null);
    try {
      await resendVerification(email.trim());
      // Deliberately vague: the server does not say whether the address exists,
      // and neither should this.
      setNotice("If that address needs verifying, a new code is on its way.");
      setCooldown(45);
      setCode("");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not reach the server.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
      <h1 className="text-2xl font-semibold tracking-tight">Check your email</h1>
      <p className="mt-2 text-sm text-muted">
        We sent a 6-digit code to{" "}
        <span className="font-medium text-foreground">{email || "your address"}</span>.
        Enter it below to finish signing up.
      </p>

      <form onSubmit={handleVerify} className="mt-8 flex flex-col gap-4">
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
            Verification code
          </label>
          <CodeInput value={code} onChange={setCode} disabled={busy} />
        </div>

        {error && (
          <p role="alert" className="text-sm text-danger">
            {error}
          </p>
        )}
        {notice && <p className="text-sm text-accent">{notice}</p>}

        <button
          type="submit"
          disabled={busy || code.length !== 6 || email.trim() === ""}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Checking…" : "Verify and continue"}
        </button>
      </form>

      <div className="mt-6 flex flex-col gap-2 border-t border-line pt-6 text-sm">
        <button
          type="button"
          onClick={handleResend}
          disabled={busy || cooldown > 0 || email.trim() === ""}
          className="self-start text-accent underline underline-offset-4 disabled:text-muted disabled:no-underline"
        >
          {cooldown > 0 ? `Send another code in ${cooldown}s` : "Send another code"}
        </button>
        <p className="text-xs text-muted">
          The code expires after 15 minutes. Check your spam folder before asking
          for another.
        </p>
        <Link href="/login" className="mt-2 self-start text-muted underline underline-offset-4">
          Back to sign in
        </Link>
      </div>
    </main>
  );
}

export default function VerifyPage() {
  return (
    <Suspense
      fallback={
        <main className="mx-auto w-full max-w-sm flex-1 px-6 py-16">
          <h1 className="text-2xl font-semibold tracking-tight">Check your email</h1>
          <p className="mt-2 text-sm text-muted">We sent a 6-digit code to your address.</p>
          <div className="mt-8 h-32 animate-pulse rounded-md bg-surface" />
        </main>
      }
    >
      <VerifyForm />
    </Suspense>
  );
}
