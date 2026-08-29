package com.agriknowledge.path.dto;

import com.agriknowledge.material.Difficulty;
import com.agriknowledge.material.MaterialType;
import com.agriknowledge.path.ProgressStatus;

/** One step in a path, with just enough of the material to render a row. */
public record PathItemResponse(
		Long itemId,
		Long materialId,
		String title,
		String slug,
		MaterialType type,
		Difficulty difficulty,
		int displayOrder,
		String note,
		ProgressStatus progress,
		boolean completed) {
}
