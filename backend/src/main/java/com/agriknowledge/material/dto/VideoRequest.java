package com.agriknowledge.material.dto;

import com.agriknowledge.material.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VideoRequest(
		@NotBlank @Size(max = 250) String title,
		@Size(max = 500) String summary,
		@Size(max = 500) String thumbnailUrl,
		@NotNull Difficulty difficulty,
		/** Any YouTube URL shape, or a bare id. Parsed into an id before storage. */
		@NotBlank String youtubeUrl,
		@Positive Integer durationSeconds,
		List<Long> topicIds,
		List<Long> examIds) {
}
