package com.agriknowledge.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A submitted attempt with its marking. This is the only response shape that ever
 * reveals which option was correct.
 *
 * @param withinTimeLimit reported rather than enforced: the quiz is practice, and
 *     rejecting a late submission would throw away work over a network hiccup
 */
public record AttemptResult(
		Long attemptId,
		Long quizId,
		String quizSlug,
		String title,
		BigDecimal score,
		BigDecimal totalMarks,
		BigDecimal percentage,
		int passPercentage,
		boolean passed,
		boolean withinTimeLimit,
		Instant startedAt,
		Instant submittedAt,
		List<ReviewQuestion> questions) {
}
