package com.agriknowledge.material;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the bare video id from whatever an admin pastes.
 *
 * <p>The same video reaches people as a watch link, a youtu.be short link, an embed
 * link, a Shorts link, or with a playlist and timestamp glued on. Storing the id
 * rather than the URL means all of those collapse to one row, and the frontend
 * decides how to build the embed.
 */
public final class YouTubeUrls {

	/** Ids are exactly 11 characters of the URL-safe base64 alphabet. */
	private static final String ID = "[A-Za-z0-9_-]{11}";

	private static final List<Pattern> PATTERNS = List.of(
			// https://www.youtube.com/watch?v=ID  (also m.youtube.com, extra params)
			Pattern.compile("[?&]v=(" + ID + ")"),
			// https://youtu.be/ID
			Pattern.compile("youtu\\.be/(" + ID + ")"),
			// https://www.youtube.com/embed/ID and /v/ID
			Pattern.compile("youtube(?:-nocookie)?\\.com/(?:embed|v)/(" + ID + ")"),
			// https://www.youtube.com/shorts/ID
			Pattern.compile("youtube\\.com/shorts/(" + ID + ")"),
			// https://www.youtube.com/live/ID
			Pattern.compile("youtube\\.com/live/(" + ID + ")"));

	private static final Pattern BARE_ID = Pattern.compile("^" + ID + "$");

	private YouTubeUrls() {
	}

	/**
	 * @return the 11-character id, or empty if the input is not a YouTube link this
	 *     recognises. Callers turn that into a 400 rather than storing something the
	 *     player will fail on later.
	 */
	public static Optional<String> extractId(String input) {
		if (input == null || input.isBlank()) {
			return Optional.empty();
		}

		String trimmed = input.trim();

		// An admin may reasonably paste just the id.
		if (BARE_ID.matcher(trimmed).matches()) {
			return Optional.of(trimmed);
		}

		for (Pattern pattern : PATTERNS) {
			Matcher matcher = pattern.matcher(trimmed);
			if (matcher.find()) {
				return Optional.of(matcher.group(1));
			}
		}

		return Optional.empty();
	}

	/**
	 * YouTube serves this without an API key, so a video gets a thumbnail with no
	 * quota to manage. hqdefault exists for every video; maxresdefault does not.
	 */
	public static String thumbnailUrl(String videoId) {
		return "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
	}

}
