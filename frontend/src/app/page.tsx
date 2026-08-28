import { apiFetch, API_BASE_URL, ApiError } from "@/lib/api";
import type { HealthResponse } from "@/lib/types";

// The API is a separate service, so this page must render per request.
// Without this Next would try to reach the backend during `next build`,
// and a Vercel deploy would fail whenever the API happened to be down.
export const dynamic = "force-dynamic";

type Probe =
  | { ok: true; health: HealthResponse }
  | { ok: false; message: string };

async function probeApi(): Promise<Probe> {
  try {
    const health = await apiFetch<HealthResponse>("/api/v1/health");
    return { ok: true, health };
  } catch (error) {
    if (error instanceof ApiError) {
      return { ok: false, message: `API replied ${error.status}: ${error.message}` };
    }
    return {
      ok: false,
      message:
        error instanceof Error
          ? error.message
          : "Could not reach the API for an unknown reason.",
    };
  }
}

export default async function Home() {
  const probe = await probeApi();

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-20">
      <header className="border-b border-line pb-10">
        <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Phase 1 · skeleton
        </p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight sm:text-5xl">
          Agriculture Knowledge
        </h1>
        <p className="mt-4 max-w-xl text-muted">
          Articles, video lessons and practice questions for agriculture
          competitive exams. Nothing is published yet — this page exists to prove
          the browser can reach the API across origins.
        </p>
      </header>

      <section className="mt-10">
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          API connection
        </h2>

        <div className="mt-4 rounded-md border border-line bg-surface p-6">
          <div className="flex items-center gap-3">
            <span
              aria-hidden
              className={`inline-block size-2.5 rounded-full ${
                probe.ok ? "bg-accent" : "bg-danger"
              }`}
            />
            <span className="text-lg font-semibold">
              {probe.ok ? "Reachable" : "Unreachable"}
            </span>
          </div>

          {probe.ok ? (
            <dl className="mt-6 grid grid-cols-1 gap-x-8 gap-y-3 sm:grid-cols-[auto_1fr]">
              <Row label="Service" value={probe.health.service} />
              <Row label="Status" value={probe.health.status} />
              <Row label="Version" value={probe.health.version} />
              <Row
                label="Profiles"
                value={
                  probe.health.profiles.length > 0
                    ? probe.health.profiles.join(", ")
                    : "default"
                }
              />
              <Row label="Responded" value={probe.health.timestamp} />
              <Row label="Origin" value={API_BASE_URL} />
            </dl>
          ) : (
            <div className="mt-5 space-y-3 text-sm">
              <p className="text-danger">{probe.message}</p>
              <p className="text-muted">
                Tried{" "}
                <code className="font-mono text-foreground">
                  {API_BASE_URL}/api/v1/health
                </code>
                . Start the backend with{" "}
                <code className="font-mono text-foreground">./mvnw spring-boot:run</code>{" "}
                in the <code className="font-mono text-foreground">backend</code>{" "}
                directory, or correct{" "}
                <code className="font-mono text-foreground">NEXT_PUBLIC_API_URL</code>{" "}
                in <code className="font-mono text-foreground">.env.local</code>.
              </p>
            </div>
          )}
        </div>
      </section>

      <section className="mt-12">
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Next up
        </h2>
        <ul className="mt-4 space-y-2 text-sm text-muted">
          <li>
            <span className="text-foreground">Phase 2 — Identity.</span> Users
            table, register and login, JWT filter, Google sign-in, role guards.
          </li>
          <li>
            <span className="text-foreground">Phase 3 — Taxonomy.</span> Exams and
            the topic tree, with admin screens to manage them.
          </li>
        </ul>
      </section>
    </main>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <>
      <dt className="font-mono text-xs uppercase tracking-[0.1em] text-muted sm:pt-0.5">
        {label}
      </dt>
      <dd className="font-mono text-sm break-all">{value}</dd>
    </>
  );
}
