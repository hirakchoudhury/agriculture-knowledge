package com.agriknowledge.path;

import com.agriknowledge.material.Material;
import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * How far a learner has got with one piece of material.
 *
 * <p>Progress belongs to the learner and the material rather than to a path, so an
 * article read inside one path counts as read everywhere else it appears.
 *
 * <p>The table has a composite primary key, but the entity uses a surrogate id with
 * a unique constraint: identical guarantee, far less JPA ceremony.
 */
@Entity
@Table(
		name = "material_progress",
		uniqueConstraints = @UniqueConstraint(name = "material_progress_once",
				columnNames = { "user_id", "material_id" }))
public class MaterialProgress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "material_id", nullable = false)
	private Material material;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProgressStatus status = ProgressStatus.IN_PROGRESS;

	@Column(name = "last_position_seconds")
	private Integer lastPositionSeconds;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected MaterialProgress() {
		// for JPA
	}

	public MaterialProgress(User user, Material material) {
		this.user = user;
		this.material = material;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public void markCompleted() {
		this.status = ProgressStatus.COMPLETED;
		// Set only on the first completion, so re-reading something does not keep
		// moving the date and reshuffling any "recently finished" view.
		if (this.completedAt == null) {
			this.completedAt = Instant.now();
		}
		this.updatedAt = Instant.now();
	}

	public void markInProgress() {
		this.status = ProgressStatus.IN_PROGRESS;
		this.completedAt = null;
		this.updatedAt = Instant.now();
	}

	public void recordPosition(Integer seconds) {
		if (seconds != null && seconds >= 0) {
			this.lastPositionSeconds = seconds;
			this.updatedAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Material getMaterial() {
		return material;
	}

	public ProgressStatus getStatus() {
		return status;
	}

	public Integer getLastPositionSeconds() {
		return lastPositionSeconds;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public boolean isCompleted() {
		return status == ProgressStatus.COMPLETED;
	}

}
