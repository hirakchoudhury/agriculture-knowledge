import Link from "next/link";
import { ThemeToggle } from "@/components/theme-toggle";

/**
 * Exists mainly to give the theme control a home now that it is out of the
 * header. The links are the ones people look for at the bottom of a page rather
 * than a second copy of the whole navigation.
 */
export function SiteFooter() {
  return (
    <footer className="mt-auto border-t border-line bg-surface">
      <div className="flex w-full flex-col gap-6 px-4 py-8 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="font-semibold tracking-tight">Agriculture Knowledge</p>
          <p className="mt-1 max-w-sm text-sm text-muted">
            Study material for agriculture competitive exams.
          </p>
        </div>

        <nav aria-label="Footer" className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
          <Link href="/materials" className="text-muted hover:text-foreground">
            Library
          </Link>
          <Link href="/exams" className="text-muted hover:text-foreground">
            Exams
          </Link>
          <Link href="/materials?type=QUIZ" className="text-muted hover:text-foreground">
            Quizzes
          </Link>
        </nav>

        <div className="flex flex-col gap-2 sm:items-end">
          <span className="font-mono text-[11px] uppercase tracking-[0.14em] text-muted">
            Theme
          </span>
          <ThemeToggle />
        </div>
      </div>
    </footer>
  );
}
