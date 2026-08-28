"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import {
  createTopic,
  deleteTopic,
  flattenTopics,
  listTopics,
  updateTopic,
} from "@/lib/admin-api";
import type { TopicNode } from "@/lib/types";

export default function AdminTopicsPage() {
  const [topics, setTopics] = useState<TopicNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [name, setName] = useState("");
  const [parentId, setParentId] = useState<string>("");
  const [editingId, setEditingId] = useState<number | null>(null);

  const refresh = useCallback(async () => {
    try {
      setTopics(await listTopics());
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not load topics.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

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

  const rows = flattenTopics(topics);

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    await run(async () => {
      const siblings = parentId
        ? (rows.find((r) => r.node.id === Number(parentId))?.node.children.length ?? 0)
        : topics.length;
      await createTopic({
        name,
        parentId: parentId ? Number(parentId) : null,
        displayOrder: siblings,
      });
      setName("");
      setParentId("");
    });
  }

  return (
    <div className="flex flex-col gap-10">
      <section>
        <h2 className="text-lg font-semibold">Add a topic</h2>
        <p className="mt-1 text-sm text-muted">
          Leave the parent empty for a top-level subject. Topics are shared across
          exams, so add each one once.
        </p>

        <form onSubmit={handleCreate} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-start">
          <input
            aria-label="Topic name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="Soil Science"
            className="flex-1 rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
          <select
            aria-label="Parent topic"
            value={parentId}
            onChange={(e) => setParentId(e.target.value)}
            className="rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          >
            <option value="">No parent (top level)</option>
            {rows.map(({ node, depth }) => (
              <option key={node.id} value={node.id}>
                {"— ".repeat(depth)}
                {node.name}
              </option>
            ))}
          </select>
          <button
            type="submit"
            disabled={busy}
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
          >
            Add
          </button>
        </form>
      </section>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <section>
        <h2 className="text-lg font-semibold">
          Topic tree {!loading && <span className="text-muted">({rows.length})</span>}
        </h2>

        {loading && <p className="mt-4 text-sm text-muted">Loading…</p>}
        {!loading && rows.length === 0 && (
          <p className="mt-4 text-sm text-muted">No topics yet. Add one above.</p>
        )}

        <ul className="mt-4 flex flex-col">
          {rows.map(({ node, depth }) => (
            <li
              key={node.id}
              className="border-b border-line py-2.5 last:border-b-0"
              style={{ paddingLeft: depth * 20 }}
            >
              {editingId === node.id ? (
                <EditTopicForm
                  node={node}
                  options={rows.filter((r) => r.node.id !== node.id)}
                  busy={busy}
                  onCancel={() => setEditingId(null)}
                  onSave={async (input) => {
                    await run(() => updateTopic(node.id, input));
                    setEditingId(null);
                  }}
                />
              ) : (
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <span className="text-sm font-medium">{node.name}</span>
                    <span className="ml-2 font-mono text-xs text-muted">{node.slug}</span>
                  </div>
                  <div className="flex shrink-0 gap-3 text-sm">
                    <button
                      type="button"
                      onClick={() => setEditingId(node.id)}
                      className="text-muted underline underline-offset-4 hover:text-foreground"
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => void run(() => deleteTopic(node.id))}
                      className="text-danger underline underline-offset-4"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function EditTopicForm({
  node,
  options,
  busy,
  onSave,
  onCancel,
}: {
  node: TopicNode;
  options: Array<{ node: TopicNode; depth: number }>;
  busy: boolean;
  onSave: (input: {
    name: string;
    parentId: number | null;
    description: string | null;
    displayOrder: number;
  }) => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState(node.name);
  const [parentId, setParentId] = useState(node.parentId ? String(node.parentId) : "");

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSave({
          name,
          parentId: parentId ? Number(parentId) : null,
          description: node.description,
          displayOrder: node.displayOrder,
        });
      }}
      className="flex flex-col gap-2 sm:flex-row sm:items-center"
    >
      <input
        aria-label="Topic name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        className="flex-1 rounded-md border border-line bg-background px-3 py-1.5 text-sm"
      />
      <select
        aria-label="Parent topic"
        value={parentId}
        onChange={(e) => setParentId(e.target.value)}
        className="rounded-md border border-line bg-background px-3 py-1.5 text-sm"
      >
        <option value="">No parent (top level)</option>
        {/*
          The topic itself is filtered out by the caller. Its descendants are still
          listed; picking one is rejected by the server with a clear 409 rather than
          being silently hidden here.
        */}
        {options.map(({ node: option, depth }) => (
          <option key={option.id} value={option.id}>
            {"— ".repeat(depth)}
            {option.name}
          </option>
        ))}
      </select>
      <button
        type="submit"
        disabled={busy}
        className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-background disabled:opacity-60"
      >
        Save
      </button>
      <button
        type="button"
        onClick={onCancel}
        className="text-sm text-muted underline underline-offset-4"
      >
        Cancel
      </button>
    </form>
  );
}
