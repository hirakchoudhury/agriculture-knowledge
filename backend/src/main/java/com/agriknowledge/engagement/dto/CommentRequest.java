package com.agriknowledge.engagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
		@NotBlank @Size(max = 4000) String body,
		/** Null for a top-level comment. Must name a top-level comment, not a reply. */
		Long parentId) {
}
