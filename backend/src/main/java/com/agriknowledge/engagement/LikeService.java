package com.agriknowledge.engagement;

import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.engagement.dto.LikeResponse;
import com.agriknowledge.material.Material;
import com.agriknowledge.material.MaterialRepository;
import com.agriknowledge.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

	private final MaterialLikeRepository likes;
	private final MaterialRepository materials;
	private final UserRepository users;

	public LikeService(MaterialLikeRepository likes, MaterialRepository materials, UserRepository users) {
		this.likes = likes;
		this.materials = materials;
		this.users = users;
	}

	@Transactional
	public LikeResponse like(Long materialId, Long userId) {
		Material material = readableMaterial(materialId);

		// Idempotent: pressing like twice is the same as pressing it once, which
		// matters when an optimistic UI retries after a dropped connection.
		if (likes.existsByUserIdAndMaterialId(userId, materialId)) {
			return new LikeResponse(true, material.getLikeCount());
		}

		try {
			likes.save(new MaterialLike(users.getReferenceById(userId), material));
			likes.flush();
		}
		catch (DataIntegrityViolationException ex) {
			// Two clicks arriving together; the unique constraint settled it. The
			// database is the arbiter here precisely because the check above races.
			return new LikeResponse(true, material.getLikeCount());
		}

		materials.adjustLikeCount(materialId, 1);

		// The bulk update above does not refresh the entity in memory, so the new
		// value is computed rather than re-read. Concurrent likes from other people
		// may make this a little low; the next page load settles it.
		return new LikeResponse(true, material.getLikeCount() + 1);
	}

	@Transactional
	public LikeResponse unlike(Long materialId, Long userId) {
		Material material = readableMaterial(materialId);

		int removed = likes.deleteByUserAndMaterial(userId, materialId);
		if (removed == 0) {
			// Nothing to undo. Not an error, for the same reason as above.
			return new LikeResponse(false, material.getLikeCount());
		}

		materials.adjustLikeCount(materialId, -removed);
		return new LikeResponse(false, Math.max(0, material.getLikeCount() - removed));
	}

	/**
	 * The viewer's own like state.
	 *
	 * <p>Needed because the material page is server-rendered without the viewer's
	 * access token — that token lives only in browser memory — so the server pass
	 * cannot know whether this reader has liked anything. The button corrects itself
	 * on mount with this one small call rather than re-fetching the whole material.
	 */
	@Transactional(readOnly = true)
	public LikeResponse status(Long materialId, Long userId) {
		Material material = materials.findById(materialId)
				.orElseThrow(() -> NotFoundException.of("Material", materialId));
		return new LikeResponse(
				userId != null && likes.existsByUserIdAndMaterialId(userId, materialId),
				material.getLikeCount());
	}

	private Material readableMaterial(Long materialId) {
		Material material = materials.findById(materialId)
				.orElseThrow(() -> NotFoundException.of("Material", materialId));

		// Liking a draft would let an engagement count exist for something nobody
		// can read, and would leak that the draft exists at all.
		if (!material.isPublished()) {
			throw new BadRequestException("That material is not published");
		}

		return material;
	}

}
