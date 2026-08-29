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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
		name = "material_likes",
		uniqueConstraints = @UniqueConstraint(name = "material_likes_once",
				columnNames = { "user_id", "material_id" }))
public class MaterialLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "material_id", nullable = false)
	private Material material;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MaterialLike() {
		// for JPA
	}

	public MaterialLike(User user, Material material) {
		this.user = user;
		this.material = material;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

}
