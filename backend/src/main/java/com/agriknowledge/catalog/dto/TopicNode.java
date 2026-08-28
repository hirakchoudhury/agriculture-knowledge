package com.agriknowledge.catalog.dto;

import com.agriknowledge.catalog.Topic;

import java.util.ArrayList;
import java.util.List;

/** A topic and everything beneath it. Children is empty rather than null at a leaf. */
public record TopicNode(
		Long id,
		String name,
		String slug,
		String description,
		int displayOrder,
		Long parentId,
		List<TopicNode> children) {

	public static TopicNode leaf(Topic topic) {
		return new TopicNode(
				topic.getId(),
				topic.getName(),
				topic.getSlug(),
				topic.getDescription(),
				topic.getDisplayOrder(),
				topic.getParentId(),
				new ArrayList<>());
	}

}
