package com.agriknowledge.engagement;

import com.agriknowledge.material.Material;
import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

	/** Shown in place of a soft-deleted comment that still has replies under it. */
	public static final String DELETED_PLACEHOLDER = "[deleted]";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "material_id", nullable = false)
	private Material material;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User author;

	/** Null for a top-level comment. Never points at another reply. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Comment parent;

	@Column(nullable = false, length = 4000)
	private String body;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "edited_at")
	private Instant editedAt;

	protected Comment() {
		// for JPA
	}

	public Comment(Material material, User author, Comment parent, String body) {
		this.material = material;
		this.author = author;
		this.parent = parent;
		this.body = body;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public void edit(String newBody) {
		this.body = newBody;
		this.editedAt = Instant.now();
	}

	/**
	 * Keeps the row so replies underneath it survive, but drops the text. Storing
	 * the original would mean a "deleted" comment was still one query away.
	 */
	public void softDelete() {
		this.deleted = true;
		this.body = DELETED_PLACEHOLDER;
		this.editedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Material getMaterial() {
		return material;
	}

	public User getAuthor() {
		return author;
	}

	public Comment getParent() {
		return parent;
	}

	public Long getParentId() {
		return parent == null ? null : parent.getId();
	}

	public boolean isReply() {
		return parent != null;
	}

	public String getBody() {
		return body;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getEditedAt() {
		return editedAt;
	}

}
