package com.agriknowledge.path.dto;

import java.time.Instant;

/**
 * @param completedCount how many steps are done, so the list can show progress
 *     without loading every path in full
 */
public record PathSummary(
		Long id,
		String title,
		String description,
		int itemCount,
		int completedCount,
		Instant createdAt) {
}
