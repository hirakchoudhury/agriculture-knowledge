package com.agriknowledge.path;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

	List<LearningPath> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

	/** Items and their materials in one query, so rendering a path is not an N+1. */
	@Query("""
			select distinct p from LearningPath p
			left join fetch p.items item
			left join fetch item.material
			where p.id = :id
			""")
	Optional<LearningPath> findByIdWithItems(@Param("id") Long id);

	@Query("select count(i) from LearningPathItem i where i.path.owner.id = :ownerId")
	long countItemsForOwner(@Param("ownerId") Long ownerId);

}
