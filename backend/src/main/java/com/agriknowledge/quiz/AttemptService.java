package com.agriknowledge.quiz;

import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.common.ConflictException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.material.MaterialStatus;
import com.agriknowledge.quiz.dto.AttemptOption;
import com.agriknowledge.quiz.dto.AttemptQuestion;
import com.agriknowledge.quiz.dto.AttemptResult;
import com.agriknowledge.quiz.dto.AttemptSummary;
import com.agriknowledge.quiz.dto.AttemptView;
import com.agriknowledge.quiz.dto.QuizSummary;
import com.agriknowledge.quiz.dto.ReviewOption;
import com.agriknowledge.quiz.dto.ReviewQuestion;
import com.agriknowledge.quiz.dto.SubmitAttemptRequest;
import com.agriknowledge.quiz.dto.SubmittedAnswer;
import com.agriknowledge.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional(readOnly = true)
public class AttemptService {

	private final QuizRepository quizzes;
	private final QuizAttemptRepository attempts;
	private final UserRepository users;

	public AttemptService(QuizRepository quizzes, QuizAttemptRepository attempts, UserRepository users) {
		this.quizzes = quizzes;
		this.attempts = attempts;
		this.users = users;
	}

	public QuizSummary summary(String slug, Long viewerId) {
		Quiz quiz = publishedQuiz(slug);
		return new QuizSummary(
				quiz.getId(),
				quiz.getSlug(),
				quiz.getTitle(),
				quiz.getSummary(),
				quiz.getQuestions().size(),
				QuizAdminService.totalMarks(quiz),
				quiz.getTimeLimitSeconds(),
				quiz.getPassPercentage(),
				viewerId == null ? 0 : attempts.countByUserIdAndQuizId(viewerId, quiz.getId()));
	}

	/**
	 * Starts an attempt, or resumes the one already open.
	 *
	 * <p>Resuming matters because a reload would otherwise abandon the attempt and
	 * start another, filling the history with half-finished rows.
	 */
	@Transactional
	public AttemptView start(String slug, Long userId) {
		Quiz quiz = publishedQuiz(slug);

		if (quiz.getQuestions().isEmpty()) {
			throw new BadRequestException("This quiz has no questions yet");
		}

		QuizAttempt attempt = attempts.findOpenAttempt(userId, quiz.getId())
				.orElseGet(() -> attempts.save(new QuizAttempt(users.getReferenceById(userId), quiz)));

		return toView(attempt, quiz);
	}

	@Transactional
	public AttemptResult submit(Long attemptId, SubmitAttemptRequest request, Long userId) {
		QuizAttempt attempt = attempts.findById(attemptId)
				.orElseThrow(() -> NotFoundException.of("Attempt", attemptId));

		if (!attempt.getUser().getId().equals(userId)) {
			throw new AccessDeniedException("That attempt belongs to someone else");
		}
		if (attempt.isSubmitted()) {
			throw new ConflictException("That attempt has already been submitted");
		}

		Quiz quiz = quizzes.findByIdWithQuestions(attempt.getQuiz().getId())
				.orElseThrow(() -> NotFoundException.of("Quiz", attempt.getQuiz().getId()));

		Map<Long, Long> chosen = new HashMap<>();
		for (SubmittedAnswer answer : request.answers()) {
			chosen.put(answer.questionId(), answer.selectedOptionId());
		}

		// Scoring walks the quiz, not the submission. A question the client omitted
		// is unanswered rather than absent, and an id the client invented for a
		// question that is not on this quiz simply never gets looked at.
		BigDecimal score = BigDecimal.ZERO;
		BigDecimal totalMarks = BigDecimal.ZERO;

		for (Question question : quiz.getQuestions()) {
			totalMarks = totalMarks.add(question.getMarks());

			Long selectedId = chosen.get(question.getId());
			QuestionOption selected = null;

			if (selectedId != null) {
				selected = question.getOptions().stream()
						.filter(option -> option.getId().equals(selectedId))
						.findFirst()
						// Rejecting rather than ignoring: an option id from a different
						// question means the client is confused, and silently scoring it
						// as unanswered would hide that.
						.orElseThrow(() -> new BadRequestException(
								"Option %d does not belong to question %d"
										.formatted(selectedId, question.getId())));
			}

			boolean correct = selected != null && selected.isCorrect();
			score = score.add(awardFor(question, selected, correct));

			attempt.addAnswer(new QuizAnswer(question, selected, correct));
		}

		attempt.submit(score, totalMarks);
		return toResult(attempt, quiz);
	}

