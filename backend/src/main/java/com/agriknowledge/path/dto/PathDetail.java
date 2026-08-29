package com.agriknowledge.path.dto;

import java.time.Instant;
import java.util.List;

public record PathDetail(
		Long id,
		String title,
		String description,
		int itemCount,
		int completedCount,
		Instant createdAt,
		List<PathItemResponse> items) {
}
