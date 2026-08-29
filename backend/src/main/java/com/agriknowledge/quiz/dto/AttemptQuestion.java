package com.agriknowledge.quiz.dto;

import java.math.BigDecimal;
import java.util.List;

/** A question as a learner sees it while taking a quiz. Carries no explanation. */
public record AttemptQuestion(
		Long id,
		String text,
		String imageUrl,
		BigDecimal marks,
		BigDecimal negativeMarks,
		List<AttemptOption> options) {
}
