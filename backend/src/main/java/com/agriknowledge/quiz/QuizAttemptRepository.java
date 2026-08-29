package com.agriknowledge.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

	/** An attempt already in progress, so a reload resumes rather than starting over. */
	@Query("""
			select a from QuizAttempt a
			where a.user.id = :userId and a.quiz.id = :quizId and a.submittedAt is null
			order by a.startedAt desc
			limit 1
			""")
	Optional<QuizAttempt> findOpenAttempt(@Param("userId") Long userId, @Param("quizId") Long quizId);

	@Query("""
			select a from QuizAttempt a
			join fetch a.quiz
			where a.user.id = :userId and a.submittedAt is not null
			""")
	Page<QuizAttempt> findHistory(@Param("userId") Long userId, Pageable pageable);

	@Query("""
			select distinct a from QuizAttempt a
			left join fetch a.answers answer
			left join fetch answer.question
			where a.id = :id
			""")
	Optional<QuizAttempt> findByIdWithAnswers(@Param("id") Long id);

	long countByUserIdAndQuizId(Long userId, Long quizId);

}
