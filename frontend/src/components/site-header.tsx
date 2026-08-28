"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";

export function SiteHeader() {
  const { user, status, isAdmin, logout } = useAuth();
  const router = useRouter();

  async function handleSignOut() {
    await logout();
    router.push("/");
  }

  return (
    <header className="border-b border-line">
      <nav className="mx-auto flex w-full max-w-4xl flex-wrap items-center justify-between gap-4 px-6 py-4">
        <div className="flex items-baseline gap-5">
          <Link href="/" className="font-semibold tracking-tight hover:text-accent">
            Agriculture Knowledge
          </Link>
          <Link href="/exams" className="text-sm text-muted hover:text-foreground">
            Exams
          </Link>
          {isAdmin && (
            <Link href="/admin/exams" className="text-sm text-muted hover:text-foreground">
              Admin
            </Link>
          )}
        </div>

        <div className="flex items-center gap-4 text-sm">
          {status === "loading" && <span className="text-muted">Checking session…</span>}

          {status === "anonymous" && (
            <>
              <Link href="/login" className="text-muted hover:text-foreground">
                Sign in
              </Link>
              <Link
                href="/register"
                className="rounded-md bg-accent px-3 py-1.5 font-medium text-background"
              >
                Create account
              </Link>
            </>
          )}

          {status === "authenticated" && user && (
            <>
              <Link href="/me" className="text-muted hover:text-foreground">
                {user.name}
              </Link>
              <button
                type="button"
                onClick={handleSignOut}
                className="text-muted underline underline-offset-4 hover:text-foreground"
              >
                Sign out
              </button>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
