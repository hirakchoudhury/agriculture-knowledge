package com.agriknowledge.catalog.admin;

import com.agriknowledge.catalog.CatalogService;
import com.agriknowledge.catalog.dto.ExamDetail;
import com.agriknowledge.catalog.dto.ExamRequest;
import com.agriknowledge.catalog.dto.ExamSummary;
import com.agriknowledge.catalog.dto.ExamTopicsRequest;
import com.agriknowledge.catalog.dto.TopicNode;
import com.agriknowledge.catalog.dto.TopicRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guarded at the method layer rather than by URL pattern alone: a URL rule is easy
 * to sidestep by adding a route and forgetting the pattern, whereas this travels
 * with the code.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

	private final CatalogService catalog;

	public AdminCatalogController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@PostMapping("/exams")
	@ResponseStatus(HttpStatus.CREATED)
	ExamSummary createExam(@Valid @RequestBody ExamRequest request) {
		return catalog.createExam(request);
	}

	@PutMapping("/exams/{id}")
	ExamSummary updateExam(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
		return catalog.updateExam(id, request);
	}

	@DeleteMapping("/exams/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteExam(@PathVariable Long id) {
		catalog.deleteExam(id);
	}

	/** Replaces the exam's whole topic set, so repeating the call changes nothing. */
	@PutMapping("/exams/{id}/topics")
	ExamDetail setExamTopics(@PathVariable Long id, @Valid @RequestBody ExamTopicsRequest request) {
		return catalog.setExamTopics(id, request.topicIds());
	}

	@PostMapping("/topics")
	@ResponseStatus(HttpStatus.CREATED)
	TopicNode createTopic(@Valid @RequestBody TopicRequest request) {
		return catalog.createTopic(request);
	}

	@PutMapping("/topics/{id}")
	TopicNode updateTopic(@PathVariable Long id, @Valid @RequestBody TopicRequest request) {
		return catalog.updateTopic(id, request);
	}

	@DeleteMapping("/topics/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteTopic(@PathVariable Long id) {
		catalog.deleteTopic(id);
	}

}
