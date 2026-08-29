package com.agriknowledge.quiz.admin;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.quiz.QuizAdminService;
import com.agriknowledge.quiz.dto.AdminQuizDetail;
import com.agriknowledge.quiz.dto.QuestionsRequest;
import com.agriknowledge.quiz.dto.QuizRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/quizzes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuizController {

	private final QuizAdminService quizzes;

	public AdminQuizController(QuizAdminService quizzes) {
		this.quizzes = quizzes;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AdminQuizDetail create(@Valid @RequestBody QuizRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return quizzes.create(request, principal.userId());
	}

	@GetMapping("/{id}")
	AdminQuizDetail get(@PathVariable Long id) {
		return quizzes.get(id);
	}

	@PutMapping("/{id}")
	AdminQuizDetail update(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
		return quizzes.update(id, request);
	}

	/**
	 * Replaces every question in one call, which is what makes pasting in fifty
	 * questions a single request rather than fifty.
	 */
	@PutMapping("/{id}/questions")
	AdminQuizDetail replaceQuestions(@PathVariable Long id,
			@Valid @RequestBody QuestionsRequest request) {
		return quizzes.replaceQuestions(id, request);
	}

}
