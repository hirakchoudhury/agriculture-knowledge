"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Field } from "@/components/field";
import { PasswordRules } from "@/components/password-rules";
import { ApiError, googleSignInUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await register(email, password, name);
      // No session yet: the account is unusable until the emailed code is entered.
      router.push(`/verify?email=${encodeURIComponent(email.trim())}`);
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
      <h1 className="text-2xl font-semibold tracking-tight">Create your account</h1>
      <p className="mt-2 text-sm text-muted">
        Already have one?{" "}
        <Link href="/login" className="text-accent underline underline-offset-4">
          Sign in
        </Link>
      </p>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        <Field id="name" label="Name" value={name} onChange={setName} autoComplete="name" />
        <Field
          id="email"
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="email"
        />
        <div>
          <Field
            id="password"
            label="Password"
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="new-password"
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
          disabled={submitting}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {submitting ? "Creating account…" : "Create account"}
        </button>
      </form>

      <div className="my-6 flex items-center gap-3 text-xs text-muted">
        <span className="h-px flex-1 bg-line" />
        or
        <span className="h-px flex-1 bg-line" />
      </div>

      <a
        href={googleSignInUrl}
        className="block rounded-md border border-line bg-surface px-4 py-2 text-center text-sm font-medium hover:border-accent"
      >
        Continue with Google
      </a>
    </main>
  );
}
