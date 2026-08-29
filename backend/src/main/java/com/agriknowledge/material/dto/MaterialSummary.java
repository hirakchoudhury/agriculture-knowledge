package com.agriknowledge.material.dto;

import com.agriknowledge.material.Difficulty;
import com.agriknowledge.material.MaterialStatus;
import com.agriknowledge.material.MaterialType;

import java.time.Instant;
import java.util.List;

/** Card view. Deliberately excludes article bodies, which are large and unused here. */
public record MaterialSummary(
		Long id,
		MaterialType type,
		String title,
		String slug,
		String summary,
		String thumbnailUrl,
		Difficulty difficulty,
		MaterialStatus status,
		Instant publishedAt,
		long viewCount,
		long likeCount,
		long commentCount,
		List<String> topicNames,
		boolean likedByMe) {
}
