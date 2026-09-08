package com.agriknowledge.material.dto;

import com.agriknowledge.material.Difficulty;
import com.agriknowledge.material.MaterialStatus;
import com.agriknowledge.material.MaterialType;

import java.time.Instant;
import java.util.List;

/**
 * One shape for every material type, discriminated by {@code type}.
 *
 * <p>{@code bodyHtml} and {@code readingMinutes} are populated for ARTICLE;
 * {@code youtubeId} and {@code durationSeconds} for VIDEO; {@code fileName},
 * {@code fileSizeBytes}, {@code pageCount} and {@code paperYear} for DOCUMENT. A
 * discriminated union is easier for the frontend to narrow than four
 * near-identical response types.
 *
 * <p>The storage key is deliberately absent: downloads go through an endpoint
 * that signs a short-lived URL, so the key is of no use to a client.
 */
public record MaterialDetail(
		Long id,
		MaterialType type,
		String title,
		String slug,
		String summary,
		String thumbnailUrl,
		Difficulty difficulty,
		MaterialStatus status,
		String authorName,
		Instant publishedAt,
		Instant updatedAt,
		long viewCount,
		long likeCount,
		long commentCount,
		List<TagRef> topics,
		List<TagRef> exams,
		String bodyHtml,
		Integer readingMinutes,
		String youtubeId,
		Integer durationSeconds,
		String fileName,
		Long fileSizeBytes,
		Integer pageCount,
		Integer paperYear,
		/** Whether the caller has liked this. Always false for anonymous readers. */
		boolean likedByMe) {

	/** Enough to render a chip and link to it, without the whole entity. */
	public record TagRef(Long id, String name, String slug) {
	}

}
