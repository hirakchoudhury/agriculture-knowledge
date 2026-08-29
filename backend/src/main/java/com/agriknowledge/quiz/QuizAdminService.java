package com.agriknowledge.quiz;

import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.material.MaterialSupport;
import com.agriknowledge.quiz.dto.AdminOption;
import com.agriknowledge.quiz.dto.AdminQuestion;
import com.agriknowledge.quiz.dto.AdminQuizDetail;
import com.agriknowledge.quiz.dto.OptionRequest;
import com.agriknowledge.quiz.dto.QuestionRequest;
import com.agriknowledge.quiz.dto.QuestionsRequest;
import com.agriknowledge.quiz.dto.QuizRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class QuizAdminService {

	private final QuizRepository quizzes;
	private final MaterialSupport materials;

	public QuizAdminService(QuizRepository quizzes, MaterialSupport materials) {
		this.quizzes = quizzes;
		this.materials = materials;
	}

	@Transactional
	public AdminQuizDetail create(QuizRequest request, Long authorId) {
		Quiz quiz = new Quiz(
				request.title().trim(),
				materials.uniqueSlug(request.title()),
				request.summary(),
				request.thumbnailUrl(),
				request.difficulty(),
				materials.author(authorId),
				request.timeLimitSeconds(),
				request.passPercentage(),
				request.shuffleQuestions());

		materials.applyTags(quiz, request.topicIds(), request.examIds());
		return toDetail(quizzes.save(quiz));
	}

	@Transactional
	public AdminQuizDetail update(Long id, QuizRequest request) {
		Quiz quiz = load(id);

		// The slug is left alone on rename: it is already in published URLs.
		quiz.setTitle(request.title().trim());
		quiz.setSummary(request.summary());
		if (request.thumbnailUrl() != null && !request.thumbnailUrl().isBlank()) {
			quiz.setThumbnailUrl(request.thumbnailUrl());
		}
		quiz.setDifficulty(request.difficulty());
		quiz.setTimeLimitSeconds(request.timeLimitSeconds());
		quiz.setPassPercentage(request.passPercentage());
		quiz.setShuffleQuestions(request.shuffleQuestions());
		materials.applyTags(quiz, request.topicIds(), request.examIds());

		return toDetail(quiz);
	}

	/**
	 * Replaces the entire question set.
	 *
	 * <p>Rewriting rather than patching is what makes a paste-in bulk import work,
	 * and it keeps the call idempotent. The cost is that editing one question
	 * resends them all, which is fine for the sizes a single admin authors.
	 *
	 * <p>Existing attempts are unaffected: their answers reference the old question
	 * rows, and a submitted attempt stores its own score and total.
	 */
	@Transactional
	public AdminQuizDetail replaceQuestions(Long quizId, QuestionsRequest request) {
		Quiz quiz = loadWithQuestions(quizId);

		for (int index = 0; index < request.questions().size(); index++) {
			validate(request.questions().get(index), index);
		}

		quiz.clearQuestions();

		int questionOrder = 0;
		for (QuestionRequest incoming : request.questions()) {
			Question question = new Question(
					incoming.text().trim(),
					blankToNull(incoming.explanation()),
					blankToNull(incoming.imageUrl()),
					incoming.marks(),
					incoming.negativeMarks(),
					questionOrder++);

			int optionOrder = 0;
			for (OptionRequest option : incoming.options()) {
				question.addOption(new QuestionOption(option.text().trim(), option.correct(), optionOrder++));
			}

			quiz.addQuestion(question);
		}

		return toDetail(quiz);
	}

	public AdminQuizDetail get(Long id) {
		return toDetail(loadWithQuestions(id));
	}

	/**
	 * Checked here rather than with an annotation, because the rule spans the whole
	 * option list. A question with no correct answer is unscoreable, and one with
	 * several is ambiguous — both are far cheaper to catch now than after a hundred
	 * learners have attempted it.
	 */
	private void validate(QuestionRequest question, int index) {
		long correct = question.options().stream().filter(OptionRequest::correct).count();
		if (correct == 0) {
			throw new BadRequestException(
					"Question %d has no correct option marked".formatted(index + 1));
		}
		if (correct > 1) {
			throw new BadRequestException(
					"Question %d has %d correct options marked; exactly one is allowed"
							.formatted(index + 1, correct));
		}
	}

	private Quiz load(Long id) {
		return quizzes.findById(id).orElseThrow(() -> NotFoundException.of("Quiz", id));
	}

	private Quiz loadWithQuestions(Long id) {
		return quizzes.findByIdWithQuestions(id)
				.orElseThrow(() -> NotFoundException.of("Quiz", id));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	AdminQuizDetail toDetail(Quiz quiz) {
		List<AdminQuestion> questions = quiz.getQuestions().stream()
				.map(question -> new AdminQuestion(
						question.getId(),
						question.getText(),
						question.getExplanation(),
						question.getImageUrl(),
						question.getMarks(),
						question.getNegativeMarks(),
						question.getDisplayOrder(),
						question.getOptions().stream()
								.map(option -> new AdminOption(option.getId(), option.getText(),
										option.isCorrect(), option.getDisplayOrder()))
								.toList()))
				.toList();

		return new AdminQuizDetail(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getSlug(),
				quiz.getSummary(),
				quiz.getStatus(),
				quiz.getTimeLimitSeconds(),
				quiz.getPassPercentage(),
				quiz.isShuffleQuestions(),
				totalMarks(quiz),
				questions);
	}

	static BigDecimal totalMarks(Quiz quiz) {
		return quiz.getQuestions().stream()
				.map(Question::getMarks)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
