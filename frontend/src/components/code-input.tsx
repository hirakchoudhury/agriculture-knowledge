"use client";

/**
 * A single field for the six-digit code, not six separate boxes.
 *
 * Six boxes look neater and are worse: paste often lands entirely in the first
 * one, autofill from an SMS or email rarely works, and screen readers announce
 * six unlabelled fields. One field with the right input attributes gets the
 * numeric keypad on a phone and lets the browser offer the code from the email.
 */
export function CodeInput({
  value,
  onChange,
  id = "code",
  disabled = false,
}: {
  value: string;
  onChange: (value: string) => void;
  id?: string;
  disabled?: boolean;
}) {
  return (
    <input
      id={id}
      value={value}
      disabled={disabled}
      onChange={(event) => {
        // People paste "123 456" and "code: 123456" more often than you would
        // think; keeping only digits saves a pointless error message.
        onChange(event.target.value.replace(/[^0-9]/g, "").slice(0, 6));
      }}
      inputMode="numeric"
      autoComplete="one-time-code"
      pattern="[0-9]*"
      maxLength={6}
      placeholder="000000"
      aria-label="Six-digit code"
      className="w-full rounded-md border border-line bg-surface px-3 py-2 text-center font-mono text-2xl tracking-[0.4em] outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:opacity-60"
    />
  );
}
