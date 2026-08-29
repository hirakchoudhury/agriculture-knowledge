package com.agriknowledge.quiz;

import com.agriknowledge.material.Difficulty;
import com.agriknowledge.material.Material;
import com.agriknowledge.material.MaterialType;
import com.agriknowledge.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * A quiz is a third kind of material, so it joins the existing hierarchy through
 * materials.id exactly as articles and videos do. It therefore appears in the same
 * feed, carries the same tags, and uses the same draft/publish workflow for free.
 */
@Entity
@Table(name = "quizzes")
@DiscriminatorValue("QUIZ")
@PrimaryKeyJoinColumn(name = "material_id")
public class Quiz extends Material {

	/** Null means untimed. */
	@Column(name = "time_limit_seconds")
	private Integer timeLimitSeconds;

	@Column(name = "pass_percentage", nullable = false)
	private int passPercentage = 60;

	@Column(name = "shuffle_questions", nullable = false)
	private boolean shuffleQuestions;

	/**
	 * Owned by the quiz: deleting a quiz takes its questions with it, and removing
	 * a question from this list deletes the row rather than orphaning it.
	 */
	@OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true,
			fetch = FetchType.LAZY)
	@OrderBy("displayOrder asc, id asc")
	private List<Question> questions = new ArrayList<>();

	protected Quiz() {
		// for JPA
	}

	public Quiz(String title, String slug, String summary, String thumbnailUrl,
			Difficulty difficulty, User author, Integer timeLimitSeconds,
			int passPercentage, boolean shuffleQuestions) {
		super(title, slug, summary, thumbnailUrl, difficulty, author);
		this.timeLimitSeconds = timeLimitSeconds;
		this.passPercentage = passPercentage;
		this.shuffleQuestions = shuffleQuestions;
	}

	@Override
	public MaterialType getType() {
		return MaterialType.QUIZ;
	}

	public Integer getTimeLimitSeconds() {
		return timeLimitSeconds;
	}

	public void setTimeLimitSeconds(Integer timeLimitSeconds) {
		this.timeLimitSeconds = timeLimitSeconds;
	}

	public int getPassPercentage() {
		return passPercentage;
	}

	public void setPassPercentage(int passPercentage) {
		this.passPercentage = passPercentage;
	}

	public boolean isShuffleQuestions() {
		return shuffleQuestions;
	}

	public void setShuffleQuestions(boolean shuffleQuestions) {
		this.shuffleQuestions = shuffleQuestions;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public void addQuestion(Question question) {
		questions.add(question);
		question.setQuiz(this);
	}

	public void clearQuestions() {
		questions.clear();
	}

}
