package com.agriknowledge.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	Optional<Topic> findBySlug(String slug);

	boolean existsBySlug(String slug);

	/** The whole tree in one query, already ordered so assembly preserves it. */
	@Query("select t from Topic t left join fetch t.parent order by t.displayOrder, t.name")
	List<Topic> findAllForTree();

	@Query("select t from Topic t join t.parent p where p.id = :parentId")
	List<Topic> findChildren(@Param("parentId") Long parentId);

	@Query("""
			select t from Topic t
			left join fetch t.parent
			join Exam e on t member of e.topics
			where e.slug = :examSlug
			order by t.displayOrder, t.name
			""")
	List<Topic> findAllForExam(@Param("examSlug") String examSlug);

}
