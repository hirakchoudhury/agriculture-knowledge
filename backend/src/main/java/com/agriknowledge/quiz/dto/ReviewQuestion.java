package com.agriknowledge.quiz.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param awarded what this question contributed: the marks if right, minus the
 *     negative marks if wrong, zero if left unanswered
 */
public record ReviewQuestion(
		Long id,
		String text,
		String explanation,
		String imageUrl,
		BigDecimal marks,
		BigDecimal negativeMarks,
		BigDecimal awarded,
		Long selectedOptionId,
		Long correctOptionId,
		boolean answeredCorrectly,
		List<ReviewOption> options) {
}
