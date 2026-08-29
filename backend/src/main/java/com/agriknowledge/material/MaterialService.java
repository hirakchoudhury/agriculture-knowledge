package com.agriknowledge.material;

import com.agriknowledge.catalog.Exam;
import com.agriknowledge.catalog.ExamRepository;
import com.agriknowledge.catalog.Topic;
import com.agriknowledge.catalog.TopicRepository;
import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.common.NotFoundException;
import com.agriknowledge.common.PageResponse;
import com.agriknowledge.common.Slugs;
import com.agriknowledge.engagement.MaterialLikeRepository;
import com.agriknowledge.material.dto.ArticleRequest;
import com.agriknowledge.material.dto.MaterialDetail;
import com.agriknowledge.material.dto.MaterialSummary;
import com.agriknowledge.material.dto.VideoRequest;
import com.agriknowledge.user.User;
import com.agriknowledge.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MaterialService {

	/**
	 * Sorting is restricted to a known list rather than passed through to JPA. A
	 * client that can name any property can sort by things that are not indexed, and
	 * can probe the schema through error messages.
	 */
	private static final Map<String, Sort> SORTS = Map.of(
			"newest", Sort.by(Sort.Direction.DESC, "publishedAt"),
			"oldest", Sort.by(Sort.Direction.ASC, "publishedAt"),
			"popular", Sort.by(Sort.Direction.DESC, "viewCount"),
			"liked", Sort.by(Sort.Direction.DESC, "likeCount"));

	private static final int MAX_PAGE_SIZE = 50;

	private final MaterialRepository materials;
	private final TopicRepository topics;
	private final ExamRepository exams;
	private final UserRepository users;
	private final ArticleHtmlSanitizer sanitizer;
	// The material package reaching into engagement is a deliberate compromise: the
	// alternative is a second round trip per page purely to colour in like buttons.
	private final MaterialLikeRepository likes;
	private final MaterialSearch search;

	public MaterialService(MaterialRepository materials, TopicRepository topics, ExamRepository exams,
			UserRepository users, ArticleHtmlSanitizer sanitizer, MaterialLikeRepository likes,
			MaterialSearch search) {
		this.materials = materials;
		this.topics = topics;
		this.exams = exams;
		this.users = users;
		this.sanitizer = sanitizer;
		this.likes = likes;
		this.search = search;
	}

	// ----- reads -------------------------------------------------------------

	public PageResponse<MaterialSummary> search(MaterialStatus status, MaterialType type,
			Difficulty difficulty, Long topicId, Long examId, String query,
			int page, int size, String sort, Long viewerId) {

		Pageable pageable = PageRequest.of(
				Math.max(0, page),
				Math.clamp(size, 1, MAX_PAGE_SIZE),
				SORTS.getOrDefault(sort == null ? "newest" : sort, SORTS.get("newest")));

		// A free-text query goes through the tsvector index and comes back ranked;
		// everything else uses the plain filtered query, sorted as the caller asked.
		Page<Material> results = (query == null || query.isBlank())
				? materials.search(status, type, difficulty, topicId, examId, null, pageable)
				: fullTextSearch(status, type, difficulty, topicId, examId, query.trim(), pageable);
		Map<Long, List<String>> topicNames = topicNamesFor(results.getContent());
		Set<Long> liked = likedIdsFor(results.getContent(), viewerId);

		return PageResponse.of(results, material -> toSummary(material, topicNames, liked));
	}

	/**
	 * Runs the ranked search, then loads the entities for that page and puts them
	 * back into rank order — {@code where id in (...)} makes no promise about order.
	 */
	private Page<Material> fullTextSearch(MaterialStatus status, MaterialType type,
			Difficulty difficulty, Long topicId, Long examId, String query, Pageable pageable) {

		Page<Long> ids = search.findMatchingIds(status, type, difficulty, topicId, examId, query, pageable);
		if (ids.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, ids.getTotalElements());
		}

		Map<Long, Material> byId = new HashMap<>();
		for (Material material : materials.findAllByIdIn(ids.getContent())) {
			byId.put(material.getId(), material);
		}

		List<Material> ordered = ids.getContent().stream()
				.map(byId::get)
				.filter(java.util.Objects::nonNull)
				.toList();

		return new PageImpl<>(ordered, pageable, ids.getTotalElements());
	}

	@Transactional
	public MaterialDetail getBySlug(String slug, boolean viewerIsAdmin, Long viewerId) {
		Material material = materials.findBySlug(slug)
				.orElseThrow(() -> new NotFoundException("No material with slug '%s'".formatted(slug)));

		// A draft is visible to its admins and to nobody else. 404 rather than 403,
		// so an unpublished slug cannot be probed for existence.
		if (!material.isPublished() && !viewerIsAdmin) {
			throw new NotFoundException("No material with slug '%s'".formatted(slug));
		}

		if (material.isPublished()) {
			materials.incrementViewCount(material.getId());
		}

		return toDetail(material, viewerId != null
				&& likes.existsByUserIdAndMaterialId(viewerId, material.getId()));
	}

	// ----- writes ------------------------------------------------------------

	@Transactional
	public MaterialDetail createArticle(ArticleRequest request, Long authorId) {
		String safeHtml = sanitizer.sanitize(request.bodyHtml());
		Article article = new Article(
				request.title().trim(),
				Slugs.uniqueFrom(request.title(), materials::existsBySlug),
				request.summary(),
				request.thumbnailUrl(),
				request.difficulty(),
				author(authorId),
				safeHtml,
				sanitizer.estimateReadingMinutes(safeHtml));

		applyTags(article, request.topicIds(), request.examIds());
		return toDetail(materials.save(article), false);
	}

	@Transactional
	public MaterialDetail createVideo(VideoRequest request, Long authorId) {
		String videoId = YouTubeUrls.extractId(request.youtubeUrl())
				.orElseThrow(() -> new BadRequestException(
						"That does not look like a YouTube link. Paste a watch, youtu.be, "
								+ "embed or shorts URL, or the 11-character video id."));

		Video video = new Video(
				request.title().trim(),
				Slugs.uniqueFrom(request.title(), materials::existsBySlug),
				request.summary(),
				// A video always has a thumbnail available for free, so default to it.
				request.thumbnailUrl() == null || request.thumbnailUrl().isBlank()
						? YouTubeUrls.thumbnailUrl(videoId)
						: request.thumbnailUrl(),
				request.difficulty(),
				author(authorId),
				videoId,
				request.durationSeconds());

		applyTags(video, request.topicIds(), request.examIds());
		return toDetail(materials.save(video), false);
	}

	@Transactional
	public MaterialDetail updateArticle(Long id, ArticleRequest request) {
		Article article = load(id, Article.class, "Article");

		applyShared(article, request.title(), request.summary(), request.thumbnailUrl(), request.difficulty());
		String safeHtml = sanitizer.sanitize(request.bodyHtml());
		article.setBodyHtml(safeHtml);
		article.setReadingMinutes(sanitizer.estimateReadingMinutes(safeHtml));
		applyTags(article, request.topicIds(), request.examIds());

		return toDetail(article, false);
	}

	@Transactional
	public MaterialDetail updateVideo(Long id, VideoRequest request) {
		Video video = load(id, Video.class, "Video");

		String videoId = YouTubeUrls.extractId(request.youtubeUrl())
				.orElseThrow(() -> new BadRequestException("That does not look like a YouTube link."));

		applyShared(video, request.title(), request.summary(), request.thumbnailUrl(), request.difficulty());
		video.setYoutubeId(videoId);
		video.setDurationSeconds(request.durationSeconds());
		applyTags(video, request.topicIds(), request.examIds());

		return toDetail(video, false);
	}

	@Transactional
	public MaterialDetail setStatus(Long id, MaterialStatus status) {
		Material material = materials.findById(id)
				.orElseThrow(() -> NotFoundException.of("Material", id));

		if (status == MaterialStatus.PUBLISHED) {
			material.publish();
		}
		else {
			material.unpublish(status);
		}

		return toDetail(material, false);
	}

	/**
	 * Archives rather than deletes. From phase 5 onwards comments, likes and progress
	 * all reference a material; removing the row would take them with it.
	 */
	@Transactional
	public void archive(Long id) {
		setStatus(id, MaterialStatus.ARCHIVED);
	}

	// ----- helpers -----------------------------------------------------------

	private User author(Long authorId) {
		return users.findById(authorId).orElseThrow(() -> NotFoundException.of("Account", authorId));
	}

	private <T extends Material> T load(Long id, Class<T> expected, String label) {
		Material material = materials.findById(id).orElseThrow(() -> NotFoundException.of(label, id));
		if (!expected.isInstance(material)) {
			throw new BadRequestException(
					"Material %d is a %s, not a %s".formatted(id, material.getType(), label.toLowerCase(Locale.ROOT)));
		}
		return expected.cast(material);
	}

	private void applyShared(Material material, String title, String summary,
			String thumbnailUrl, Difficulty difficulty) {
		// The slug is left alone on rename: it is already in published URLs.
		material.setTitle(title.trim());
		material.setSummary(summary);
		if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
			material.setThumbnailUrl(thumbnailUrl);
		}
		material.setDifficulty(difficulty);
	}

	private void applyTags(Material material, List<Long> topicIds, List<Long> examIds) {
		material.replaceTopics(resolveTopics(topicIds));
		material.replaceExams(resolveExams(examIds));
	}

	private Set<Topic> resolveTopics(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Set.of();
		}
		Set<Long> wanted = new LinkedHashSet<>(ids);
		List<Topic> found = topics.findAllById(wanted);
		if (found.size() != wanted.size()) {
			throw new NotFoundException("Unknown topic id in " + wanted);
		}
		return new LinkedHashSet<>(found);
	}

	private Set<Exam> resolveExams(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Set.of();
		}
		Set<Long> wanted = new LinkedHashSet<>(ids);
		List<Exam> found = exams.findAllById(wanted);
		if (found.size() != wanted.size()) {
			throw new NotFoundException("Unknown exam id in " + wanted);
		}
		return new LinkedHashSet<>(found);
	}

	/**
	 * Topic names for a whole page in one query. Reading {@code material.getTopics()}
	 * per row is the textbook N+1: 20 cards would cost 21 queries.
	 */
	private Map<Long, List<String>> topicNamesFor(Collection<Material> page) {
		if (page.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = page.stream().map(Material::getId).toList();
		Map<Long, List<String>> byMaterial = new HashMap<>();
		for (Object[] row : materials.findTopicNamesFor(ids)) {
			byMaterial.computeIfAbsent((Long) row[0], key -> new ArrayList<>()).add((String) row[1]);
		}
		return byMaterial;
	}

	/** Which of these the viewer already liked, in one query rather than one per card. */
	private Set<Long> likedIdsFor(Collection<Material> page, Long viewerId) {
		if (viewerId == null || page.isEmpty()) {
			return Set.of();
		}
		return Set.copyOf(likes.findLikedMaterialIds(viewerId, page.stream().map(Material::getId).toList()));
	}

	private MaterialSummary toSummary(Material material, Map<Long, List<String>> topicNames,
			Set<Long> likedIds) {
		return new MaterialSummary(
				material.getId(),
				material.getType(),
				material.getTitle(),
				material.getSlug(),
				material.getSummary(),
				material.getThumbnailUrl(),
				material.getDifficulty(),
				material.getStatus(),
				material.getPublishedAt(),
				material.getViewCount(),
				material.getLikeCount(),
				material.getCommentCount(),
				topicNames.getOrDefault(material.getId(), List.of()),
				likedIds.contains(material.getId()));
	}

	private MaterialDetail toDetail(Material material, boolean likedByMe) {
		String bodyHtml = null;
		Integer readingMinutes = null;
		String youtubeId = null;
		Integer durationSeconds = null;

		if (material instanceof Article article) {
			bodyHtml = article.getBodyHtml();
			readingMinutes = article.getReadingMinutes();
		}
		else if (material instanceof Video video) {
			youtubeId = video.getYoutubeId();
			durationSeconds = video.getDurationSeconds();
		}

		return new MaterialDetail(
				material.getId(),
				material.getType(),
				material.getTitle(),
				material.getSlug(),
				material.getSummary(),
				material.getThumbnailUrl(),
				material.getDifficulty(),
				material.getStatus(),
				material.getAuthor().getName(),
				material.getPublishedAt(),
				material.getUpdatedAt(),
				material.getViewCount(),
				material.getLikeCount(),
				material.getCommentCount(),
				material.getTopics().stream()
						.map(topic -> new MaterialDetail.TagRef(topic.getId(), topic.getName(), topic.getSlug()))
						.toList(),
				material.getExams().stream()
						.map(exam -> new MaterialDetail.TagRef(exam.getId(), exam.getName(), exam.getSlug()))
						.toList(),
				bodyHtml,
				readingMinutes,
				youtubeId,
				durationSeconds,
				likedByMe);
	}

}
