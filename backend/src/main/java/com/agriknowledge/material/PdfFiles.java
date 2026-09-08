package com.agriknowledge.material;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

/**
 * Checks and names uploaded PDFs.
 *
 * <p>Nothing here trusts the client. The declared Content-Type and the file
 * extension are both set by whoever is uploading, so neither is evidence of
 * anything; the leading bytes are. And the supplied filename is attacker-chosen
 * text that ends up in a Content-Disposition header and a storage key, so it is
 * rebuilt from scratch rather than sanitised in place.
 */
public final class PdfFiles {

	/** Every PDF begins with these five bytes, per ISO 32000 section 7.5.2. */
	private static final byte[] MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	/** Generous for a scanned paper, small enough that one upload cannot exhaust the container. */
	public static final long MAX_BYTES = 25L * 1024 * 1024;

	private static final int MAX_NAME_LENGTH = 120;

	private PdfFiles() {
	}

	/**
	 * @return true if the content actually starts with the PDF signature
	 */
	public static boolean looksLikePdf(byte[] head) {
		if (head == null || head.length < MAGIC.length) {
			return false;
		}
		for (int i = 0; i < MAGIC.length; i++) {
			if (head[i] != MAGIC[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Rebuilds a safe download filename from an arbitrary uploaded one.
	 *
	 * <p>Built from an allow-list rather than by removing bad characters: a
	 * deny-list has to anticipate every encoding of "../", and this does not.
	 */
	public static String safeFileName(String supplied) {
		String base = supplied == null ? "" : supplied;

		// Drop any directory part, whichever separator was used.
		int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
		if (slash >= 0) {
			base = base.substring(slash + 1);
		}
		if (base.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			base = base.substring(0, base.length() - 4);
		}

		StringBuilder cleaned = new StringBuilder(base.length());
		for (char c : base.toCharArray()) {
			boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
					|| (c >= '0' && c <= '9') || c == ' ' || c == '-' || c == '_' || c == '.';
			cleaned.append(allowed ? c : ' ');
		}

		String result = cleaned.toString().replaceAll("\\s+", " ").trim();
		if (result.length() > MAX_NAME_LENGTH) {
			result = result.substring(0, MAX_NAME_LENGTH).trim();
		}
		// A name of only disallowed characters collapses to nothing, and an empty
		// Content-Disposition filename makes browsers invent their own.
		return (result.isEmpty() ? "document" : result) + ".pdf";
	}

	/**
	 * Builds the object key.
	 *
	 * <p>Contains a random component so that two papers with the same title never
	 * collide, and so the key cannot be guessed from the title if the bucket is
	 * ever misconfigured.
	 */
	public static String storageKey(Integer paperYear, String slug) {
		int year = paperYear != null ? paperYear : Year.now().getValue();
		String safeSlug = slug == null || slug.isBlank() ? "paper" : slug;
		if (safeSlug.length() > 60) {
			safeSlug = safeSlug.substring(0, 60);
		}
		String random = UUID.randomUUID().toString().substring(0, 8);
		return "papers/%d/%s-%s.pdf".formatted(year, safeSlug, random);
	}

}
