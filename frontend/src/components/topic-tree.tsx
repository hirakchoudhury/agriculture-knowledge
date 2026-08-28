import type { TopicNode } from "@/lib/types";

/**
 * Renders a topic forest. Indentation comes from nesting rather than a computed
 * depth, so the markup stays a real list and screen readers announce the structure.
 */
export function TopicTree({ nodes }: { nodes: TopicNode[] }) {
  if (nodes.length === 0) {
    return <p className="text-sm text-muted">No topics have been added yet.</p>;
  }

  return (
    <ul className="flex flex-col gap-2">
      {nodes.map((node) => (
        <li key={node.id}>
          <div className="flex flex-col gap-0.5">
            <span className="text-sm font-medium">{node.name}</span>
            {node.description && (
              <span className="text-xs text-muted">{node.description}</span>
            )}
          </div>
          {node.children.length > 0 && (
            <div className="mt-2 border-l border-line pl-4">
              <TopicTree nodes={node.children} />
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}
