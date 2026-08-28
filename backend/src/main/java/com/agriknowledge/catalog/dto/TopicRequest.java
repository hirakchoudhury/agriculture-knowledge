package com.agriknowledge.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TopicRequest(
		@NotBlank @Size(max = 160) String name,
		/** Null makes this a root topic. */
		Long parentId,
		@Size(max = 4000) String description,
		@PositiveOrZero int displayOrder) {
}
