package com.agriknowledge.engagement;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.engagement.dto.CommentRequest;
import com.agriknowledge.engagement.dto.CommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Editing and removing a comment, addressed by the comment's own id rather than
 * through its material: the client already holds the id and the material adds nothing.
 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

	private final CommentService comments;

	public CommentController(CommentService comments) {
		this.comments = comments;
	}

	@PatchMapping("/{id}")
	CommentResponse edit(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return comments.edit(id, request, principal);
	}

	/** The author may remove their own; an admin may remove any. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
		comments.delete(id, principal);
	}

}
