"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { ThemeToggle } from "@/components/theme-toggle";
import type { ExamSummary } from "@/lib/types";

type MenuName = "exams" | "more";

/**
 * Closes a popover on Escape or on a click outside it.
 *
 * Both are needed: a dropdown that only closes on a second click of its trigger
 * traps the pointer, and one that only closes on outside click traps the keyboard.
 */
function useDismiss(open: boolean, close: () => void) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") close();
    }
    function onPointerDown(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) close();
    }

    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("mousedown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("mousedown", onPointerDown);
    };
  }, [open, close]);

  return ref;
}

export function SiteHeader({ exams }: { exams: ExamSummary[] }) {
  const { user, status, isAdmin, logout } = useAuth();
  const router = useRouter();

  const [openMenu, setOpenMenu] = useState<MenuName | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [query, setQuery] = useState("");

  const closeMenu = useCallback(() => setOpenMenu(null), []);
  const menuRef = useDismiss(openMenu !== null, closeMenu);

  // A drawer that scrolls the page behind it feels broken on a phone.
  useEffect(() => {
    if (!drawerOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setDrawerOpen(false);
    }
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.body.style.overflow = previous;
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [drawerOpen]);

  async function handleSignOut() {
    setDrawerOpen(false);
    closeMenu();
    await logout();
    router.push("/");
  }

  function handleSearch(event: React.FormEvent) {
    event.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    setDrawerOpen(false);
    router.push(`/materials?q=${encodeURIComponent(trimmed)}`);
  }

  const moreLinks = [
    { href: "/materials?type=VIDEO", label: "Video lessons" },
    { href: "/materials?type=ARTICLE", label: "Articles" },
    ...(status === "authenticated"
      ? [
          { href: "/me/paths", label: "My learning paths" },
          { href: "/me/attempts", label: "My quiz attempts" },
        ]
      : []),
    ...(isAdmin ? [{ href: "/admin/materials", label: "Admin" }] : []),
  ];

  const triggerClass =
    "flex items-center gap-1 rounded-md px-2.5 py-1.5 text-sm text-muted transition-colors hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent";
  const panelClass =
    "absolute left-0 top-full z-50 mt-1 min-w-56 overflow-hidden rounded-lg border border-line bg-surface py-1.5 shadow-lg";
  const itemClass =
    "block px-3.5 py-2 text-sm text-muted transition-colors hover:bg-accent-soft hover:text-foreground";

  return (
    <>
      {/*
        Sticky rather than fixed. Fixed would need a matching spacer on every
        page to stop the header covering the first heading; sticky keeps the
        document flow intact and costs nothing.

        Opaque, not translucent. At 85% over the hero the fill resolved to
        within a hair of the hero's own top colour, so the bar stopped reading
        as a bar at all and its contents looked loose on the page.
      */}
      <header className="sticky top-0 z-40 border-b border-line bg-surface shadow-sm">
        {/*
          Full-bleed rather than boxed to a max width, so the logo and the
          controls sit 16px from their respective edges. The reference site's
          header runs the full width of the viewport; a centred container left
          everything stranded 81px in on a 1265px bar.
        */}
        <nav className="flex h-15 w-full items-center gap-3 px-4">
          <button
            type="button"
            aria-label="Open menu"
            aria-expanded={drawerOpen}
            onClick={() => setDrawerOpen(true)}
            className="-ml-1 rounded-md p-2 text-foreground transition-colors hover:bg-accent-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent lg:hidden"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true" fill="none">
              <path d="M3 5h14M3 10h14M3 15h14" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
            </svg>
          </button>

          <Link
            href="/"
            className="shrink-0 font-semibold tracking-tight hover:text-accent"
          >
            Agriculture Knowledge
          </Link>

          <div ref={menuRef} className="hidden items-center gap-0.5 lg:flex">
            <div className="relative">
              <button
                type="button"
                aria-expanded={openMenu === "exams"}
                aria-haspopup="true"
                onClick={() => setOpenMenu(openMenu === "exams" ? null : "exams")}
                className={triggerClass}
              >
                Exams
                <Chevron open={openMenu === "exams"} />
              </button>

              {openMenu === "exams" && (
                <div className={panelClass}>
                  {exams.length === 0 && (
                    <p className="px-3.5 py-2 text-sm text-muted">No exams published yet.</p>
                  )}
                  {exams.slice(0, 8).map((exam) => (
                    <Link
                      key={exam.id}
                      href={`/exams/${exam.slug}`}
                      onClick={closeMenu}
                      className={itemClass}
                    >
                      {exam.name}
                    </Link>
                  ))}
                  <div className="my-1.5 border-t border-line" />
                  <Link href="/exams" onClick={closeMenu} className={`${itemClass} text-accent`}>
                    All exams →
                  </Link>
                </div>
              )}
            </div>

            <Link href="/materials" className={triggerClass}>
              Library
            </Link>
            <Link href="/materials?type=QUIZ" className={triggerClass}>
              Quizzes
            </Link>

            <div className="relative">
              <button
                type="button"
                aria-expanded={openMenu === "more"}
                aria-haspopup="true"
                onClick={() => setOpenMenu(openMenu === "more" ? null : "more")}
                className={triggerClass}
              >
                More
                <Chevron open={openMenu === "more"} />
              </button>

              {openMenu === "more" && (
                <div className={panelClass}>
                  {moreLinks.map((link) => (
                    <Link
                      key={link.href}
                      href={link.href}
                      onClick={closeMenu}
                      className={itemClass}
                    >
                      {link.label}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>

          <form onSubmit={handleSearch} className="ml-auto hidden min-w-0 flex-1 justify-end md:flex">
            <label htmlFor="site-search" className="sr-only">
              Search the library
            </label>
            <input
              id="site-search"
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search articles, videos, quizzes"
              className="w-full max-w-72 rounded-full border border-line bg-surface px-4 py-1.5 text-sm outline-none placeholder:text-muted focus-visible:ring-2 focus-visible:ring-accent"
            />
          </form>

          <div className="ml-auto flex items-center gap-3 text-sm md:ml-0">
            <ThemeToggle />

            {status === "loading" && (
              <span className="hidden text-muted sm:inline">Checking session…</span>
            )}

            {/*
              No sign-up button here on purpose: the call to action belongs on
              the page, where it can be the largest thing in the hero rather
              than a 32px pill competing with the navigation.
            */}
            {status === "anonymous" && (
              <Link href="/login" className="text-muted hover:text-foreground">
                Sign in
              </Link>
            )}

            {status === "authenticated" && user && (
              <>
                <Link href="/me" className="hidden text-muted hover:text-foreground sm:inline">
                  {user.name}
                </Link>
                <button
                  type="button"
                  onClick={handleSignOut}
                  className="hidden text-muted underline underline-offset-4 hover:text-foreground sm:inline"
                >
                  Sign out
                </button>
              </>
            )}
          </div>
        </nav>
      </header>

      {/* ---------------------------------------------------------- drawer */}
      {drawerOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            aria-label="Close menu"
            onClick={() => setDrawerOpen(false)}
            className="absolute inset-0 h-full w-full bg-foreground/40 backdrop-blur-[2px]"
          />

          <div
            role="dialog"
            aria-modal="true"
            aria-label="Menu"
            className="drawer-panel absolute inset-y-0 left-0 flex w-[19rem] max-w-[85vw] flex-col border-r border-line bg-surface"
          >
            <div className="flex items-center justify-between border-b border-line px-5 py-4">
              <span className="font-semibold tracking-tight">Menu</span>
              <button
                type="button"
                aria-label="Close menu"
                onClick={() => setDrawerOpen(false)}
                className="rounded-md p-1.5 text-muted transition-colors hover:bg-accent-soft hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
              >
                <svg width="18" height="18" viewBox="0 0 20 20" aria-hidden="true" fill="none">
                  <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-3 py-4">
              <form onSubmit={handleSearch} className="px-2 pb-4">
                <label htmlFor="drawer-search" className="sr-only">
                  Search the library
                </label>
                <input
                  id="drawer-search"
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Search"
                  className="w-full rounded-full border border-line bg-background px-4 py-2 text-sm outline-none placeholder:text-muted focus-visible:ring-2 focus-visible:ring-accent"
                />
              </form>

              <DrawerSection title="Browse">
                <DrawerLink href="/materials" onClick={() => setDrawerOpen(false)}>
                  Library
                </DrawerLink>
                <DrawerLink href="/materials?type=QUIZ" onClick={() => setDrawerOpen(false)}>
                  Quizzes
                </DrawerLink>
                <DrawerLink href="/materials?type=VIDEO" onClick={() => setDrawerOpen(false)}>
                  Video lessons
                </DrawerLink>
                <DrawerLink href="/materials?type=ARTICLE" onClick={() => setDrawerOpen(false)}>
                  Articles
                </DrawerLink>
              </DrawerSection>

              <DrawerSection title="Exams">
                {exams.slice(0, 6).map((exam) => (
                  <DrawerLink
                    key={exam.id}
                    href={`/exams/${exam.slug}`}
                    onClick={() => setDrawerOpen(false)}
                  >
                    {exam.name}
                  </DrawerLink>
                ))}
                <DrawerLink href="/exams" onClick={() => setDrawerOpen(false)} accent>
                  All exams →
                </DrawerLink>
              </DrawerSection>

              {status === "authenticated" && user && (
                <DrawerSection title="You">
                  <DrawerLink href="/me" onClick={() => setDrawerOpen(false)}>
                    {user.name}
                  </DrawerLink>
                  <DrawerLink href="/me/paths" onClick={() => setDrawerOpen(false)}>
                    My learning paths
                  </DrawerLink>
                  <DrawerLink href="/me/attempts" onClick={() => setDrawerOpen(false)}>
                    My quiz attempts
                  </DrawerLink>
                  {isAdmin && (
                    <DrawerLink href="/admin/materials" onClick={() => setDrawerOpen(false)}>
                      Admin
                    </DrawerLink>
                  )}
                </DrawerSection>
              )}
            </div>

            <div className="border-t border-line px-5 py-4">
              {status === "anonymous" && (
                <Link
                  href="/login"
                  onClick={() => setDrawerOpen(false)}
                  className="block rounded-full border border-line px-4 py-2 text-center text-sm hover:border-accent"
                >
                  Sign in
                </Link>
              )}

              {status === "authenticated" && (
                <button
                  type="button"
                  onClick={handleSignOut}
                  className="w-full rounded-full border border-line px-4 py-2 text-sm hover:border-accent"
                >
                  Sign out
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function Chevron({ open }: { open: boolean }) {
  return (
    <svg
      width="10"
      height="10"
      viewBox="0 0 10 10"
      aria-hidden="true"
      fill="none"
      className={open ? "rotate-180 transition-transform" : "transition-transform"}
    >
      <path d="M1.5 3.5L5 7l3.5-3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  );
}

function DrawerSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="pb-4">
      <h2 className="px-2 pb-1.5 font-mono text-[11px] uppercase tracking-[0.14em] text-muted">
        {title}
      </h2>
      <div className="flex flex-col">{children}</div>
    </section>
  );
}

function DrawerLink({
  href,
  onClick,
  accent,
  children,
}: {
  href: string;
  onClick: () => void;
  accent?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className={`rounded-md px-2 py-2 text-sm transition-colors hover:bg-accent-soft ${
        accent ? "text-accent" : "text-foreground"
      }`}
    >
      {children}
    </Link>
  );
}
