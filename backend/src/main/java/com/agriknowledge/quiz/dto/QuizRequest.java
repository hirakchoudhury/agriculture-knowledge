package com.agriknowledge.quiz.dto;

import com.agriknowledge.material.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizRequest(
		@NotBlank @Size(max = 250) String title,
		@Size(max = 500) String summary,
		@Size(max = 500) String thumbnailUrl,
		@NotNull Difficulty difficulty,
		/** Null for an untimed quiz. */
		@Positive Integer timeLimitSeconds,
		@Min(0) @Max(100) int passPercentage,
		boolean shuffleQuestions,
		List<Long> topicIds,
		List<Long> examIds) {
}
