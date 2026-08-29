package com.agriknowledge.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Replaces the whole question set in one call.
 *
 * <p>Sending the complete list rather than one question at a time is what makes a
 * bulk import possible: fifty questions pasted in go in as a single request, and
 * repeating the call is idempotent.
 */
public record QuestionsRequest(
		@NotNull @Valid @Size(max = 200) List<QuestionRequest> questions) {
}
