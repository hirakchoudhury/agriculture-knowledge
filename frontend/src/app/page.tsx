import Link from "next/link";
import { fetchPublic } from "@/lib/public-api";
import type { ExamSummary, MaterialSummary, PageResponse } from "@/lib/types";

// The API is a separate service, so this page must render per request. Without
// this Next would try to reach the backend during `next build`, and a Vercel
// deploy would fail whenever the API happened to be down.
export const dynamic = "force-dynamic";

export default async function Home() {
  // One round trip each, in parallel rather than in sequence.
  const [articles, exams] = await Promise.all([
    fetchPublic<PageResponse<MaterialSummary>>("/api/v1/materials?type=ARTICLE&size=4"),
    fetchPublic<ExamSummary[]>("/api/v1/exams"),
  ]);

  return (
    <main className="flex-1">
      {/*
        The gradient sits on a full-bleed band rather than on <main>, so it runs
        edge to edge under the sticky header while the content below keeps the
        same measured column as every other page.
      */}
      <section className="grad-hero border-b border-line">
        <div className="mx-auto w-full max-w-4xl px-6 pb-16 pt-20 sm:pt-24">
          {/*
            The headline is a promise, not the site's name: "Agriculture
            Knowledge" already sits in the header a few centimetres above, so
            repeating it here spent the largest text on the page saying nothing.
          */}
          <h1 className="max-w-2xl text-5xl font-bold leading-[1.03] tracking-tight sm:text-6xl">
            Ace your next exam
          </h1>
          <p className="mt-5 max-w-xl text-lg text-muted">
            Articles, video lessons and practice questions, organised by the exam they
            count towards and the topic they cover.
          </p>

          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              href="/materials"
              className="btn-grad rounded-full px-6 py-2.5 text-sm font-semibold"
            >
              Browse the library
            </Link>
            <Link
              href="/materials?type=QUIZ"
              className="rounded-full border border-line bg-surface px-6 py-2.5 text-sm font-semibold transition-colors hover:border-accent"
            >
              Practice quizzes
            </Link>
          </div>
        </div>
      </section>

      <div className="mx-auto w-full max-w-4xl px-6 pb-20">
        {/*
          Promoted above the reading list. A visitor arrives with one question --
          "is there anything here for my exam?" -- and this answers it without
          making them find the Exams tab first.
        */}
        <section className="pt-16">
          <div className="flex items-baseline justify-between gap-4">
            <h2 className="text-2xl font-bold tracking-tight">Choose your exam</h2>
            {exams && exams.length > 0 && (
              <Link
                href="/exams"
                className="text-xs text-accent underline underline-offset-4"
              >
                All exams
              </Link>
            )}
          </div>

          {exams === null && (
            <p className="mt-4 text-sm text-danger">
              Could not reach the API just now. Please try again in a moment.
            </p>
          )}

          {exams?.length === 0 && (
            <p className="mt-4 text-sm text-muted">No syllabuses published yet.</p>
          )}

          {exams && exams.length > 0 && (
            <ul className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {exams.map((exam) => (
                <li key={exam.id}>
                  <Link
                    href={`/exams/${exam.slug}`}
                    className="flex h-full flex-col rounded-md border border-line bg-surface p-5 transition-colors hover:border-accent"
                  >
                    <h3 className="font-semibold leading-snug">{exam.name}</h3>
                    {exam.description && (
                      <p className="mt-1.5 line-clamp-2 text-sm text-muted">
                        {exam.description}
                      </p>
                    )}
                    <span className="mt-auto pt-3 font-mono text-xs text-muted">
                      {exam.topicCount} {exam.topicCount === 1 ? "topic" : "topics"}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Reading material second, because the exam is the coarser choice. */}
        <section className="mt-16">
          <div className="flex items-baseline justify-between gap-4">
            <h2 className="text-2xl font-bold tracking-tight">Latest reading</h2>
            {articles && articles.totalElements > 4 && (
              <Link
                href="/materials?type=ARTICLE"
                className="text-xs text-accent underline underline-offset-4"
              >
                All {articles.totalElements} articles
              </Link>
            )}
          </div>

          {articles === null && (
            <p className="mt-4 text-sm text-danger">
              Could not reach the API just now. Please try again in a moment.
            </p>
          )}

          {articles?.content.length === 0 && (
            <p className="mt-4 text-sm text-muted">Nothing published yet.</p>
          )}

          <ul className="mt-5 flex flex-col gap-3">
            {articles?.content.map((article) => (
              <li key={article.id}>
                <Link
                  href={`/materials/${article.slug}`}
                  className="block rounded-md border border-line bg-surface p-5 transition-colors hover:border-accent"
                >
                  <h3 className="font-semibold leading-snug">{article.title}</h3>

                  {/* The summary is the excerpt: enough to decide whether to read on. */}
                  {article.summary && (
                    <p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted">
                      {article.summary}
                    </p>
                  )}

                  <p className="mt-3 flex flex-wrap items-center gap-x-3 font-mono text-[11px] text-muted">
                    <span className="text-accent">Read more →</span>
                    {article.topicNames.length > 0 && (
                      <span>{article.topicNames.join(" · ")}</span>
                    )}
                    <span>
                      {article.viewCount} {article.viewCount === 1 ? "view" : "views"}
                    </span>
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        </section>

        <section className="mt-16 border-t border-line pt-8">
          <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
            How it works
          </h2>
          <ul className="mt-4 space-y-2 text-sm text-muted">
            <li>
              <span className="text-foreground">Read and watch.</span> Articles and
              video lessons, tagged by topic and by the exams they count towards.
            </li>
            <li>
              <span className="text-foreground">Test yourself.</span> Practice quizzes
              marked the moment you submit, with an explanation for every question.
            </li>
            <li>
              <span className="text-foreground">Build your own order.</span> Collect
              anything into a learning path and track what you have finished.
            </li>
          </ul>
        </section>
      </div>
    </main>
  );
}
