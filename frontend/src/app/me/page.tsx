"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";

/**
 * Guarded on the client, not in middleware. The refresh cookie is set by the API
 * on its own origin and scoped to /api/v1/auth, so the Next server never receives
 * it and cannot tell a signed-in visitor from an anonymous one.
 */
export default function ProfilePage() {
  const { user, status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "anonymous") {
      router.replace("/login");
    }
  }, [status, router]);

  if (status !== "authenticated" || !user) {
    return <main className="flex-1 px-6 py-16 text-sm text-muted">Loading your profile…</main>;
  }

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
      <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">Your account</p>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">{user.name}</h1>

      <dl className="mt-8 grid grid-cols-1 gap-x-8 gap-y-3 sm:grid-cols-[auto_1fr]">
        <Row label="Email" value={user.email} />
        <Row label="Role" value={user.role} />
        <Row label="Signed up with" value={user.provider === "GOOGLE" ? "Google" : "Email and password"} />
        <Row label="Member since" value={new Date(user.createdAt).toLocaleDateString()} />
      </dl>

      <div className="mt-10 flex flex-col gap-2">
        <a href="/me/paths" className="text-sm text-accent underline underline-offset-4">
          Your learning paths
        </a>
        <a href="/me/attempts" className="text-sm text-accent underline underline-offset-4">
          Your quiz attempts
        </a>
      </div>
    </main>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <>
      <dt className="font-mono text-xs uppercase tracking-[0.1em] text-muted sm:pt-0.5">{label}</dt>
      <dd className="font-mono text-sm break-all">{value}</dd>
    </>
  );
}
