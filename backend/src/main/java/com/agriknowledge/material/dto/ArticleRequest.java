package com.agriknowledge.material.dto;

import com.agriknowledge.material.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArticleRequest(
		@NotBlank @Size(max = 250) String title,
		@Size(max = 500) String summary,
		@Size(max = 500) String thumbnailUrl,
		@NotNull Difficulty difficulty,
		/** Sanitised server-side before storage; whatever the editor sends is untrusted. */
		@NotBlank String bodyHtml,
		List<Long> topicIds,
		List<Long> examIds) {
}
