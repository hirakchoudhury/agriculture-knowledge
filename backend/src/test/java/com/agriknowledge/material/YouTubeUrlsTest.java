package com.agriknowledge.material;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeUrlsTest {

	private static final String ID = "dQw4w9WgXcQ";

	@ParameterizedTest
	@ValueSource(strings = {
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ",
			"http://youtube.com/watch?v=dQw4w9WgXcQ",
			"https://m.youtube.com/watch?v=dQw4w9WgXcQ",
			"https://youtu.be/dQw4w9WgXcQ",
			"https://www.youtube.com/embed/dQw4w9WgXcQ",
			"https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
			"https://www.youtube.com/v/dQw4w9WgXcQ",
			"https://www.youtube.com/shorts/dQw4w9WgXcQ",
			"https://www.youtube.com/live/dQw4w9WgXcQ",
			"dQw4w9WgXcQ",
			"  https://youtu.be/dQw4w9WgXcQ  ",
	})
	@DisplayName("every shape a video reaches us in collapses to the same id")
	void extractsTheId(String input) {
		assertThat(YouTubeUrls.extractId(input)).contains(ID);
	}

	@Test
	@DisplayName("playlists and timestamps are discarded, not stored")
	void ignoresTrailingParameters() {
		assertThat(YouTubeUrls.extractId("https://www.youtube.com/watch?v=" + ID + "&t=42s"))
				.contains(ID);
		assertThat(YouTubeUrls.extractId("https://www.youtube.com/watch?list=PL1&v=" + ID))
				.contains(ID);
		assertThat(YouTubeUrls.extractId("https://youtu.be/" + ID + "?t=30")).contains(ID);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://vimeo.com/12345",
			"https://example.com/watch?v=dQw4w9WgXcQ",
			"https://www.youtube.com/watch?v=dQw4w9WgXcQextra",
			"https://youtu.be/dQw4w9WgXcQextra",
			"not a url at all",
			"dQw4w9WgXc",
			"",
			"   ",
	})
	@DisplayName("anything else is rejected rather than half-parsed")
	void rejectsWhatItCannotRead(String input) {
		assertThat(YouTubeUrls.extractId(input)).isEmpty();
	}

	@Test
	void handlesNull() {
		assertThat(YouTubeUrls.extractId(null)).isEmpty();
	}

	@Test
	void buildsAThumbnailUrlFromTheId() {
		assertThat(YouTubeUrls.thumbnailUrl(ID))
				.isEqualTo("https://i.ytimg.com/vi/" + ID + "/hqdefault.jpg");
	}

}
