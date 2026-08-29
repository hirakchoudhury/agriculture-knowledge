package com.agriknowledge.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One row in a learner's attempt history. */
public record AttemptSummary(
		Long attemptId,
		Long quizId,
		String quizSlug,
		String quizTitle,
		BigDecimal score,
		BigDecimal totalMarks,
		BigDecimal percentage,
		boolean passed,
		Instant submittedAt) {
}
