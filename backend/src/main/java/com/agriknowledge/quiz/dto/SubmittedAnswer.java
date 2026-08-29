package com.agriknowledge.quiz.dto;

import jakarta.validation.constraints.NotNull;

/** @param selectedOptionId null for a question left unanswered. */
public record SubmittedAnswer(
		@NotNull Long questionId,
		Long selectedOptionId) {
}
