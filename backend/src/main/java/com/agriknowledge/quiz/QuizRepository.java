package com.agriknowledge.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

	Optional<Quiz> findBySlug(String slug);

	/**
	 * Questions are join-fetched; their options come from the batched collection on
	 * Question. Join-fetching both is what Hibernate rejects as a multiple-bag
	 * fetch, and lazy-loading both would be an N+1.
	 */
	@Query("""
			select distinct q from Quiz q
			left join fetch q.questions question
			where q.id = :id
			""")
	Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);

	@Query("""
			select distinct q from Quiz q
			left join fetch q.questions question
			where q.slug = :slug
			""")
	Optional<Quiz> findBySlugWithQuestions(@Param("slug") String slug);

}
