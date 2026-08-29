package com.agriknowledge.path;

import com.agriknowledge.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A learner-built sequence of material. Private to its owner. */
@Entity
@Table(name = "learning_paths")
public class LearningPath {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User owner;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 1000)
	private String description;

	@Column(name = "is_public", nullable = false)
	private boolean shared;

	@OneToMany(mappedBy = "path", cascade = CascadeType.ALL, orphanRemoval = true,
			fetch = FetchType.LAZY)
	@OrderBy("displayOrder asc, id asc")
	private List<LearningPathItem> items = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected LearningPath() {
		// for JPA
	}

	public LearningPath(User owner, String title, String description) {
		this.owner = owner;
		this.title = title;
		this.description = description;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public void addItem(LearningPathItem item) {
		items.add(item);
		item.setPath(this);
	}

	public boolean removeItem(Long itemId) {
		return items.removeIf(item -> item.getId().equals(itemId));
	}

	/** Appends after whatever is currently last, leaving existing order untouched. */
	public int nextDisplayOrder() {
		return items.stream()
				.mapToInt(LearningPathItem::getDisplayOrder)
				.max()
				.orElse(-1) + 1;
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public List<LearningPathItem> getItems() {
		return items;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
