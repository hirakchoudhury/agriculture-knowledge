package com.agriknowledge.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One choice for a question.
 *
 * <p>The {@code correct} flag must never reach a learner who has not submitted.
 * That is enforced structurally: the DTO used for taking a quiz has no such field
 * at all, so there is nothing to remember to strip.
 */
@Entity
@Table(name = "question_options")
public class QuestionOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(nullable = false, length = 1000)
	private String text;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected QuestionOption() {
		// for JPA
	}

	public QuestionOption(String text, boolean correct, int displayOrder) {
		this.text = text;
		this.correct = correct;
		this.displayOrder = displayOrder;
	}

	public Long getId() {
		return id;
	}

	public Question getQuestion() {
		return question;
	}

	void setQuestion(Question question) {
		this.question = question;
	}

	public String getText() {
		return text;
	}

	public boolean isCorrect() {
		return correct;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

}
