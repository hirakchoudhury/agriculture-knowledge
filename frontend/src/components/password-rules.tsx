"use client";

/**
 * Live checklist of the password rules.
 *
 * The server is the authority — this mirrors StrongPasswordValidator — but
 * showing the rules as they are met turns a rejected form into something the
 * person can fix while typing, rather than after submitting.
 */
export function PasswordRules({ password }: { password: string }) {
  const rules = [
    { label: "8 characters or more", met: password.length >= 8 },
    { label: "a capital letter", met: /[A-Z]/.test(password) },
    { label: "a number", met: /[0-9]/.test(password) },
    // Anything that is not a letter, digit or space — matching the server, which
    // deliberately does not use a fixed list of allowed symbols.
    { label: "a symbol", met: /[^A-Za-z0-9\s]/.test(password) },
  ];

  return (
    <ul className="mt-1 flex flex-wrap gap-x-3 gap-y-1" aria-live="polite">
      {rules.map((rule) => (
        <li
          key={rule.label}
          className={`flex items-center gap-1 text-xs ${
            rule.met ? "text-accent" : "text-muted"
          }`}
        >
          <span aria-hidden>{rule.met ? "✓" : "○"}</span>
          <span>
            <span className="sr-only">{rule.met ? "Met: " : "Still needed: "}</span>
            {rule.label}
          </span>
        </li>
      ))}
    </ul>
  );
}
