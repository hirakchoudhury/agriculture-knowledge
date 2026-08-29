package com.agriknowledge.engagement;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.engagement.dto.CommentRequest;
import com.agriknowledge.engagement.dto.CommentResponse;
import com.agriknowledge.material.Material;
import com.agriknowledge.material.MaterialRepository;
import com.agriknowledge.user.User;
import com.agriknowledge.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

	private static final int MAX_PAGE_SIZE = 50;

	private final CommentRepository comments;
	private final MaterialRepository materials;
	private final UserRepository users;
	private final CommentRateLimiter rateLimiter;

	public CommentService(CommentRepository comments, MaterialRepository materials,
			UserRepository users, CommentRateLimiter rateLimiter) {
		this.comments = comments;
		this.materials = materials;
		this.users = users;
		this.rateLimiter = rateLimiter;
	}

	@Transactional(readOnly = true)
	public PageResponse<CommentResponse> list(Long materialId, int page, int size, Long viewerId) {
		Page<Comment> roots = comments.findRoots(
				materialId,
				PageRequest.of(Math.max(0, page), Math.clamp(size, 1, MAX_PAGE_SIZE),
						Sort.by(Sort.Direction.DESC, "createdAt")));

		// Replies for the whole page in one query rather than one per root comment.
		Map<Long, List<Comment>> repliesByParent = new HashMap<>();
		List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
		if (!rootIds.isEmpty()) {
			for (Comment reply : comments.findRepliesTo(rootIds)) {
				repliesByParent.computeIfAbsent(reply.getParentId(), key -> new ArrayList<>()).add(reply);
			}
		}

		List<CommentResponse> content = new ArrayList<>();
		for (Comment root : roots.getContent()) {
			// The query keeps a deleted comment only while it still has a visible
			// reply, so the conversation underneath it does not lose its anchor.
			List<Comment> replies = repliesByParent.getOrDefault(root.getId(), List.of());

			content.add(toResponse(root, viewerId,
					replies.stream().map(reply -> toResponse(reply, viewerId, List.of())).toList()));
		}

		return new PageResponse<>(
				content,
				roots.getNumber(),
				roots.getSize(),
				roots.getTotalElements(),
				roots.getTotalPages(),
				roots.isFirst(),
				roots.isLast());
	}

	@Transactional
	public CommentResponse create(Long materialId, CommentRequest request, Long userId) {
		rateLimiter.checkAllowed(userId);

		try {
			Material material = materials.findById(materialId)
					.orElseThrow(() -> NotFoundException.of("Material", materialId));

			if (!material.isPublished()) {
				throw new BadRequestException("That material is not published");
			}

			Comment parent = resolveParent(request.parentId(), materialId);
			User author = users.getReferenceById(userId);

			Comment comment = comments.save(new Comment(material, author, parent, request.body().trim()));
			materials.adjustCommentCount(materialId, 1);

			return toResponse(comment, userId, List.of());
		}
		catch (RuntimeException ex) {
			// A rejected request should not use up the caller's allowance.
			rateLimiter.refund(userId);
			throw ex;
		}
	}

	@Transactional
	public CommentResponse edit(Long commentId, CommentRequest request, AuthPrincipal viewer) {
		Comment comment = load(commentId);

		// Editing is the author's alone. An admin who disagrees with a comment can
		// remove it, but putting words in someone's mouth is a different thing.
		if (!comment.getAuthor().getId().equals(viewer.userId())) {
			throw new AccessDeniedException("You can only edit your own comments");
		}
		if (comment.isDeleted()) {
			throw new BadRequestException("That comment has been deleted");
		}

		comment.edit(request.body().trim());
		return toResponse(comment, viewer.userId(), List.of());
	}

	@Transactional
	public void delete(Long commentId, AuthPrincipal viewer) {
		Comment comment = load(commentId);

		if (!comment.getAuthor().getId().equals(viewer.userId()) && !viewer.isAdmin()) {
			throw new AccessDeniedException("You can only delete your own comments");
		}
		if (comment.isDeleted()) {
			return;
		}

		comment.softDelete();
		materials.adjustCommentCount(comment.getMaterial().getId(), -1);
	}

	private Comment resolveParent(Long parentId, Long materialId) {
		if (parentId == null) {
			return null;
		}

		Comment parent = load(parentId);

		// Threads are one level deep. Without this, replies to replies would nest
		// without bound and the UI would have no sensible way to render them.
		if (parent.isReply()) {
			throw new BadRequestException("Replies cannot themselves be replied to");
		}

		// Otherwise a comment could be attached to a thread on a different page.
		if (!parent.getMaterial().getId().equals(materialId)) {
			throw new BadRequestException("That comment belongs to different material");
		}

		return parent;
	}

	private Comment load(Long commentId) {
		return comments.findById(commentId)
				.orElseThrow(() -> NotFoundException.of("Comment", commentId));
	}

	private CommentResponse toResponse(Comment comment, Long viewerId, List<CommentResponse> replies) {
		User author = comment.getAuthor();
		return new CommentResponse(
				comment.getId(),
				comment.getBody(),
				// A deleted comment reveals nothing about who wrote it.
				comment.isDeleted() ? null : author.getId(),
				comment.isDeleted() ? null : author.getName(),
				comment.isDeleted() ? null : author.getAvatarUrl(),
				comment.getCreatedAt(),
				comment.getEditedAt(),
				comment.isDeleted(),
				!comment.isDeleted() && viewerId != null && author.getId().equals(viewerId),
				comment.getParentId(),
				replies);
	}

}
