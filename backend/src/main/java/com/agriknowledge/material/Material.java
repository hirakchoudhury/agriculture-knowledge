package com.agriknowledge.material;

import com.agriknowledge.catalog.Exam;
import com.agriknowledge.catalog.Topic;
import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Everything an article, a video and (from phase 6) a quiz have in common.
 *
 * <p>JOINED inheritance rather than a single wide table: the shared row keeps the
 * feed to one query, while each type keeps its own columns without a pile of
 * nullable ones. The trade-off is that polymorphic queries join every subtype
 * table, which is why list endpoints use projections instead of loading entities.
 */
@Entity
@Table(name = "materials")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Material {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 250)
	private String title;

	@Column(nullable = false, unique = true, length = 280)
	private String slug;

	/**
	 * The discriminator again, mapped read-only purely so queries can filter by type
	 * with a bindable parameter. JPQL's type() operator cannot take one, and writes
	 * still go through the discriminator, so this can never disagree with the class.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "type", insertable = false, updatable = false, nullable = false, length = 20)
	private MaterialType typeColumn;

	@Column(length = 500)
	private String summary;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Difficulty difficulty = Difficulty.BEGINNER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MaterialStatus status = MaterialStatus.DRAFT;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "like_count", nullable = false)
	private long likeCount;

	@Column(name = "comment_count", nullable = false)
	private long commentCount;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "material_topics",
			joinColumns = @JoinColumn(name = "material_id"),
			inverseJoinColumns = @JoinColumn(name = "topic_id"))
	private Set<Topic> topics = new LinkedHashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "material_exams",
			joinColumns = @JoinColumn(name = "material_id"),
			inverseJoinColumns = @JoinColumn(name = "exam_id"))
	private Set<Exam> exams = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Material() {
		// for JPA
	}

	protected Material(String title, String slug, String summary, String thumbnailUrl,
			Difficulty difficulty, User author) {
		this.title = title;
		this.slug = slug;
		this.summary = summary;
		this.thumbnailUrl = thumbnailUrl;
		this.difficulty = difficulty;
		this.author = author;
	}

	/** Implemented per subclass rather than stored: the discriminator already holds it. */
	public abstract MaterialType getType();

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

	/**
	 * Publishing stamps the date only the first time. Re-publishing something that
	 * was archived keeps its original date, so the feed order does not jump around.
	 */
	public void publish() {
		this.status = MaterialStatus.PUBLISHED;
		if (this.publishedAt == null) {
			this.publishedAt = Instant.now();
		}
	}

	public void unpublish(MaterialStatus target) {
		if (target == MaterialStatus.PUBLISHED) {
			throw new IllegalArgumentException("Use publish() to publish");
		}
		this.status = target;
	}

	public boolean isPublished() {
		return status == MaterialStatus.PUBLISHED;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSlug() {
		return slug;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	public MaterialStatus getStatus() {
		return status;
	}

	public User getAuthor() {
		return author;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public long getViewCount() {
		return viewCount;
	}

	public long getLikeCount() {
		return likeCount;
	}

	public long getCommentCount() {
		return commentCount;
	}

	public Set<Topic> getTopics() {
		return topics;
	}

	public void replaceTopics(Set<Topic> replacement) {
		this.topics.clear();
		this.topics.addAll(replacement);
	}

	public Set<Exam> getExams() {
		return exams;
	}

	public void replaceExams(Set<Exam> replacement) {
		this.exams.clear();
		this.exams.addAll(replacement);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
