package com.agriknowledge.catalog;

import com.agriknowledge.catalog.dto.ExamDetail;
import com.agriknowledge.catalog.dto.ExamSummary;
import com.agriknowledge.catalog.dto.TopicNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public catalogue. No authentication: browsing the syllabus is the shop window. */
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

	private final CatalogService catalog;

	public CatalogController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/exams")
	List<ExamSummary> listExams() {
		return catalog.listExams();
	}

	@GetMapping("/exams/{slug}")
	ExamDetail getExam(@PathVariable String slug) {
		return catalog.getExam(slug);
	}

	@GetMapping("/exams/{slug}/topics")
	List<TopicNode> getExamSyllabus(@PathVariable String slug) {
		return catalog.getExam(slug).syllabus();
	}

	@GetMapping("/topics")
	List<TopicNode> topicTree() {
		return catalog.topicTree();
	}

}
