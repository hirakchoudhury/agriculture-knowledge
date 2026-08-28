package com.agriknowledge.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Used for both create and update; the slug is derived from the name, never sent. */
public record ExamRequest(
		@NotBlank @Size(max = 160) String name,
		@Size(max = 4000) String description,
		@Size(max = 500) String iconUrl,
		@PositiveOrZero int displayOrder) {
}
