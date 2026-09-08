package com.agriknowledge.material.dto;

import com.agriknowledge.material.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The metadata half of a document upload.
 *
 * <p>Sent as the JSON part of a multipart request; the file is the other part.
 */
public record DocumentRequest(
		@NotBlank @Size(max = 250) String title,
		@Size(max = 500) String summary,
		@Size(max = 500) String thumbnailUrl,
		@NotNull Difficulty difficulty,
		/** The year the paper is from. Null for anything not year-specific. */
		@Min(1900) @Max(2200) Integer paperYear,
		List<Long> topicIds,
		List<Long> examIds) {
}
