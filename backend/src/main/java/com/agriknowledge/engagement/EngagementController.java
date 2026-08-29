package com.agriknowledge.engagement;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.engagement.dto.CommentRequest;
import com.agriknowledge.engagement.dto.CommentResponse;
import com.agriknowledge.engagement.dto.LikeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Likes and comments, both hung off a material. */
@RestController
@RequestMapping("/api/v1/materials/{materialId}")
public class EngagementController {

	private final LikeService likes;
	private final CommentService comments;

	public EngagementController(LikeService likes, CommentService comments) {
		this.likes = likes;
		this.comments = comments;
	}

	/** Readable signed out, where it simply reports liked = false. */
	@GetMapping("/like")
	LikeResponse likeStatus(@PathVariable Long materialId,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return likes.status(materialId, principal == null ? null : principal.userId());
	}

	@PostMapping("/like")
	LikeResponse like(@PathVariable Long materialId, @AuthenticationPrincipal AuthPrincipal principal) {
		return likes.like(materialId, principal.userId());
	}

	@DeleteMapping("/like")
	LikeResponse unlike(@PathVariable Long materialId, @AuthenticationPrincipal AuthPrincipal principal) {
		return likes.unlike(materialId, principal.userId());
	}

	/** Readable signed out: the discussion is part of the page. */
	@GetMapping("/comments")
	PageResponse<CommentResponse> listComments(
			@PathVariable Long materialId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return comments.list(materialId, page, size, principal == null ? null : principal.userId());
	}

	@PostMapping("/comments")
	@ResponseStatus(HttpStatus.CREATED)
	CommentResponse addComment(@PathVariable Long materialId,
			@Valid @RequestBody CommentRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return comments.create(materialId, request, principal.userId());
	}

}
