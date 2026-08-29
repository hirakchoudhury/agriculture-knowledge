package com.agriknowledge.quiz.dto;

import java.math.BigDecimal;

/**
 * What a learner sees before starting: enough to decide whether to attempt it,
 * and nothing about the questions themselves.
 */
public record QuizSummary(
		Long id,
		String slug,
		String title,
		String summary,
		int questionCount,
		BigDecimal totalMarks,
		Integer timeLimitSeconds,
		int passPercentage,
		long attemptsByMe) {
}
