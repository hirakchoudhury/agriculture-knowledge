import type { QuestionInput } from "./quiz-api";

export type ParseResult = {
  questions: QuestionInput[];
  errors: string[];
};

/**
 * Parses a plain-text block of MCQs.
 *
 * The format is deliberately the shape people already type questions in:
 *
 *   Q: Which nutrient is most affected by soil pH?
 *   *Phosphorus
 *   Carbon
 *   Silicon
 *   E: Phosphorus availability drops sharply outside pH 6 to 7.
 *
 * A blank line separates questions, an asterisk marks the correct option, and the
 * explanation is optional. Anything a bank of questions cannot express this way is
 * still editable afterwards in the form.
 *
 * Errors are collected rather than thrown, so pasting fifty questions reports every
 * problem at once instead of one per attempt.
 */
export function parseQuestions(input: string): ParseResult {
  const questions: QuestionInput[] = [];
  const errors: string[] = [];

  const blocks = input
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter((block) => block.length > 0);

  blocks.forEach((block, blockIndex) => {
    const label = `Question ${blockIndex + 1}`;
    const lines = block
      .split("\n")
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    let text = "";
    let explanation: string | null = null;
    const options: Array<{ text: string; correct: boolean }> = [];

    for (const line of lines) {
      if (/^q:/i.test(line)) {
        text = line.slice(2).trim();
      } else if (/^e:/i.test(line)) {
        explanation = line.slice(2).trim();
      } else if (line.startsWith("*")) {
        options.push({ text: line.slice(1).trim(), correct: true });
      } else if (text === "") {
        // A block that opens without a Q: prefix still reads as a question.
        text = line;
      } else {
        options.push({ text: line, correct: false });
      }
    }

    if (text === "") {
      errors.push(`${label}: no question text`);
      return;
    }
    if (options.length < 2) {
      errors.push(`${label}: needs at least two options`);
      return;
    }

    const correctCount = options.filter((option) => option.correct).length;
    if (correctCount === 0) {
      errors.push(`${label}: mark the correct option with a leading asterisk`);
      return;
    }
    if (correctCount > 1) {
      errors.push(`${label}: ${correctCount} options are marked correct; exactly one is allowed`);
      return;
    }

    questions.push({
      text,
      explanation,
      marks: 1,
      negativeMarks: 0,
      options,
    });
  });

  if (questions.length === 0 && errors.length === 0) {
    errors.push("Nothing to import.");
  }

  return { questions, errors };
}

/** The reverse, so an existing quiz can be edited as text and pasted elsewhere. */
export function toPasteFormat(questions: QuestionInput[]): string {
  return questions
    .map((question) => {
      const lines = [`Q: ${question.text}`];
      for (const option of question.options) {
        lines.push(option.correct ? `*${option.text}` : option.text);
      }
      if (question.explanation) {
        lines.push(`E: ${question.explanation}`);
      }
      return lines.join("\n");
    })
    .join("\n\n");
}
