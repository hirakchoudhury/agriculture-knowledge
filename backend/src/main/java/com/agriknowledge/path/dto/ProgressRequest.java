package com.agriknowledge.path.dto;

import com.agriknowledge.path.ProgressStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProgressRequest(
		@NotNull ProgressStatus status,
		/** Only meaningful for videos; ignored for everything else. */
		@PositiveOrZero Integer lastPositionSeconds) {
}
