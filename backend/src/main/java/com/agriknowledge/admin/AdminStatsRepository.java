package com.agriknowledge.admin;

import com.agriknowledge.material.Material;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Counts for the admin dashboard.
 *
 * <p>Grouped queries rather than one count per bucket: three statuses and three
 * types would otherwise be six round trips for two numbers.
 */
public interface AdminStatsRepository extends JpaRepository<Material, Long> {

	@Query("select m.status, count(m) from Material m group by m.status")
	List<Object[]> countByStatus();

	@Query("select m.typeColumn, count(m) from Material m where m.status = 'PUBLISHED' group by m.typeColumn")
	List<Object[]> countPublishedByType();

	@Query("select m from Material m where m.status = 'PUBLISHED' order by m.viewCount desc")
	List<Material> findMostViewed(Pageable pageable);

}
