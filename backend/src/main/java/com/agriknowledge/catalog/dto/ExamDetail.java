package com.agriknowledge.catalog.dto;

import java.util.List;

public record ExamDetail(
		Long id,
		String name,
		String slug,
		String description,
		String iconUrl,
		int displayOrder,
		List<TopicNode> syllabus) {
}
