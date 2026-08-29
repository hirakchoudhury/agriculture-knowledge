package com.agriknowledge.path;

import com.agriknowledge.material.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_path_items")
public class LearningPathItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "path_id", nullable = false)
	private LearningPath path;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "material_id", nullable = false)
	private Material material;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(length = 500)
	private String note;

	protected LearningPathItem() {
		// for JPA
	}

	public LearningPathItem(Material material, int displayOrder, String note) {
		this.material = material;
		this.displayOrder = displayOrder;
		this.note = note;
	}

	public Long getId() {
		return id;
	}

	public LearningPath getPath() {
		return path;
	}

	void setPath(LearningPath path) {
		this.path = path;
	}

	public Material getMaterial() {
		return material;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

}
