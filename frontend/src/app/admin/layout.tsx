"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";

const TABS = [
  { href: "/admin/materials", label: "Material" },
  { href: "/admin/exams", label: "Exams" },
  { href: "/admin/topics", label: "Topics" },
];

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  const { status, isAdmin } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "anonymous") {
      router.replace("/login");
    }
  }, [status, router]);

  if (status === "loading") {
    return <main className="flex-1 px-6 py-16 text-sm text-muted">Checking your access…</main>;
  }

  // Signed in but not an admin. This is a real 403, not a prompt to sign in again,
  // so say so plainly rather than bouncing them to a login form they already passed.
  if (status === "authenticated" && !isAdmin) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
        <h1 className="text-2xl font-semibold tracking-tight">Admins only</h1>
        <p className="mt-3 text-muted">
          Your account does not have admin access, so this area is hidden. If that
          looks wrong, ask whoever runs the site to add your email to the admin list.
        </p>
        <Link href="/" className="mt-6 inline-block text-sm text-accent underline underline-offset-4">
          Back to the site
        </Link>
      </main>
    );
  }

  if (status !== "authenticated") {
    return <main className="flex-1 px-6 py-16 text-sm text-muted">Redirecting…</main>;
  }

  return (
    <div className="mx-auto w-full max-w-4xl flex-1 px-6 py-12">
      <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">Admin</p>
      <nav className="mt-4 flex gap-1 border-b border-line">
        {TABS.map((tab) => {
          const active = pathname.startsWith(tab.href);
          return (
            <Link
              key={tab.href}
              href={tab.href}
              className={`-mb-px border-b-2 px-3 py-2 text-sm ${
                active
                  ? "border-accent font-medium text-foreground"
                  : "border-transparent text-muted hover:text-foreground"
              }`}
            >
              {tab.label}
            </Link>
          );
        })}
      </nav>
      <div className="pt-8">{children}</div>
    </div>
  );
}
