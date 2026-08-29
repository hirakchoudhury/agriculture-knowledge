package com.agriknowledge.material;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-text search over materials.
 *
 * <p>This returns ids rather than entities on purpose. The {@code @@} operator and
 * {@code ts_rank} have no JPQL equivalent, so the query has to be native — but
 * mapping a native result back onto a JOINED inheritance hierarchy is fragile.
 * Fetching ids here and loading the entities through JPA afterwards keeps the
 * exotic SQL in one place and the entity loading on the well-trodden path, at the
 * cost of one extra query.
 *
 * <p>Ordering is by rank, so the best match leads rather than the newest.
 */
@Repository
public class MaterialSearch {

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * @param query free text, already known to be non-blank
	 * @return matching ids for the requested page, best match first
	 */
	public Page<Long> findMatchingIds(MaterialStatus status, MaterialType type, Difficulty difficulty,
			Long topicId, Long examId, String query, Pageable pageable) {

		Map<String, Object> parameters = new LinkedHashMap<>();
		StringBuilder where = new StringBuilder(" where m.search_vector @@ plainto_tsquery('english', :query)");
		parameters.put("query", query);

		if (status != null) {
			where.append(" and m.status = :status");
			parameters.put("status", status.name());
		}
		if (type != null) {
			where.append(" and m.type = :type");
			parameters.put("type", type.name());
		}
		if (difficulty != null) {
			where.append(" and m.difficulty = :difficulty");
			parameters.put("difficulty", difficulty.name());
		}
		if (topicId != null) {
			where.append(" and exists (select 1 from material_topics mt"
					+ " where mt.material_id = m.id and mt.topic_id = :topicId)");
			parameters.put("topicId", topicId);
		}
		if (examId != null) {
			where.append(" and exists (select 1 from material_exams me"
					+ " where me.material_id = m.id and me.exam_id = :examId)");
			parameters.put("examId", examId);
		}

		// Every fragment above is a fixed string; only values are bound. Nothing
		// from the caller is ever concatenated into the SQL.
		Query countQuery = entityManager.createNativeQuery(
				"select count(*) from materials m" + where);
		bind(countQuery, parameters);
		long total = ((Number) countQuery.getSingleResult()).longValue();

		if (total == 0) {
			return new PageImpl<>(List.of(), pageable, 0);
		}

		Query idQuery = entityManager.createNativeQuery(
				"select m.id from materials m" + where
						+ " order by ts_rank(m.search_vector, plainto_tsquery('english', :query)) desc,"
						+ " m.published_at desc nulls last, m.id desc"
						+ " limit :limit offset :offset");
		bind(idQuery, parameters);
		idQuery.setParameter("limit", pageable.getPageSize());
		idQuery.setParameter("offset", pageable.getOffset());

		List<Long> ids = new ArrayList<>();
		for (Object row : idQuery.getResultList()) {
			ids.add(((Number) row).longValue());
		}

		return new PageImpl<>(ids, pageable, total);
	}

	private void bind(Query query, Map<String, Object> parameters) {
		parameters.forEach(query::setParameter);
	}

}
