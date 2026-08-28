import { MaterialCard } from "@/components/material-card";
import { MaterialFilters } from "@/components/material-filters";
import { fetchPublic } from "@/lib/public-api";
import type { ExamSummary, MaterialSummary, PageResponse, TopicNode } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata = {
  title: "Library · Agriculture Knowledge",
  description: "Articles and video lessons for agriculture competitive exams.",
};

export default async function MaterialsPage({ searchParams }: PageProps<"/materials">) {
  const params = await searchParams;

  const query = new URLSearchParams();
  for (const key of ["type", "difficulty", "topicId", "examId", "q", "page"]) {
    const value = params[key];
    if (typeof value === "string" && value) {
      query.set(key, value);
    }
  }

  const [results, topics, exams] = await Promise.all([
    fetchPublic<PageResponse<MaterialSummary>>(`/api/v1/materials?${query}`),
    fetchPublic<TopicNode[]>("/api/v1/topics"),
    fetchPublic<ExamSummary[]>("/api/v1/exams"),
  ]);

  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-6 py-16">
      <h1 className="text-3xl font-semibold tracking-tight">Library</h1>
      <p className="mt-3 max-w-xl text-muted">
        Everything published so far, filterable by exam, topic and format.
      </p>

      <MaterialFilters topics={topics ?? []} exams={exams ?? []} />

      {results === null && (
        <p className="mt-8 text-sm text-danger">Could not reach the API.</p>
      )}

      {results && results.content.length === 0 && (
        <p className="mt-8 text-sm text-muted">
          Nothing matches those filters yet.
        </p>
      )}

      {results && results.content.length > 0 && (
        <>
          <p className="mt-8 font-mono text-xs text-muted">
            {results.totalElements} {results.totalElements === 1 ? "result" : "results"}
          </p>
          <ul className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {results.content.map((material) => (
              <li key={material.id}>
                <MaterialCard material={material} />
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  );
}
