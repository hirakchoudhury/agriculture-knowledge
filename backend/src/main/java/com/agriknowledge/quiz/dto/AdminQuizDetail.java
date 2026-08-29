package com.agriknowledge.quiz.dto;

import com.agriknowledge.material.MaterialStatus;

import java.math.BigDecimal;
import java.util.List;

/** Everything an admin needs to edit a quiz, answer key included. */
public record AdminQuizDetail(
		Long id,
		String title,
		String slug,
		String summary,
		MaterialStatus status,
		Integer timeLimitSeconds,
		int passPercentage,
		boolean shuffleQuestions,
		BigDecimal totalMarks,
		List<AdminQuestion> questions) {
}