	public AttemptResult review(Long attemptId, Long userId) {
		QuizAttempt attempt = attempts.findByIdWithAnswers(attemptId)
				.orElseThrow(() -> NotFoundException.of("Attempt", attemptId));

		if (!attempt.getUser().getId().equals(userId)) {
			throw new AccessDeniedException("That attempt belongs to someone else");
		}
		if (!attempt.isSubmitted()) {
			throw new BadRequestException("That attempt has not been submitted yet");
		}

		Quiz quiz = quizzes.findByIdWithQuestions(attempt.getQuiz().getId())
				.orElseThrow(() -> NotFoundException.of("Quiz", attempt.getQuiz().getId()));

		return toResult(attempt, quiz);
	}

	public PageResponse<AttemptSummary> history(Long userId, int page, int size) {
		return PageResponse.of(
				attempts.findHistory(userId, PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 50),
						Sort.by(Sort.Direction.DESC, "submittedAt"))),
				attempt -> new AttemptSummary(
						attempt.getId(),
						attempt.getQuiz().getId(),
						attempt.getQuiz().getSlug(),
						attempt.getQuiz().getTitle(),
						attempt.getScore(),
						attempt.getTotalMarks(),
						attempt.percentage(),
						attempt.isPassed(),
						attempt.getSubmittedAt()));
	}

	/** Right earns the marks, wrong loses the negative marks, blank scores nothing. */
	private BigDecimal awardFor(Question question, QuestionOption selected, boolean correct) {
		if (selected == null) {
			return BigDecimal.ZERO;
		}
		return correct ? question.getMarks() : question.getNegativeMarks().negate();
	}

	private Quiz publishedQuiz(String slug) {
		Quiz quiz = quizzes.findBySlugWithQuestions(slug)
				.orElseThrow(() -> new NotFoundException("No quiz with slug '%s'".formatted(slug)));

		// Same rule as any other material: an unpublished slug must not be
		// distinguishable from one that does not exist.
		if (quiz.getStatus() != MaterialStatus.PUBLISHED) {
			throw new NotFoundException("No quiz with slug '%s'".formatted(slug));
		}

		return quiz;
	}

	private AttemptView toView(QuizAttempt attempt, Quiz quiz) {
		List<Question> ordered = new ArrayList<>(quiz.getQuestions());

		if (quiz.isShuffleQuestions()) {
			// Seeded by the attempt id so a reload gives the same order. An
			// unseeded shuffle would reshuffle mid-attempt and lose the learner
			// their place.
			Collections.shuffle(ordered, new Random(attempt.getId()));
		}

		List<AttemptQuestion> questions = ordered.stream()
				.map(question -> new AttemptQuestion(
						question.getId(),
						question.getText(),
						question.getImageUrl(),
						question.getMarks(),
						question.getNegativeMarks(),
						question.getOptions().stream()
								// AttemptOption has no correct field, so the answer key
								// cannot travel through this path at all.
								.map(option -> new AttemptOption(option.getId(), option.getText()))
								.toList()))
				.toList();

		Instant expiresAt = quiz.getTimeLimitSeconds() == null
				? null
				: attempt.getStartedAt().plusSeconds(quiz.getTimeLimitSeconds());

		return new AttemptView(
				attempt.getId(),
				quiz.getId(),
				quiz.getSlug(),
				quiz.getTitle(),
				quiz.getTimeLimitSeconds(),
				attempt.getStartedAt(),
				expiresAt,
				questions);
	}

	private AttemptResult toResult(QuizAttempt attempt, Quiz quiz) {
		Map<Long, QuizAnswer> answersByQuestion = new LinkedHashMap<>();
		for (QuizAnswer answer : attempt.getAnswers()) {
			answersByQuestion.put(answer.getQuestion().getId(), answer);
		}

		List<ReviewQuestion> questions = quiz.getQuestions().stream()
				.map(question -> {
					QuizAnswer answer = answersByQuestion.get(question.getId());
					QuestionOption selected = answer == null ? null : answer.getSelectedOption();
					boolean correct = answer != null && answer.isCorrect();

					return new ReviewQuestion(
							question.getId(),
							question.getText(),
							question.getExplanation(),
							question.getImageUrl(),
							question.getMarks(),
							question.getNegativeMarks(),
							awardFor(question, selected, correct),
							selected == null ? null : selected.getId(),
							question.correctOption().map(QuestionOption::getId).orElse(null),
							correct,
							question.getOptions().stream()
									.map(option -> new ReviewOption(option.getId(), option.getText(),
											option.isCorrect()))
									.toList());
				})
				.toList();

		return new AttemptResult(
				attempt.getId(),
				quiz.getId(),
				quiz.getSlug(),
				quiz.getTitle(),
				attempt.getScore(),
				attempt.getTotalMarks(),
				attempt.percentage(),
				quiz.getPassPercentage(),
				attempt.isPassed(),
				attempt.isWithinTimeLimit(),
				attempt.getStartedAt(),
				attempt.getSubmittedAt(),
				questions);
	}

}
