import Link from "next/link";
import { notFound } from "next/navigation";
import { TopicTree } from "@/components/topic-tree";
import { fetchPublic } from "@/lib/public-api";
import type { ExamDetail } from "@/lib/types";

export const dynamic = "force-dynamic";

export async function generateMetadata({ params }: PageProps<"/exams/[slug]">) {
  const { slug } = await params;
  const exam = await fetchPublic<ExamDetail>(`/api/v1/exams/${slug}`);
  return {
    title: exam ? `${exam.name} · Agriculture Knowledge` : "Exam not found",
    description: exam?.description ?? undefined,
  };
}

export default async function ExamPage({ params }: PageProps<"/exams/[slug]">) {
  const { slug } = await params;
  const exam = await fetchPublic<ExamDetail>(`/api/v1/exams/${slug}`);

  if (!exam) {
    notFound();
  }

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
      <Link href="/exams" className="font-mono text-xs text-muted hover:text-foreground">
        &larr; All exams
      </Link>

      <h1 className="mt-4 text-3xl font-semibold tracking-tight">{exam.name}</h1>
      {exam.description && <p className="mt-3 max-w-xl text-muted">{exam.description}</p>}

      <h2 className="mt-12 font-mono text-xs uppercase tracking-[0.14em] text-muted">
        Syllabus
      </h2>
      <div className="mt-4 rounded-md border border-line bg-surface p-6">
        <TopicTree nodes={exam.syllabus} />
      </div>

      <p className="mt-8 text-sm text-muted">
        Articles, video lessons and practice questions for these topics arrive in
        phases 4 to 6.
      </p>
    </main>
  );
}
