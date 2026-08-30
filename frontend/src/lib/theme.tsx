"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

export type Theme = "light" | "dark" | "system";

const STORAGE_KEY = "ak-theme";

type ThemeContextValue = {
  theme: Theme;
  /** What is actually on screen once "system" is resolved. */
  resolved: "light" | "dark";
  setTheme: (theme: Theme) => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * Runs before first paint, injected into <head>.
 *
 * Without this the page renders with the default palette and then corrects
 * itself once React hydrates, which is the flash of wrong theme everyone
 * recognises. It is deliberately tiny and dependency-free.
 */
export const themeInitScript = `
(function () {
  try {
    var stored = localStorage.getItem('${STORAGE_KEY}');
    if (stored === 'light' || stored === 'dark') {
      document.documentElement.setAttribute('data-theme', stored);
    }
  } catch (e) {
    // Private mode, or storage disabled. The media query still applies.
  }
})();
`;

function systemPrefersDark(): boolean {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-color-scheme: dark)").matches
  );
}

function apply(theme: Theme) {
  const root = document.documentElement;
  if (theme === "system") {
    // Removing the attribute hands control back to the media query rather than
    // freezing whatever the OS happened to be at the time.
    root.removeAttribute("data-theme");
  } else {
    root.setAttribute("data-theme", theme);
  }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  // Always starts at "system" so the server and the first client render agree;
  // the real value is read from storage in the effect below.
  const [theme, setThemeState] = useState<Theme>("system");
  const [resolved, setResolved] = useState<"light" | "dark">("light");

  useEffect(() => {
    let stored: Theme = "system";
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw === "light" || raw === "dark") {
        stored = raw;
      }
    } catch {
      // Storage unavailable; "system" is a fine answer.
    }
    setThemeState(stored);
    setResolved(stored === "system" ? (systemPrefersDark() ? "dark" : "light") : stored);
  }, []);

  // While following the system, track it live rather than only at load.
  useEffect(() => {
    if (theme !== "system" || typeof window === "undefined") return;

    const query = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => setResolved(query.matches ? "dark" : "light");
    query.addEventListener("change", onChange);
    return () => query.removeEventListener("change", onChange);
  }, [theme]);

  const setTheme = useCallback((next: Theme) => {
    setThemeState(next);
    setResolved(next === "system" ? (systemPrefersDark() ? "dark" : "light") : next);
    apply(next);
    try {
      if (next === "system") {
        localStorage.removeItem(STORAGE_KEY);
      } else {
        localStorage.setItem(STORAGE_KEY, next);
      }
    } catch {
      // The choice still applies for this page; it just will not be remembered.
    }
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, resolved, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used inside <ThemeProvider>");
  }
  return context;
}
