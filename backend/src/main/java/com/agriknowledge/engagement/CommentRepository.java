package com.agriknowledge.engagement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	/**
	 * Top-level comments only. Replies are fetched separately for the whole page,
	 * so a thread costs two queries rather than one per comment.
	 *
	 * <p>A deleted comment with no replies is excluded here rather than filtered
	 * afterwards, so the page's total agrees with what it actually contains. Doing
	 * it in memory made the UI say "Discussion (1)" above an empty thread.
	 */
	@Query("""
			select c from Comment c
			join fetch c.author
			where c.material.id = :materialId
			  and c.parent is null
			  and (c.deleted = false
				   or exists (select 1 from Comment r where r.parent = c and r.deleted = false))
			""")
	Page<Comment> findRoots(@Param("materialId") Long materialId, Pageable pageable);

	/**
	 * Deleted replies are dropped outright. Nothing hangs beneath a reply, so a
	 * placeholder there would preserve no structure and only add noise.
	 */
	@Query("""
			select c from Comment c
			join fetch c.author
			where c.parent.id in :parentIds and c.deleted = false
			order by c.createdAt asc
			""")
	List<Comment> findRepliesTo(@Param("parentIds") Collection<Long> parentIds);

	long countByMaterialIdAndDeletedFalse(Long materialId);

}
