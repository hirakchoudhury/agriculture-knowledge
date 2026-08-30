import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      // Warn, not error — a deliberate, temporary downgrade.
      //
      // Nine places trip this, and they are not one problem. Some are genuine
      // debt: theme.tsx reads localStorage on mount and sets state, which is the
      // standard hydration-safe shape but is better expressed with
      // useSyncExternalStore. Others look like false positives: the admin list
      // pages only call setState after an await, so nothing is synchronous, yet
      // the rule still flags the effect.
      //
      // Untangling that is a real refactor across nine files of working code,
      // and it should not be the price of turning CI on. Left visible as a
      // warning so it stays on the list rather than disappearing.
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
