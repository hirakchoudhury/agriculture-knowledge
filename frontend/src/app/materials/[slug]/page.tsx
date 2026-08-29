import Link from "next/link";
import { notFound } from "next/navigation";
import { CommentThread } from "@/components/comment-thread";
import { LikeButton } from "@/components/like-button";
import { MaterialActions } from "@/components/material-actions";
import { QuizStartCard } from "@/components/quiz-start-card";
import { YouTubeEmbed } from "@/components/youtube-embed";
import { fetchPublic } from "@/lib/public-api";
import type { MaterialDetail } from "@/lib/types";

const TYPE_LABEL: Record<string, string> = {
  ARTICLE: "Article",
  VIDEO: "Video lesson",
  QUIZ: "Practice quiz",
};

export const dynamic = "force-dynamic";

export async function generateMetadata({ params }: PageProps<"/materials/[slug]">) {
  const { slug } = await params;
  const material = await fetchPublic<MaterialDetail>(`/api/v1/materials/${slug}`);
  return {
    title: material ? `${material.title} · Agriculture Knowledge` : "Not found",
    description: material?.summary ?? undefined,
  };
}

export default async function MaterialPage({ params }: PageProps<"/materials/[slug]">) {
  const { slug } = await params;
  const material = await fetchPublic<MaterialDetail>(`/api/v1/materials/${slug}`);

  if (!material) {
    notFound();
  }

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-16">
      <Link href="/materials" className="font-mono text-xs text-muted hover:text-foreground">
        &larr; Library
      </Link>

      <p className="mt-6 font-mono text-xs uppercase tracking-[0.14em] text-accent">
        {TYPE_LABEL[material.type]}
        {material.readingMinutes ? ` · ${material.readingMinutes} min read` : ""}
      </p>

      <h1 className="mt-3 text-3xl font-semibold leading-tight tracking-tight">
        {material.title}
      </h1>

      {material.summary && <p className="mt-3 text-muted">{material.summary}</p>}

      <p className="mt-4 font-mono text-xs text-muted">
        {material.authorName}
        {material.publishedAt &&
          ` · ${new Date(material.publishedAt).toLocaleDateString()}`}
        {` · ${material.viewCount} views`}
      </p>

      {material.type === "VIDEO" && material.youtubeId && (
        <div className="mt-8">
          <YouTubeEmbed videoId={material.youtubeId} title={material.title} />
        </div>
      )}

      {material.type === "QUIZ" && (
        <div className="mt-8">
          <QuizStartCard slug={material.slug} />
        </div>
      )}

      {material.type === "ARTICLE" && material.bodyHtml && (
        /*
          The body is sanitised server-side on write with the OWASP allow-list, so
          what is stored is already safe. Rendering it as HTML here is deliberate;
          the alternative would be to sanitise again in every client that reads it.
        */
        <article
          className="prose-agri mt-8"
          dangerouslySetInnerHTML={{ __html: material.bodyHtml }}
        />
      )}

      <div className="mt-8">
        <LikeButton materialId={material.id} initialCount={material.likeCount} />
        <MaterialActions materialId={material.id} />
      </div>

      {(material.topics.length > 0 || material.exams.length > 0) && (
        <footer className="mt-12 border-t border-line pt-6">
          {material.topics.length > 0 && (
            <div className="flex flex-wrap items-baseline gap-2">
              <span className="font-mono text-xs uppercase tracking-wider text-muted">
                Topics
              </span>
              {material.topics.map((topic) => (
                <span
                  key={topic.id}
                  className="rounded border border-line px-2 py-0.5 text-xs text-muted"
                >
                  {topic.name}
                </span>
              ))}
            </div>
          )}
          {material.exams.length > 0 && (
            <div className="mt-3 flex flex-wrap items-baseline gap-2">
              <span className="font-mono text-xs uppercase tracking-wider text-muted">
                Exams
              </span>
              {material.exams.map((exam) => (
                <Link
                  key={exam.id}
                  href={`/exams/${exam.slug}`}
                  className="rounded border border-line px-2 py-0.5 text-xs text-accent hover:border-accent"
                >
                  {exam.name}
                </Link>
              ))}
            </div>
          )}
        </footer>
      )}

      <CommentThread materialId={material.id} />
    </main>
  );
}
