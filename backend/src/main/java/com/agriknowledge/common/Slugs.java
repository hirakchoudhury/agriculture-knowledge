package com.agriknowledge.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Turns a display name into a URL-safe slug.
 *
 * <p>Slugs are part of the public URL, so once something is published its slug
 * should not change: old links and any search ranking depend on it.
 */
public final class Slugs {

	private static final int MAX_LENGTH = 180;

	private Slugs() {
	}

	public static String from(String input) {
		if (input == null || input.isBlank()) {
			throw new IllegalArgumentException("Cannot build a slug from an empty name");
		}

		String slug = stripAccents(input).toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");

		if (slug.isEmpty()) {
			throw new IllegalArgumentException("Name '%s' contains no usable characters".formatted(input));
		}

		return slug.length() > MAX_LENGTH ? trimAtBoundary(slug) : slug;
	}

	/**
	 * Adds a numeric suffix until the slug is free, so two topics both called
	 * "Soil pH" can coexist instead of the second failing on the unique index.
	 */
	public static String uniqueFrom(String input, Predicate<String> isTaken) {
		String base = from(input);
		if (!isTaken.test(base)) {
			return base;
		}
		for (int suffix = 2; suffix < 1000; suffix++) {
			String candidate = base + "-" + suffix;
			if (!isTaken.test(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Could not find a free slug for '%s'".formatted(input));
	}

	/**
	 * NFKD splits an accented character into its base letter plus a combining mark,
	 * so dropping the marks turns "Pomología" into "pomologia" rather than losing
	 * the letter entirely and yielding "pomolog".
	 */
	private static String stripAccents(String input) {
		String decomposed = Normalizer.normalize(input, Normalizer.Form.NFKD);
		StringBuilder result = new StringBuilder(decomposed.length());
		decomposed.codePoints()
				.filter(codePoint -> Character.getType(codePoint) != Character.NON_SPACING_MARK)
				.forEach(result::appendCodePoint);
		return result.toString();
	}

	private static String trimAtBoundary(String slug) {
		String cut = slug.substring(0, MAX_LENGTH);
		int lastDash = cut.lastIndexOf('-');
		return lastDash > 0 ? cut.substring(0, lastDash) : cut;
	}

}
