package com.agriknowledge.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

	Optional<Exam> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Exam> findAllByOrderByDisplayOrderAscNameAsc();

	/** Fetches the join collection up front, so rendering one exam is a single query. */
	@Query("select e from Exam e left join fetch e.topics where e.slug = :slug")
	Optional<Exam> findBySlugWithTopics(String slug);

	/** Topic counts for the whole list without firing a query per exam. */
	@Query("select e.id, count(t.id) from Exam e left join e.topics t group by e.id")
	List<Object[]> countTopicsPerExam();

}
