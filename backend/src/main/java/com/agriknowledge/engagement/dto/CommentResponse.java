package com.agriknowledge.engagement.dto;

import java.time.Instant;
import java.util.List;

/**
 * @param deleted a soft-deleted comment still appears when it has replies, so the
 *     conversation underneath it stays readable. Its body is the placeholder.
 * @param mine    lets the client show edit and delete controls without comparing ids
 * @param replies always present, empty at a leaf. Only top-level comments have any.
 */
public record CommentResponse(
		Long id,
		String body,
		Long authorId,
		String authorName,
		String authorAvatarUrl,
		Instant createdAt,
		Instant editedAt,
		boolean deleted,
		boolean mine,
		Long parentId,
		List<CommentResponse> replies) {
}
