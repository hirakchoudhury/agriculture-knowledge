package com.agriknowledge.material.admin;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.material.Difficulty;
import com.agriknowledge.material.MaterialService;
import com.agriknowledge.material.MaterialStatus;
import com.agriknowledge.material.MaterialType;
import com.agriknowledge.material.dto.ArticleRequest;
import com.agriknowledge.material.dto.MaterialDetail;
import com.agriknowledge.material.dto.MaterialStatusRequest;
import com.agriknowledge.material.dto.MaterialSummary;
import com.agriknowledge.material.dto.VideoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/materials")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMaterialController {

	private final MaterialService materials;

	public AdminMaterialController(MaterialService materials) {
		this.materials = materials;
	}

	/** Unlike the public list, this one can see drafts and archived items. */
	@GetMapping
	PageResponse<MaterialSummary> list(
			@RequestParam(required = false) MaterialStatus status,
			@RequestParam(required = false) MaterialType type,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return materials.search(status, type, null, null, null, q, page, size, "newest");
	}

	@PostMapping("/articles")
	@ResponseStatus(HttpStatus.CREATED)
	MaterialDetail createArticle(@Valid @RequestBody ArticleRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return materials.createArticle(request, principal.userId());
	}

	@PostMapping("/videos")
	@ResponseStatus(HttpStatus.CREATED)
	MaterialDetail createVideo(@Valid @RequestBody VideoRequest request,
			@AuthenticationPrincipal AuthPrincipal principal) {
		return materials.createVideo(request, principal.userId());
	}

	@PutMapping("/articles/{id}")
	MaterialDetail updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
		return materials.updateArticle(id, request);
	}

	@PutMapping("/videos/{id}")
	MaterialDetail updateVideo(@PathVariable Long id, @Valid @RequestBody VideoRequest request) {
		return materials.updateVideo(id, request);
	}

	@PatchMapping("/{id}/status")
	MaterialDetail setStatus(@PathVariable Long id, @Valid @RequestBody MaterialStatusRequest request) {
		return materials.setStatus(id, request.status());
	}

	/** Archives rather than deletes: comments and progress reference this row. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable Long id) {
		materials.archive(id);
	}

}
