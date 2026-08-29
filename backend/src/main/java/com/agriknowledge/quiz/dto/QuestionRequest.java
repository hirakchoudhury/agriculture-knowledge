package com.agriknowledge.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Options arrive in display order, and exactly one must be marked correct. That
 * rule is checked in the service rather than by an annotation, because it spans
 * the whole list.
 */
public record QuestionRequest(
		@NotBlank @Size(max = 2000) String text,
		@Size(max = 2000) String explanation,
		@Size(max = 500) String imageUrl,
		@NotNull @DecimalMin(value = "0.01") BigDecimal marks,
		@NotNull @DecimalMin(value = "0.00") BigDecimal negativeMarks,
		@NotNull @Valid @Size(min = 2, max = 8) List<OptionRequest> options) {
}
