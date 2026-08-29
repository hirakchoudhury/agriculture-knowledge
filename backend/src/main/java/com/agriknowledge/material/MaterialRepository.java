package com.agriknowledge.material;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

	Optional<Material> findBySlug(String slug);

	boolean existsBySlug(String slug);

	/**
	 * Every filter is optional, so one query serves the public feed, a topic page,
	 * an exam page and the admin list.
	 *
	 * <p>Topic and exam are matched with EXISTS rather than a join, so a material
	 * tagged with three topics still appears once and the result stays pageable
	 * without DISTINCT.
	 */
	@Query("""
			select m from Material m
			where (:status is null or m.status = :status)
			  and (:type is null or m.typeColumn = :type)
			  and (:difficulty is null or m.difficulty = :difficulty)
			  and (:topicId is null or exists (select 1 from m.topics t where t.id = :topicId))
			  and (:examId is null or exists (select 1 from m.exams e where e.id = :examId))
			  and (:pattern is null
				   or lower(m.title) like :pattern
				   or lower(coalesce(m.summary, '')) like :pattern)
			""")
	Page<Material> search(
			@Param("status") MaterialStatus status,
			@Param("type") MaterialType type,
			@Param("difficulty") Difficulty difficulty,
			@Param("topicId") Long topicId,
			@Param("examId") Long examId,
			@Param("pattern") String pattern,
			Pageable pageable);

	/**
	 * Topic names for a whole page of results in one query. Reading them off each
	 * entity instead would fire one query per card.
	 */
	@Query("select m.id, t.name from Material m join m.topics t where m.id in :ids order by t.displayOrder, t.name")
	List<Object[]> findTopicNamesFor(@Param("ids") Collection<Long> ids);

	/**
	 * A direct increment rather than read-modify-write: two readers arriving at once
	 * would otherwise both write the same number and lose a view.
	 */
	@Modifying
	@Query("update Material m set m.viewCount = m.viewCount + 1 where m.id = :id")
	void incrementViewCount(@Param("id") Long id);

	/**
	 * Counters move by a delta in the same transaction as the like or comment that
	 * caused them, rather than being recomputed with COUNT(*) on every listing.
	 */
	@Modifying
	@Query("update Material m set m.likeCount = m.likeCount + :delta where m.id = :id")
	void adjustLikeCount(@Param("id") Long id, @Param("delta") int delta);

	@Modifying
	@Query("update Material m set m.commentCount = m.commentCount + :delta where m.id = :id")
	void adjustCommentCount(@Param("id") Long id, @Param("delta") int delta);

}
