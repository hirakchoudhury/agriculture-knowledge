package com.agriknowledge.path.dto;

import com.agriknowledge.path.ProgressStatus;

import java.time.Instant;

public record ProgressResponse(
		Long materialId,
		ProgressStatus status,
		boolean completed,
		Integer lastPositionSeconds,
		Instant completedAt) {
}
