package com.agriknowledge.quiz;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.quiz.dto.AttemptResult;
import com.agriknowledge.quiz.dto.AttemptSummary;
import com.agriknowledge.quiz.dto.AttemptView;
import com.agriknowledge.quiz.dto.QuizSummary;
import com.agriknowledge.quiz.dto.SubmitAttemptRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Taking a quiz. Nothing here ever reveals which option is correct before submission. */
@RestController
@RequestMapping("/api/v1")
public class QuizController {

	private final AttemptService attempts;

	public QuizController(AttemptService attempts) {
		this.attempts = attempts;
	}

	/** Readable signed out, so a visitor can see what they would be attempting. */
	@GetMapping("/quizzes/{slug}")
	QuizSummary summary(@PathVariable String slug, @AuthenticationPrincipal AuthPrincipal principal) {
		return attempts.summary(slug, principal == null ? null : principal.userId());
	}

	@PostMapping("/quizzes/{slug}/attempts")
	@ResponseStatus(HttpStatus.CREATED)
	AttemptView start(@PathVariable String slug, @AuthenticationPrincipal AuthPrincipal principal) {
		return attempts.start(slug, principal.userId());
	}

	@PostMapping("/attempts/{id}/submit")
	AttemptResult submit(@PathVariable Long id, @Valid @RequestBody SubmitAttemptRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return attempts.submit(id, request, principal.userId());
	}

	@GetMapping("/attempts/{id}")
	AttemptResult review(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
		return attempts.review(id, principal.userId());
	}

	@GetMapping("/users/me/attempts")
	PageResponse<AttemptSummary> history(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return attempts.history(principal.userId(), page, size);
	}

}
