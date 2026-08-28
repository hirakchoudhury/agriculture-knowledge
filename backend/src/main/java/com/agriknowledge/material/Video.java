package com.agriknowledge.material;

import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "videos")
@DiscriminatorValue("VIDEO")
@PrimaryKeyJoinColumn(name = "material_id")
public class Video extends Material {

	/** The bare 11-character id. Building URLs is the frontend's job. */
	@Column(name = "youtube_id", nullable = false, length = 20)
	private String youtubeId;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	protected Video() {
		// for JPA
	}

	public Video(String title, String slug, String summary, String thumbnailUrl,
			Difficulty difficulty, User author, String youtubeId, Integer durationSeconds) {
		super(title, slug, summary, thumbnailUrl, difficulty, author);
		this.youtubeId = youtubeId;
		this.durationSeconds = durationSeconds;
	}

	@Override
	public MaterialType getType() {
		return MaterialType.VIDEO;
	}

	public String getYoutubeId() {
		return youtubeId;
	}

	public void setYoutubeId(String youtubeId) {
		this.youtubeId = youtubeId;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

}
