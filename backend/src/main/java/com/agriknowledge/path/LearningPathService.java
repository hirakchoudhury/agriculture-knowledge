package com.agriknowledge.path;

import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.common.ConflictException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.material.Material;
import com.agriknowledge.material.MaterialRepository;
import com.agriknowledge.path.dto.AddItemRequest;
import com.agriknowledge.path.dto.PathDetail;
import com.agriknowledge.path.dto.PathItemResponse;
import com.agriknowledge.path.dto.PathRequest;
import com.agriknowledge.path.dto.PathSummary;
import com.agriknowledge.path.dto.ProgressRequest;
import com.agriknowledge.path.dto.ProgressResponse;
import com.agriknowledge.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LearningPathService {

	private static final int MAX_PATHS_PER_USER = 50;
	private static final int MAX_ITEMS_PER_PATH = 200;

	private final LearningPathRepository paths;
	private final MaterialProgressRepository progress;
	private final MaterialRepository materials;
	private final UserRepository users;

	public LearningPathService(LearningPathRepository paths, MaterialProgressRepository progress,
			MaterialRepository materials, UserRepository users) {
		this.paths = paths;
		this.progress = progress;
		this.materials = materials;
		this.users = users;
	}

	// ----- paths -------------------------------------------------------------

	public List<PathSummary> listMine(Long ownerId) {
		List<LearningPath> owned = paths.findByOwnerIdOrderByCreatedAtDesc(ownerId);
		if (owned.isEmpty()) {
			return List.of();
		}

		// Every material across every path, so the completed counts cost one query
		// rather than one per path.
		Set<Long> materialIds = new HashSet<>();
		for (LearningPath path : owned) {
			for (LearningPathItem item : path.getItems()) {
				materialIds.add(item.getMaterial().getId());
			}
		}

		Set<Long> completed = completedMaterialIds(ownerId, materialIds);

		return owned.stream()
				.map(path -> new PathSummary(
						path.getId(),
						path.getTitle(),
						path.getDescription(),
						path.getItems().size(),
						(int) path.getItems().stream()
								.filter(item -> completed.contains(item.getMaterial().getId()))
								.count(),
						path.getCreatedAt()))
				.toList();
	}

	public PathDetail get(Long pathId, Long viewerId) {
		return toDetail(loadOwned(pathId, viewerId), viewerId);
	}

	@Transactional
	public PathDetail create(PathRequest request, Long ownerId) {
		if (paths.findByOwnerIdOrderByCreatedAtDesc(ownerId).size() >= MAX_PATHS_PER_USER) {
			throw new ConflictException(
					"You already have %d paths. Delete one before creating another."
							.formatted(MAX_PATHS_PER_USER));
		}

		LearningPath path = new LearningPath(
				users.getReferenceById(ownerId),
				request.title().trim(),
				blankToNull(request.description()));

		return toDetail(paths.save(path), ownerId);
	}

	@Transactional
	public PathDetail update(Long pathId, PathRequest request, Long ownerId) {
		LearningPath path = loadOwned(pathId, ownerId);
		path.setTitle(request.title().trim());
		path.setDescription(blankToNull(request.description()));
		return toDetail(path, ownerId);
	}

	@Transactional
	public void delete(Long pathId, Long ownerId) {
		// Deleting a path removes its items but never the material, and never the
		// learner's progress: progress belongs to the person, not the path.
		paths.delete(loadOwned(pathId, ownerId));
	}

	// ----- items -------------------------------------------------------------

	@Transactional
	public PathDetail addItem(Long pathId, AddItemRequest request, Long ownerId) {
		LearningPath path = loadOwned(pathId, ownerId);

		if (path.getItems().size() >= MAX_ITEMS_PER_PATH) {
			throw new ConflictException("A path can hold at most %d items".formatted(MAX_ITEMS_PER_PATH));
		}

		Material material = materials.findById(request.materialId())
				.orElseThrow(() -> NotFoundException.of("Material", request.materialId()));

		// Adding a draft would put something in the path that cannot be opened.
		if (!material.isPublished()) {
			throw new BadRequestException("That material is not published");
		}

		boolean alreadyThere = path.getItems().stream()
				.anyMatch(item -> item.getMaterial().getId().equals(material.getId()));
		if (alreadyThere) {
			throw new ConflictException("That material is already in this path");
		}

		path.addItem(new LearningPathItem(material, path.nextDisplayOrder(), blankToNull(request.note())));

		try {
			paths.flush();
		}
		catch (DataIntegrityViolationException ex) {
			// Two rapid clicks; the unique constraint settled it.
			throw new ConflictException("That material is already in this path");
		}

		return toDetail(path, ownerId);
	}

	@Transactional
	public PathDetail removeItem(Long pathId, Long itemId, Long ownerId) {
		LearningPath path = loadOwned(pathId, ownerId);
		if (!path.removeItem(itemId)) {
			throw NotFoundException.of("Path item", itemId);
		}
		return toDetail(path, ownerId);
	}

	/**
	 * Rewrites the order of the whole path in one transaction.
	 *
	 * <p>Taking the complete list rather than a move instruction means a dropped
	 * request cannot leave the order half-applied, and repeating the call is a no-op.
	 */
	@Transactional
	public PathDetail reorder(Long pathId, List<Long> itemIds, Long ownerId) {
		LearningPath path = loadOwned(pathId, ownerId);

		Map<Long, LearningPathItem> byId = new HashMap<>();
		for (LearningPathItem item : path.getItems()) {
			byId.put(item.getId(), item);
		}

		// Insisting on exactly the current set catches a stale client sending an
		// order built before someone added or removed a step.
		if (itemIds.size() != byId.size() || !byId.keySet().containsAll(itemIds)) {
			throw new BadRequestException(
					"The order must list every item in this path exactly once");
		}
		if (new HashSet<>(itemIds).size() != itemIds.size()) {
			throw new BadRequestException("The order contains a duplicate item");
		}

		for (int index = 0; index < itemIds.size(); index++) {
			byId.get(itemIds.get(index)).setDisplayOrder(index);
		}

		return toDetail(path, ownerId);
	}

	// ----- progress ----------------------------------------------------------

	@Transactional
	public ProgressResponse setProgress(Long materialId, ProgressRequest request, Long userId) {
		Material material = materials.findById(materialId)
				.orElseThrow(() -> NotFoundException.of("Material", materialId));

		if (!material.isPublished()) {
			throw new BadRequestException("That material is not published");
		}

		MaterialProgress record = progress.findByUserIdAndMaterialId(userId, materialId)
				.orElseGet(() -> progress.save(
						new MaterialProgress(users.getReferenceById(userId), material)));

		if (request.status() == ProgressStatus.COMPLETED) {
			record.markCompleted();
		}
		else {
			record.markInProgress();
		}
		record.recordPosition(request.lastPositionSeconds());

		return toProgressResponse(record);
	}

	public ProgressResponse getProgress(Long materialId, Long userId) {
		return progress.findByUserIdAndMaterialId(userId, materialId)
				.map(this::toProgressResponse)
				.orElseGet(() -> new ProgressResponse(materialId, ProgressStatus.IN_PROGRESS,
						false, null, null));
	}

	// ----- helpers -----------------------------------------------------------

	private LearningPath loadOwned(Long pathId, Long viewerId) {
		LearningPath path = paths.findByIdWithItems(pathId)
				.orElseThrow(() -> NotFoundException.of("Learning path", pathId));

		// Paths are private. 403 rather than 404 here is a deliberate difference
		// from drafts: the learner knows their own paths exist, so hiding the
		// distinction buys nothing and a clear message is more useful.
		if (!path.getOwner().getId().equals(viewerId)) {
			throw new AccessDeniedException("That learning path belongs to someone else");
		}

		return path;
	}

	private Set<Long> completedMaterialIds(Long userId, Set<Long> materialIds) {
		if (materialIds.isEmpty()) {
			return Set.of();
		}
		Set<Long> completed = new HashSet<>();
		for (MaterialProgress record : progress.findForMaterials(userId, materialIds)) {
			if (record.isCompleted()) {
				completed.add(record.getMaterial().getId());
			}
		}
		return completed;
	}

	private PathDetail toDetail(LearningPath path, Long viewerId) {
		Set<Long> materialIds = new HashSet<>();
		for (LearningPathItem item : path.getItems()) {
			materialIds.add(item.getMaterial().getId());
		}

		Map<Long, MaterialProgress> byMaterial = new HashMap<>();
		if (!materialIds.isEmpty()) {
			for (MaterialProgress record : progress.findForMaterials(viewerId, materialIds)) {
				byMaterial.put(record.getMaterial().getId(), record);
			}
		}

		List<PathItemResponse> items = new ArrayList<>();
		int completedCount = 0;

		for (LearningPathItem item : path.getItems()) {
			Material material = item.getMaterial();
			MaterialProgress record = byMaterial.get(material.getId());
			boolean completed = record != null && record.isCompleted();
			if (completed) {
				completedCount++;
			}

			items.add(new PathItemResponse(
					item.getId(),
					material.getId(),
					material.getTitle(),
					material.getSlug(),
					material.getType(),
					material.getDifficulty(),
					item.getDisplayOrder(),
					item.getNote(),
					record == null ? ProgressStatus.IN_PROGRESS : record.getStatus(),
					completed));
		}

		items.sort((left, right) -> Integer.compare(left.displayOrder(), right.displayOrder()));

		return new PathDetail(
				path.getId(),
				path.getTitle(),
				path.getDescription(),
				items.size(),
				completedCount,
				path.getCreatedAt(),
				items);
	}

	private ProgressResponse toProgressResponse(MaterialProgress record) {
		return new ProgressResponse(
				record.getMaterial().getId(),
				record.getStatus(),
				record.isCompleted(),
				record.getLastPositionSeconds(),
				record.getCompletedAt());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

}
