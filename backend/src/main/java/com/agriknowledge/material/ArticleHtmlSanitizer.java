package com.agriknowledge.material;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Cleans article HTML before it is stored.
 *
 * <p>Only admins write articles, so this is not defence against a hostile author.
 * It is defence against the one bug in this application that would execute in every
 * reader's browser: a script reaching the body by way of a pasted fragment, a
 * compromised admin account, or an editor emitting more than expected. Sanitising
 * on write means the stored value is already safe, so no reader depends on the
 * frontend escaping it correctly at render time.
 */
@Component
public class ArticleHtmlSanitizer {

	/**
	 * Built from the library's vetted presets rather than hand-rolled rules.
	 * LINKS already restricts href to standard protocols (so javascript: cannot
	 * survive) and forces rel=nofollow. The extra builder only adds elements the
	 * presets leave out.
	 */
	private static final PolicyFactory POLICY = Sanitizers.FORMATTING
			.and(Sanitizers.BLOCKS)
			.and(Sanitizers.TABLES)
			.and(Sanitizers.LINKS)
			.and(Sanitizers.IMAGES)
			.and(new HtmlPolicyBuilder()
					.allowElements("ul", "ol", "li", "pre", "code", "hr", "br", "figure", "figcaption")
					.toFactory());

	public String sanitize(String rawHtml) {
		if (rawHtml == null) {
			return "";
		}
		return POLICY.sanitize(rawHtml);
	}

	/**
	 * A rough reading time from the visible text. Deliberately not exact: readers
	 * use it to decide whether they have time now, not to schedule anything.
	 */
	public int estimateReadingMinutes(String html) {
		if (html == null || html.isBlank()) {
			return 1;
		}
		String text = html.replaceAll("<[^>]+>", " ").trim();
		if (text.isEmpty()) {
			return 1;
		}
		int words = text.split("\\s+").length;
		return Math.max(1, (int) Math.ceil(words / 200.0));
	}

}
