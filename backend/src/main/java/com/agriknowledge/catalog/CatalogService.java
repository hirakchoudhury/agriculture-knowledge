package com.agriknowledge.catalog;

import com.agriknowledge.catalog.dto.ExamDetail;
import com.agriknowledge.catalog.dto.ExamRequest;
import com.agriknowledge.catalog.dto.ExamSummary;
import com.agriknowledge.catalog.dto.TopicNode;
import com.agriknowledge.catalog.dto.TopicRequest;
import com.agriknowledge.common.ConflictException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.common.Slugs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CatalogService {

	private final ExamRepository exams;
	private final TopicRepository topics;

	public CatalogService(ExamRepository exams, TopicRepository topics) {
		this.exams = exams;
		this.topics = topics;
	}

	// ----- public reads ------------------------------------------------------

	public List<ExamSummary> listExams() {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : exams.countTopicsPerExam()) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return exams.findAllByOrderByDisplayOrderAscNameAsc().stream()
				.map(exam -> ExamSummary.of(exam, counts.getOrDefault(exam.getId(), 0L)))
				.toList();
	}

	public ExamDetail getExam(String slug) {
		Exam exam = exams.findBySlug(slug)
				.orElseThrow(() -> new NotFoundException("No exam with slug '%s'".formatted(slug)));

		return new ExamDetail(
				exam.getId(),
				exam.getName(),
				exam.getSlug(),
				exam.getDescription(),
				exam.getIconUrl(),
				exam.getDisplayOrder(),
				buildTree(topics.findAllForExam(slug)));
	}

	public List<TopicNode> topicTree() {
		return buildTree(topics.findAllForTree());
	}

	/**
	 * Assembles a flat, ordered list into a forest.
	 *
	 * <p>A topic whose parent is not in the list becomes a root. That matters for an
	 * exam syllabus, which may include a sub-topic without including its parent —
	 * dropping it would silently hide part of the syllabus.
	 */
	private List<TopicNode> buildTree(List<Topic> flat) {
		Map<Long, TopicNode> nodes = new HashMap<>();
		for (Topic topic : flat) {
			nodes.put(topic.getId(), TopicNode.leaf(topic));
		}

		List<TopicNode> roots = new ArrayList<>();
		for (Topic topic : flat) {
			TopicNode node = nodes.get(topic.getId());
			TopicNode parent = topic.getParentId() == null ? null : nodes.get(topic.getParentId());
			if (parent == null) {
				roots.add(node);
			}
			else {
				parent.children().add(node);
			}
		}
		return roots;
	}

	// ----- admin writes ------------------------------------------------------

	@Transactional
	public ExamSummary createExam(ExamRequest request) {
		Exam exam = new Exam(
				request.name().trim(),
				Slugs.uniqueFrom(request.name(), exams::existsBySlug),
				request.description(),
				request.iconUrl(),
				request.displayOrder());
		return ExamSummary.of(exams.save(exam), 0);
	}

	@Transactional
	public ExamSummary updateExam(Long id, ExamRequest request) {
		Exam exam = exams.findById(id).orElseThrow(() -> NotFoundException.of("Exam", id));
		// The slug is deliberately left alone: it is already in published URLs.
		exam.setName(request.name().trim());
		exam.setDescription(request.description());
		exam.setIconUrl(request.iconUrl());
		exam.setDisplayOrder(request.displayOrder());
		return ExamSummary.of(exam, exam.getTopics().size());
	}

	@Transactional
	public void deleteExam(Long id) {
		Exam exam = exams.findById(id).orElseThrow(() -> NotFoundException.of("Exam", id));
		// Only the join rows go; the topics themselves belong to other exams too.
		exams.delete(exam);
	}

	@Transactional
	public ExamDetail setExamTopics(Long examId, List<Long> topicIds) {
		Exam exam = exams.findById(examId).orElseThrow(() -> NotFoundException.of("Exam", examId));

		Set<Long> requested = new LinkedHashSet<>(topicIds);
		List<Topic> found = topics.findAllById(requested);
		if (found.size() != requested.size()) {
			Set<Long> foundIds = found.stream().map(Topic::getId).collect(java.util.stream.Collectors.toSet());
			Set<Long> missing = new LinkedHashSet<>(requested);
			missing.removeAll(foundIds);
			throw new NotFoundException("No topic with id " + missing);
		}

		exam.replaceTopics(new LinkedHashSet<>(found));
		return getExamDetail(exam);
	}

	@Transactional
	public TopicNode createTopic(TopicRequest request) {
		Topic parent = resolveParent(request.parentId());
		Topic topic = new Topic(
				request.name().trim(),
				Slugs.uniqueFrom(request.name(), topics::existsBySlug),
				parent,
				request.description(),
				request.displayOrder());
		return TopicNode.leaf(topics.save(topic));
	}

	@Transactional
	public TopicNode updateTopic(Long id, TopicRequest request) {
		Topic topic = topics.findById(id).orElseThrow(() -> NotFoundException.of("Topic", id));

		Topic parent = resolveParent(request.parentId());
		if (parent != null) {
			assertNoCycle(topic, parent);
		}

		topic.setName(request.name().trim());
		topic.setParent(parent);
		topic.setDescription(request.description());
		topic.setDisplayOrder(request.displayOrder());
		return TopicNode.leaf(topic);
	}

	@Transactional
	public void deleteTopic(Long id) {
		Topic topic = topics.findById(id).orElseThrow(() -> NotFoundException.of("Topic", id));

		// The foreign key would cascade and take the whole subtree with it. Deleting
		// a branch by accident is not something an admin can undo, so make it explicit.
		if (!topics.findChildren(id).isEmpty()) {
			throw new ConflictException(
					"This topic has sub-topics. Move or delete them first.");
		}

		topics.delete(topic);
	}

	private ExamDetail getExamDetail(Exam exam) {
		return new ExamDetail(
				exam.getId(),
				exam.getName(),
				exam.getSlug(),
				exam.getDescription(),
				exam.getIconUrl(),
				exam.getDisplayOrder(),
				buildTree(topics.findAllForExam(exam.getSlug())));
	}

	private Topic resolveParent(Long parentId) {
		if (parentId == null) {
			return null;
		}
		return topics.findById(parentId)
				.orElseThrow(() -> NotFoundException.of("Parent topic", parentId));
	}

	/**
	 * Walks up from the proposed parent. If we reach the topic being moved, the move
	 * would create a loop that no database constraint catches and that would make the
	 * tree builder recurse forever.
	 */
	private void assertNoCycle(Topic topic, Topic proposedParent) {
		Set<Long> seen = new HashSet<>();
		Topic cursor = proposedParent;
		while (cursor != null) {
			if (cursor.getId().equals(topic.getId())) {
				throw new ConflictException("A topic cannot be moved beneath itself");
			}
			if (!seen.add(cursor.getId())) {
				throw new ConflictException("The topic tree already contains a loop");
			}
			cursor = cursor.getParent();
		}
	}

}
