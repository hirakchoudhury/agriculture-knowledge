package com.agriknowledge.material;

import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * A PDF, currently a previous year paper.
 *
 * <p>The bytes are not here. This row holds the storage key and enough metadata
 * to render a card and a download without touching the object store.
 */
@Entity
@Table(name = "documents")
@DiscriminatorValue("DOCUMENT")
@PrimaryKeyJoinColumn(name = "material_id")
public class Document extends Material {

	/**
	 * Opaque key inside the bucket. Deliberately never exposed: downloads go
	 * through a short-lived signed URL, so knowing the key buys nothing.
	 */
	@Column(name = "storage_key", nullable = false, unique = true, length = 500)
	private String storageKey;

	/** The uploaded filename, used to name the download rather than the key. */
	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType = "application/pdf";

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "page_count")
	private Integer pageCount;

	/** The axis people browse previous year papers by. Null if not year-specific. */
	@Column(name = "paper_year")
	private Integer paperYear;

	protected Document() {
		// for JPA
	}

	public Document(String title, String slug, String summary, String thumbnailUrl,
			Difficulty difficulty, User author, String storageKey, String originalName,
			String contentType, long sizeBytes, Integer pageCount, Integer paperYear) {
		super(title, slug, summary, thumbnailUrl, difficulty, author);
		this.storageKey = storageKey;
		this.originalName = originalName;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.pageCount = pageCount;
		this.paperYear = paperYear;
	}

	@Override
	public MaterialType getType() {
		return MaterialType.DOCUMENT;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public Integer getPaperYear() {
		return paperYear;
	}

	public void setPaperYear(Integer paperYear) {
		this.paperYear = paperYear;
	}

}
