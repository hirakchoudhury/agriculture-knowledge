package com.agriknowledge.quiz.dto;

import java.time.Instant;
import java.util.List;

/**
 * An attempt in progress.
 *
 * @param expiresAt null for an untimed quiz. Computed from the server clock so a
 *     client with a skewed clock cannot award itself more time on screen.
 */
public record AttemptView(
		Long attemptId,
		Long quizId,
		String quizSlug,
		String title,
		Integer timeLimitSeconds,
		Instant startedAt,
		Instant expiresAt,
		List<AttemptQuestion> questions) {
}
