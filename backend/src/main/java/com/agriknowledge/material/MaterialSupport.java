package com.agriknowledge.material;

import com.agriknowledge.catalog.Exam;
import com.agriknowledge.catalog.ExamRepository;
import com.agriknowledge.catalog.Topic;
import com.agriknowledge.catalog.TopicRepository;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.common.Slugs;
import com.agriknowledge.user.User;
import com.agriknowledge.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The parts of creating a material that every type shares: a unique slug, the
 * author, and resolving tag ids to entities.
 *
 * <p>Extracted so quizzes can be created the same way articles and videos are,
 * without either duplicating the rules or the material package having to reach
 * back into the quiz package.
 */
@Component
public class MaterialSupport {

	private final MaterialRepository materials;
	private final TopicRepository topics;
	private final ExamRepository exams;
	private final UserRepository users;

	public MaterialSupport(MaterialRepository materials, TopicRepository topics,
			ExamRepository exams, UserRepository users) {
		this.materials = materials;
		this.topics = topics;
		this.exams = exams;
		this.users = users;
	}

	public String uniqueSlug(String title) {
		return Slugs.uniqueFrom(title, materials::existsBySlug);
	}

	public User author(Long authorId) {
		return users.findById(authorId)
				.orElseThrow(() -> NotFoundException.of("Account", authorId));
	}

	public void applyTags(Material material, List<Long> topicIds, List<Long> examIds) {
		material.replaceTopics(resolveTopics(topicIds));
		material.replaceExams(resolveExams(examIds));
	}

	public Set<Topic> resolveTopics(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Set.of();
		}
		Set<Long> wanted = new LinkedHashSet<>(ids);
		List<Topic> found = topics.findAllById(wanted);
		if (found.size() != wanted.size()) {
			throw new NotFoundException("Unknown topic id in " + wanted);
		}
		return new LinkedHashSet<>(found);
	}

	public Set<Exam> resolveExams(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Set.of();
		}
		Set<Long> wanted = new LinkedHashSet<>(ids);
		List<Exam> found = exams.findAllById(wanted);
		if (found.size() != wanted.size()) {
			throw new NotFoundException("Unknown exam id in " + wanted);
		}
		return new LinkedHashSet<>(found);
	}

}
