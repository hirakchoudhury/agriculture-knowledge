package com.agriknowledge.admin;

import com.agriknowledge.catalog.ExamRepository;
import com.agriknowledge.catalog.TopicRepository;
import com.agriknowledge.engagement.CommentRepository;
import com.agriknowledge.engagement.MaterialLikeRepository;
import com.agriknowledge.material.Material;
import com.agriknowledge.path.LearningPathRepository;
import com.agriknowledge.quiz.QuizAttemptRepository;
import com.agriknowledge.user.Role;
import com.agriknowledge.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

	private static final int MOST_VIEWED_LIMIT = 5;

	private final AdminStatsRepository materials;
	private final UserRepository users;
	private final TopicRepository topics;
	private final ExamRepository exams;
	private final CommentRepository comments;
	private final MaterialLikeRepository likes;
	private final QuizAttemptRepository attempts;
	private final LearningPathRepository paths;

	public AdminStatsService(AdminStatsRepository materials, UserRepository users,
			TopicRepository topics, ExamRepository exams, CommentRepository comments,
			MaterialLikeRepository likes, QuizAttemptRepository attempts,
			LearningPathRepository paths) {
		this.materials = materials;
		this.users = users;
		this.topics = topics;
		this.exams = exams;
		this.comments = comments;
		this.likes = likes;
		this.attempts = attempts;
		this.paths = paths;
	}

	public AdminStats collect() {
		return new AdminStats(
				// Buckets with no rows are absent from a GROUP BY, so seed them at
				// zero: a dashboard showing nothing at all for DRAFT is confusing.
				tally(materials.countByStatus(), "DRAFT", "PUBLISHED", "ARCHIVED"),
				tally(materials.countPublishedByType(), "ARTICLE", "VIDEO", "QUIZ"),
				users.count(),
				users.countByRole(Role.ADMIN),
				topics.count(),
				exams.count(),
				comments.count(),
				likes.count(),
				attempts.count(),
				paths.count(),
				materials.findMostViewed(PageRequest.of(0, MOST_VIEWED_LIMIT)).stream()
						.map(this::toPopular)
						.toList());
	}

	private Map<String, Long> tally(List<Object[]> rows, String... expected) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (String key : expected) {
			counts.put(key, 0L);
		}
		for (Object[] row : rows) {
			counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
		}
		return counts;
	}

	private AdminStats.PopularMaterial toPopular(Material material) {
		return new AdminStats.PopularMaterial(
				material.getId(),
				material.getTitle(),
				material.getSlug(),
				material.getType().name(),
				material.getViewCount(),
				material.getLikeCount());
	}

}
