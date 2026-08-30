"use client";

import { useTheme, type Theme } from "@/lib/theme";

const OPTIONS: Array<{ value: Theme; label: string; symbol: string }> = [
  { value: "light", label: "Light", symbol: "☀" },
  { value: "dark", label: "Dark", symbol: "☾" },
  { value: "system", label: "Match system", symbol: "◐" },
];

/**
 * Three states rather than a two-way switch. A plain toggle cannot express
 * "follow my system", which is what most people want by default and the only
 * setting that keeps up when their phone switches at sunset.
 */
export function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <div
      role="radiogroup"
      aria-label="Colour theme"
      className="flex items-center gap-0.5 rounded-md border border-line p-0.5"
    >
      {OPTIONS.map((option) => {
        const active = theme === option.value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            aria-label={option.label}
            title={option.label}
            onClick={() => setTheme(option.value)}
            className={`rounded px-1.5 py-0.5 text-xs leading-none transition-colors ${
              active
                ? "bg-accent text-background"
                : "text-muted hover:text-foreground"
            }`}
          >
            <span aria-hidden>{option.symbol}</span>
          </button>
        );
      })}
    </div>
  );
}
