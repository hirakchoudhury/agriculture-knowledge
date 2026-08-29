package com.agriknowledge.material;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stored article body is rendered as HTML in every reader's browser, so what
 * survives sanitising is the whole security boundary for that feature.
 */
class ArticleHtmlSanitizerTest {

	private final ArticleHtmlSanitizer sanitizer = new ArticleHtmlSanitizer();

	@ParameterizedTest
	@ValueSource(strings = {
			"<script>alert('xss')</script>",
			"<img src=x onerror=alert('xss')>",
			"<a href=\"javascript:alert(1)\">click</a>",
			"<iframe src=\"https://evil.example\"></iframe>",
			"<svg onload=alert(1)>",
			"<body onload=alert(1)>text</body>",
			"<form action=\"https://evil.example\"><input name=\"password\"></form>",
			"<style>body{display:none}</style>",
	})
	@DisplayName("nothing executable survives")
	void stripsExecutableContent(String hostile) {
		String cleaned = sanitizer.sanitize(hostile).toLowerCase();

		assertThat(cleaned).doesNotContain("<script");
		assertThat(cleaned).doesNotContain("javascript:");
		assertThat(cleaned).doesNotContain("onerror");
		assertThat(cleaned).doesNotContain("onload");
		assertThat(cleaned).doesNotContain("<iframe");
		assertThat(cleaned).doesNotContain("<form");
		assertThat(cleaned).doesNotContain("<style");
	}

	@Test
	@DisplayName("ordinary formatting is left alone")
	void keepsWhatAnAuthorActuallyWrites() {
		String body = """
				<h2>Soil pH</h2>
				<p>Phosphorus availability <strong>drops</strong> outside <em>pH 6 to 7</em>.</p>
				<ul><li>Test in spring</li><li>Retest after liming</li></ul>
				<blockquote>Lime raises pH slowly.</blockquote>
				<a href="https://example.com/soil">Further reading</a>
				""";

		String cleaned = sanitizer.sanitize(body);

		assertThat(cleaned).contains("<h2>", "Soil pH");
		assertThat(cleaned).contains("<strong>", "<em>");
		assertThat(cleaned).contains("<ul>", "<li>");
		assertThat(cleaned).contains("<blockquote>");
		assertThat(cleaned).contains("https://example.com/soil");
	}

	@Test
	@DisplayName("a link that opens elsewhere cannot reach back through window.opener")
	void addsRelToLinks() {
		String cleaned = sanitizer.sanitize("<a href=\"https://example.com\">x</a>");

		assertThat(cleaned).contains("rel=");
		assertThat(cleaned).contains("nofollow");
	}

	@Test
	void handlesNullAndEmpty() {
		assertThat(sanitizer.sanitize(null)).isEmpty();
		assertThat(sanitizer.sanitize("")).isEmpty();
	}

	@Test
	@DisplayName("reading time is never zero, and grows with the text")
	void estimatesReadingTime() {
		assertThat(sanitizer.estimateReadingMinutes(null)).isEqualTo(1);
		assertThat(sanitizer.estimateReadingMinutes("")).isEqualTo(1);
		assertThat(sanitizer.estimateReadingMinutes("<p>Three short words</p>")).isEqualTo(1);

		String longArticle = "<p>" + "word ".repeat(1000) + "</p>";
		assertThat(sanitizer.estimateReadingMinutes(longArticle)).isEqualTo(5);
	}

	@Test
	@DisplayName("markup is not counted as words")
	void ignoresTagsWhenCountingWords() {
		String wrapped = "<div><p><strong>one</strong> <em>two</em></p></div>";

		assertThat(sanitizer.estimateReadingMinutes(wrapped)).isEqualTo(1);
	}

}
