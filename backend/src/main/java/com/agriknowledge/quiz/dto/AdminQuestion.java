package com.agriknowledge.quiz.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminQuestion(
		Long id,
		String text,
		String explanation,
		String imageUrl,
		BigDecimal marks,
		BigDecimal negativeMarks,
		int displayOrder,
		List<AdminOption> options) {
}
