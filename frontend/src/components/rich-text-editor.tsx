"use client";

import Link from "@tiptap/extension-link";
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { useEffect } from "react";

/**
 * The editor emits HTML, which the API sanitises against an allow-list before
 * storing. Nothing here is a security boundary: a toolbar can always be bypassed,
 * so the server does not trust this output.
 */
export function RichTextEditor({
  value,
  onChange,
}: {
  value: string;
  onChange: (html: string) => void;
}) {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Link.configure({ openOnClick: false, autolink: true }),
    ],
    content: value,
    // Required in the App Router: rendering immediately would run the editor
    // during SSR, where there is no DOM, and produce a hydration mismatch.
    immediatelyRender: false,
    editorProps: {
      attributes: {
        class: "prose-agri min-h-64 px-4 py-3 outline-none",
      },
    },
    onUpdate: ({ editor: instance }) => onChange(instance.getHTML()),
  });

  // Loading an existing article replaces the content once, when it arrives.
  useEffect(() => {
    if (editor && value && editor.getHTML() !== value) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
    // Intentionally keyed on the editor only: re-running on every keystroke
    // would fight the user for control of the cursor.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editor]);

  if (!editor) {
    return (
      <div className="rounded-md border border-line bg-surface px-4 py-3 text-sm text-muted">
        Loading editor…
      </div>
    );
  }

  const button = (label: string, active: boolean, action: () => void) => (
    <button
      key={label}
      type="button"
      onMouseDown={(event) => {
        // Keeps focus in the document so the command applies to the selection.
        event.preventDefault();
        action();
      }}
      className={`rounded px-2 py-1 text-xs ${
        active ? "bg-accent text-background" : "text-muted hover:text-foreground"
      }`}
    >
      {label}
    </button>
  );

  return (
    <div className="rounded-md border border-line bg-surface">
      <div className="flex flex-wrap gap-1 border-b border-line px-2 py-1.5">
        {button("Bold", editor.isActive("bold"), () => editor.chain().focus().toggleBold().run())}
        {button("Italic", editor.isActive("italic"), () => editor.chain().focus().toggleItalic().run())}
        {button("H2", editor.isActive("heading", { level: 2 }), () =>
          editor.chain().focus().toggleHeading({ level: 2 }).run())}
        {button("H3", editor.isActive("heading", { level: 3 }), () =>
          editor.chain().focus().toggleHeading({ level: 3 }).run())}
        {button("Bullets", editor.isActive("bulletList"), () =>
          editor.chain().focus().toggleBulletList().run())}
        {button("Numbers", editor.isActive("orderedList"), () =>
          editor.chain().focus().toggleOrderedList().run())}
        {button("Quote", editor.isActive("blockquote"), () =>
          editor.chain().focus().toggleBlockquote().run())}
        {button("Code", editor.isActive("codeBlock"), () =>
          editor.chain().focus().toggleCodeBlock().run())}
        {button("Link", editor.isActive("link"), () => {
          const url = window.prompt("Link URL");
          if (url === null) return;
          if (url === "") {
            editor.chain().focus().unsetLink().run();
            return;
          }
          editor.chain().focus().setLink({ href: url }).run();
        })}
      </div>
      <EditorContent editor={editor} />
    </div>
  );
}
