package com.agriknowledge.material;

import com.agriknowledge.auth.jwt.AuthPrincipal;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.material.dto.MaterialDetail;
import com.agriknowledge.material.dto.MaterialSummary;
import com.agriknowledge.material.storage.DocumentStorage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	private final DocumentStorage storage;

	public MaterialController(MaterialService materials, DocumentStorage storage) {
		this.materials = materials;
		this.storage = storage;
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
			@RequestParam(defaultValue = "newest") String sort,
			@AuthenticationPrincipal AuthPrincipal principal) {

		// Status is fixed, not a parameter: a query string must never be able to
		// surface drafts on the public endpoint.
		return materials.search(MaterialStatus.PUBLISHED, type, difficulty, topicId, examId,
				q, page, size, sort, principal == null ? null : principal.userId());
	}

	@GetMapping("/{slug}")
	MaterialDetail get(@PathVariable String slug, @AuthenticationPrincipal AuthPrincipal principal) {
		// Admins may follow a link to their own draft; everyone else gets a 404.
		return materials.getBySlug(slug, principal != null && principal.isAdmin(),
				principal == null ? null : principal.userId());
	}

	/**
	 * Downloads a PDF.
	 *
	 * <p>Redirects to a short-lived signed URL where the store can produce one, so
	 * the bytes travel from the object store to the reader without passing through
	 * this container at all. Only falls back to streaming for the local store,
	 * which cannot sign.
	 */
	@GetMapping("/{slug}/download")
	ResponseEntity<?> download(@PathVariable String slug) {
		MaterialService.DownloadTarget target = materials.downloadTarget(slug);

		if (target.signedUrl() != null) {
			return ResponseEntity.status(302).location(target.signedUrl()).build();
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(target.fileName()).build().toString())
				.body(new InputStreamResource(storage.open(target.storageKey())));
	}

}
