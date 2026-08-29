"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  addComment,
  deleteComment,
  editComment,
  listComments,
} from "@/lib/engagement-api";
import type { CommentResponse } from "@/lib/types";

export function CommentThread({ materialId }: { materialId: number }) {
  const { status, isAdmin } = useAuth();
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [body, setBody] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const page = await listComments(materialId);
      setComments(page.content);
      setTotal(page.totalElements);
      setError(null);
    } catch (caught) {
      setError(
        caught instanceof ApiError ? caught.message : "Could not load the discussion.",
      );
    } finally {
      setLoading(false);
    }
  }, [materialId]);

  // Wait for the session to settle before fetching. The auth context restores it
  // with a round trip, and a request sent before that arrives carries no token —
  // so the server sees an anonymous reader and every comment comes back with
  // mine = false, hiding the author's own edit and delete controls.
  useEffect(() => {
    if (status === "loading") {
      return;
    }
    void refresh();
  }, [refresh, status]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  }

  async function post(event: React.FormEvent) {
    event.preventDefault();
    await run(async () => {
      await addComment(materialId, body.trim());
      setBody("");
    });
  }

  return (
    <section className="mt-14 border-t border-line pt-8">
      <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
        Discussion {total > 0 && `(${total})`}
      </h2>

      {status === "authenticated" ? (
        <form onSubmit={post} className="mt-4 flex flex-col gap-2">
          <label htmlFor="new-comment" className="sr-only">
            Add a comment
          </label>
          <textarea
            id="new-comment"
            rows={3}
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder="Ask a question or add something useful…"
            className="rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
          <div>
            <button
              type="submit"
              disabled={busy || body.trim().length === 0}
              className="rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-background disabled:opacity-60"
            >
              Post comment
            </button>
          </div>
        </form>
      ) : (
        <p className="mt-4 text-sm text-muted">
          <Link href="/login" className="text-accent underline underline-offset-4">
            Sign in
          </Link>{" "}
          to join the discussion.
        </p>
      )}

      {error && (
        <p role="alert" className="mt-4 text-sm text-danger">
          {error}
        </p>
      )}

      {loading && <p className="mt-6 text-sm text-muted">Loading…</p>}

      {!loading && comments.length === 0 && (
        <p className="mt-6 text-sm text-muted">No comments yet.</p>
      )}

      <ul className="mt-6 flex flex-col gap-6">
        {comments.map((comment) => (
          <li key={comment.id}>
            <CommentItem
              comment={comment}
              materialId={materialId}
              canModerate={isAdmin}
              signedIn={status === "authenticated"}
              busy={busy}
              run={run}
            />
          </li>
        ))}
      </ul>
    </section>
  );
}

function CommentItem({
  comment,
  materialId,
  canModerate,
  signedIn,
  busy,
  run,
  isReply = false,
}: {
  comment: CommentResponse;
  materialId: number;
  canModerate: boolean;
  signedIn: boolean;
  busy: boolean;
  run: (action: () => Promise<unknown>) => Promise<void>;
  isReply?: boolean;
}) {
  const [replying, setReplying] = useState(false);
  const [replyBody, setReplyBody] = useState("");
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(comment.body);

  return (
    <article>
      <header className="flex flex-wrap items-baseline gap-2">
        <span className="text-sm font-medium">
          {comment.deleted ? "—" : comment.authorName}
        </span>
        <time
          dateTime={comment.createdAt}
          className="font-mono text-xs text-muted"
          title={new Date(comment.createdAt).toLocaleString()}
        >
          {new Date(comment.createdAt).toLocaleDateString()}
        </time>
        {comment.editedAt && !comment.deleted && (
          <span className="font-mono text-xs text-muted">edited</span>
        )}
      </header>

      {editing ? (
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            await run(() => editComment(comment.id, draft.trim()));
            setEditing(false);
          }}
          className="mt-2 flex flex-col gap-2"
        >
          <label htmlFor={`edit-${comment.id}`} className="sr-only">
            Edit comment
          </label>
          <textarea
            id={`edit-${comment.id}`}
            rows={3}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            className="rounded-md border border-line bg-surface px-3 py-2 text-sm"
          />
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={busy || draft.trim().length === 0}
              className="rounded-md bg-accent px-3 py-1 text-sm font-medium text-background disabled:opacity-60"
            >
              Save
            </button>
            <button
              type="button"
              onClick={() => {
                setDraft(comment.body);
                setEditing(false);
              }}
              className="text-sm text-muted underline underline-offset-4"
            >
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <p
          className={`mt-1.5 whitespace-pre-wrap text-sm ${
            comment.deleted ? "italic text-muted" : ""
          }`}
        >
          {comment.body}
        </p>
      )}

      {!comment.deleted && !editing && (
        <div className="mt-2 flex flex-wrap gap-4 text-xs">
          {/* Replies are one level deep, so a reply cannot itself be replied to. */}
          {signedIn && !isReply && (
            <button
              type="button"
              onClick={() => setReplying((open) => !open)}
              className="text-muted underline underline-offset-4 hover:text-foreground"
            >
              {replying ? "Cancel reply" : "Reply"}
            </button>
          )}
          {comment.mine && (
            <button
              type="button"
              onClick={() => setEditing(true)}
              className="text-muted underline underline-offset-4 hover:text-foreground"
            >
              Edit
            </button>
          )}
          {(comment.mine || canModerate) && (
            <button
              type="button"
              disabled={busy}
              onClick={() => void run(() => deleteComment(comment.id))}
              className="text-danger underline underline-offset-4"
            >
              Delete
            </button>
          )}
        </div>
      )}

      {replying && (
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            await run(() => addComment(materialId, replyBody.trim(), comment.id));
            setReplyBody("");
            setReplying(false);
          }}
          className="mt-3 flex flex-col gap-2"
        >
          <label htmlFor={`reply-${comment.id}`} className="sr-only">
            Reply
          </label>
          <textarea
            id={`reply-${comment.id}`}
            rows={2}
            value={replyBody}
            onChange={(event) => setReplyBody(event.target.value)}
            placeholder="Write a reply…"
            className="rounded-md border border-line bg-surface px-3 py-2 text-sm"
          />
          <div>
            <button
              type="submit"
              disabled={busy || replyBody.trim().length === 0}
              className="rounded-md bg-accent px-3 py-1 text-sm font-medium text-background disabled:opacity-60"
            >
              Post reply
            </button>
          </div>
        </form>
      )}

      {comment.replies.length > 0 && (
        <ul className="mt-4 flex flex-col gap-4 border-l border-line pl-4">
          {comment.replies.map((reply) => (
            <li key={reply.id}>
              <CommentItem
                comment={reply}
                materialId={materialId}
                canModerate={canModerate}
                signedIn={signedIn}
                busy={busy}
                run={run}
                isReply
              />
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
