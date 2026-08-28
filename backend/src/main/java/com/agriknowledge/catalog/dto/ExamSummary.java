package com.agriknowledge.catalog.dto;

import com.agriknowledge.catalog.Exam;

/** List view. Carries a topic count so the landing page needs no extra query. */
public record ExamSummary(
		Long id,
		String name,
		String slug,
		String description,
		String iconUrl,
		int displayOrder,
		long topicCount) {

	public static ExamSummary of(Exam exam, long topicCount) {
		return new ExamSummary(
				exam.getId(),
				exam.getName(),
				exam.getSlug(),
				exam.getDescription(),
				exam.getIconUrl(),
				exam.getDisplayOrder(),
				topicCount);
	}

}
