package com.agriknowledge.quiz;

import com.agriknowledge.user.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	/**
	 * Both the score and the total are stored rather than recomputed on read. A
	 * later edit to the quiz must not silently rewrite someone's past result.
	 */
	@Column(precision = 7, scale = 2)
	private BigDecimal score;

	@Column(name = "total_marks", precision = 7, scale = 2)
	private BigDecimal totalMarks;

	@OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true,
			fetch = FetchType.LAZY)
	private List<QuizAnswer> answers = new ArrayList<>();

	protected QuizAttempt() {
		// for JPA
	}

	public QuizAttempt(User user, Quiz quiz) {
		this.user = user;
		this.quiz = quiz;
	}

	@PrePersist
	void onCreate() {
		this.startedAt = Instant.now();
	}

	public boolean isSubmitted() {
		return submittedAt != null;
	}

	public void submit(BigDecimal score, BigDecimal totalMarks) {
		this.score = score;
		this.totalMarks = totalMarks;
		this.submittedAt = Instant.now();
	}

	public void addAnswer(QuizAnswer answer) {
		answers.add(answer);
		answer.setAttempt(this);
	}

	/** Percentage of the marks available, floored at zero so a negative total reads as 0%. */
	public BigDecimal percentage() {
		if (score == null || totalMarks == null || totalMarks.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return score.max(BigDecimal.ZERO)
				.multiply(BigDecimal.valueOf(100))
				.divide(totalMarks, 1, RoundingMode.HALF_UP);
	}

	public boolean isPassed() {
		return percentage().compareTo(BigDecimal.valueOf(quiz.getPassPercentage())) >= 0;
	}

	/**
	 * Whether the submission landed inside the time limit.
	 *
	 * <p>This is reported, not enforced. The quiz is practice: a learner who runs
	 * over only shortchanges their own exam simulation, and rejecting the
	 * submission would throw away work over a network hiccup.
	 */
	public boolean isWithinTimeLimit() {
		Integer limit = quiz.getTimeLimitSeconds();
		if (limit == null || submittedAt == null) {
			return true;
		}
		return Duration.between(startedAt, submittedAt).getSeconds() <= limit;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Quiz getQuiz() {
		return quiz;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public BigDecimal getScore() {
		return score;
	}

	public BigDecimal getTotalMarks() {
		return totalMarks;
	}

	public List<QuizAnswer> getAnswers() {
		return answers;
	}

}
