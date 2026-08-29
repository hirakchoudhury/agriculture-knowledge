package com.agriknowledge.quiz;

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
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "questions")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Column(nullable = false, length = 2000)
	private String text;

	/** Revealed only after submission. */
	@Column(length = 2000)
	private String explanation;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	/**
	 * Decimal because negative marking in Indian competitive exams is routinely
	 * fractional: a quarter mark off for a wrong answer is the usual convention.
	 */
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal marks = BigDecimal.ONE;

	/**
	 * Subtracted for a wrong answer. Stored as a positive number and subtracted at
	 * scoring time, so the sign convention lives in exactly one place.
	 */
	@Column(name = "negative_marks", nullable = false, precision = 5, scale = 2)
	private BigDecimal negativeMarks = BigDecimal.ZERO;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	/**
	 * Loaded in batches rather than join-fetched alongside the questions. Hibernate
	 * refuses to join-fetch two list collections in one query (MultipleBagFetch),
	 * and BatchSize gets the options for a whole quiz in one extra select instead
	 * of one per question.
	 */
	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true,
			fetch = FetchType.LAZY)
	@OrderBy("displayOrder asc, id asc")
	@BatchSize(size = 200)
	private List<QuestionOption> options = new ArrayList<>();

	protected Question() {
		// for JPA
	}

	public Question(String text, String explanation, String imageUrl,
			BigDecimal marks, BigDecimal negativeMarks, int displayOrder) {
		this.text = text;
		this.explanation = explanation;
		this.imageUrl = imageUrl;
		this.marks = marks;
		this.negativeMarks = negativeMarks;
		this.displayOrder = displayOrder;
	}

	public void addOption(QuestionOption option) {
		options.add(option);
		option.setQuestion(this);
	}

	/** The correct option. Empty only if the question was saved mis-configured. */
	public Optional<QuestionOption> correctOption() {
		return options.stream().filter(QuestionOption::isCorrect).findFirst();
	}

	public Long getId() {
		return id;
	}

	public Quiz getQuiz() {
		return quiz;
	}

	void setQuiz(Quiz quiz) {
		this.quiz = quiz;
	}

	public String getText() {
		return text;
	}

	public String getExplanation() {
		return explanation;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getMarks() {
		return marks;
	}

	public BigDecimal getNegativeMarks() {
		return negativeMarks;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public List<QuestionOption> getOptions() {
		return options;
	}

}
