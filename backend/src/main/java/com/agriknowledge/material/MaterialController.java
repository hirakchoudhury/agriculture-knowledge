package com.agriknowledge.material;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.material.dto.MaterialDetail;
import com.agriknowledge.material.dto.MaterialSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public reading. Only published material is ever returned here. */
@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

	private final MaterialService materials;

	public MaterialController(MaterialService materials) {
		this.materials = materials;
	}

	@GetMapping
	PageResponse<MaterialSummary> list(
			@RequestParam(required = false) MaterialType type,
			@RequestParam(required = false) Difficulty difficulty,
			@RequestParam(required = false) Long topicId,
			@RequestParam(required = false) Long examId,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size,
			@RequestParam(defaultValue = "newest") String sort) {

		// Status is fixed, not a parameter: a query string must never be able to
		// surface drafts on the public endpoint.
		return materials.search(MaterialStatus.PUBLISHED, type, difficulty, topicId, examId,
				q, page, size, sort);
	}

	@GetMapping("/{slug}")
	MaterialDetail get(@PathVariable String slug, @AuthenticationPrincipal AuthPrincipal principal) {
		// Admins may follow a link to their own draft; everyone else gets a 404.
		return materials.getBySlug(slug, principal != null && principal.isAdmin());
	}

}
