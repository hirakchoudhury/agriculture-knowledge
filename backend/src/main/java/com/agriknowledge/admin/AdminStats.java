package com.agriknowledge.admin;

import java.util.List;
import java.util.Map;

/**
 * @param materialsByStatus DRAFT, PUBLISHED and ARCHIVED counts
 * @param materialsByType   ARTICLE, VIDEO and QUIZ counts, published only
 * @param mostViewed        the handful worth knowing about, not a full leaderboard
 */
public record AdminStats(
		Map<String, Long> materialsByStatus,
		Map<String, Long> materialsByType,
		long users,
		long admins,
		long topics,
		long exams,
		long comments,
		long likes,
		long quizAttempts,
		long learningPaths,
		List<PopularMaterial> mostViewed) {

	public record PopularMaterial(Long id, String title, String slug, String type,
			long viewCount, long likeCount) {
	}

}
