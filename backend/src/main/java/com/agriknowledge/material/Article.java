package com.agriknowledge.material;

import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles")
@DiscriminatorValue("ARTICLE")
@PrimaryKeyJoinColumn(name = "material_id")
public class Article extends Material {

	/** Already sanitised when it gets here. Never store what the editor sent verbatim. */
	@Column(name = "body_html", nullable = false, columnDefinition = "text")
	private String bodyHtml;

	@Column(name = "reading_minutes", nullable = false)
	private int readingMinutes = 1;

	protected Article() {
		// for JPA
	}

	public Article(String title, String slug, String summary, String thumbnailUrl,
			Difficulty difficulty, User author, String bodyHtml, int readingMinutes) {
		super(title, slug, summary, thumbnailUrl, difficulty, author);
		this.bodyHtml = bodyHtml;
		this.readingMinutes = readingMinutes;
	}

	@Override
	public MaterialType getType() {
		return MaterialType.ARTICLE;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public void setBodyHtml(String bodyHtml) {
		this.bodyHtml = bodyHtml;
	}

	public int getReadingMinutes() {
		return readingMinutes;
	}

	public void setReadingMinutes(int readingMinutes) {
		this.readingMinutes = readingMinutes;
	}

}
