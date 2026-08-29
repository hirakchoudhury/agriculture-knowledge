package com.agriknowledge.engagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MaterialLikeRepository extends JpaRepository<MaterialLike, Long> {

	boolean existsByUserIdAndMaterialId(Long userId, Long materialId);

	@Modifying
	@Query("delete from MaterialLike l where l.user.id = :userId and l.material.id = :materialId")
	int deleteByUserAndMaterial(@Param("userId") Long userId, @Param("materialId") Long materialId);

	/**
	 * Which of these materials the viewer has already liked, in one query. Asking
	 * per card would be the same N+1 problem as topic names.
	 */
	@Query("select l.material.id from MaterialLike l where l.user.id = :userId and l.material.id in :materialIds")
	List<Long> findLikedMaterialIds(@Param("userId") Long userId,
			@Param("materialIds") Collection<Long> materialIds);

	long countByMaterialId(Long materialId);

}
