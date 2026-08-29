package com.agriknowledge.path;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialProgressRepository extends JpaRepository<MaterialProgress, Long> {

	Optional<MaterialProgress> findByUserIdAndMaterialId(Long userId, Long materialId);

	/**
	 * Progress for a whole path in one query. Asking per item would be an N+1 on
	 * the page where it matters most.
	 */
	@Query("""
			select p from MaterialProgress p
			where p.user.id = :userId and p.material.id in :materialIds
			""")
	List<MaterialProgress> findForMaterials(@Param("userId") Long userId,
			@Param("materialIds") Collection<Long> materialIds);

	long countByUserIdAndStatus(Long userId, ProgressStatus status);

}
