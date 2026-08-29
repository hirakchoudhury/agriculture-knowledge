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

/** One learner's choice for one question, and whether it was right. */
@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id", nullable = false)
	private QuizAttempt attempt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	/** Null means the question was left unanswered, which scores zero. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "selected_option_id")
	private QuestionOption selectedOption;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	protected QuizAnswer() {
		// for JPA
	}

	public QuizAnswer(Question question, QuestionOption selectedOption, boolean correct) {
		this.question = question;
		this.selectedOption = selectedOption;
		this.correct = correct;
	}

	public Long getId() {
		return id;
	}

	public QuizAttempt getAttempt() {
		return attempt;
	}

	void setAttempt(QuizAttempt attempt) {
		this.attempt = attempt;
	}

	public Question getQuestion() {
		return question;
	}

	public QuestionOption getSelectedOption() {
		return selectedOption;
	}

	public Long getSelectedOptionId() {
		return selectedOption == null ? null : selectedOption.getId();
	}

	public boolean isCorrect() {
		return correct;
	}

}
