import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-24">
      <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">404</p>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">
        That page is not here
      </h1>
      <p className="mt-3 text-muted">
        The link may be out of date, or the material may have been withdrawn.
      </p>
      <div className="mt-8 flex flex-wrap gap-4 text-sm">
        <Link
          href="/materials"
          className="rounded-md bg-accent px-4 py-2 font-medium text-background"
        >
          Browse the library
        </Link>
        <Link
          href="/exams"
          className="self-center text-accent underline underline-offset-4"
        >
          See the exams
        </Link>
      </div>
    </main>
  );
}
