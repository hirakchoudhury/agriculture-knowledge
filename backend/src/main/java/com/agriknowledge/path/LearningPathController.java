package com.agriknowledge.path;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.path.dto.AddItemRequest;
import com.agriknowledge.path.dto.PathDetail;
import com.agriknowledge.path.dto.PathRequest;
import com.agriknowledge.path.dto.PathSummary;
import com.agriknowledge.path.dto.ProgressRequest;
import com.agriknowledge.path.dto.ProgressResponse;
import com.agriknowledge.path.dto.ReorderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Learner-owned paths. Everything here is private to the caller. */
@RestController
@RequestMapping("/api/v1")
public class LearningPathController {

	private final LearningPathService pathService;

	public LearningPathController(LearningPathService pathService) {
		this.pathService = pathService;
	}

	@GetMapping("/learning-paths")
	List<PathSummary> list(@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.listMine(principal.userId());
	}

	@PostMapping("/learning-paths")
	@ResponseStatus(HttpStatus.CREATED)
	PathDetail create(@Valid @RequestBody PathRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.create(request, principal.userId());
	}

	@GetMapping("/learning-paths/{id}")
	PathDetail get(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.get(id, principal.userId());
	}

	@PutMapping("/learning-paths/{id}")
	PathDetail update(@PathVariable Long id, @Valid @RequestBody PathRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.update(id, request, principal.userId());
	}

	@DeleteMapping("/learning-paths/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
		pathService.delete(id, principal.userId());
	}

	@PostMapping("/learning-paths/{id}/items")
	PathDetail addItem(@PathVariable Long id, @Valid @RequestBody AddItemRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.addItem(id, request, principal.userId());
	}

	@DeleteMapping("/learning-paths/{id}/items/{itemId}")
	PathDetail removeItem(@PathVariable Long id, @PathVariable Long itemId,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.removeItem(id, itemId, principal.userId());
	}

	/** Takes the complete order, so a dropped request cannot half-apply it. */
	@PutMapping("/learning-paths/{id}/items/order")
	PathDetail reorder(@PathVariable Long id, @Valid @RequestBody ReorderRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.reorder(id, request.itemIds(), principal.userId());
	}

	@GetMapping("/progress/{materialId}")
	ProgressResponse getProgress(@PathVariable Long materialId,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.getProgress(materialId, principal.userId());
	}

	@PutMapping("/progress/{materialId}")
	ProgressResponse setProgress(@PathVariable Long materialId,
			@Valid @RequestBody ProgressRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return pathService.setProgress(materialId, request, principal.userId());
	}

}
