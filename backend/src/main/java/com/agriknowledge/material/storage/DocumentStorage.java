package com.agriknowledge.material.storage;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * Where uploaded PDFs actually live.
 *
 * <p>Two implementations for the same reason {@code EmailTransport} has two: the
 * production one needs credentials the developer machine does not have, and local
 * work should not require an object-store account to add a paper.
 */
public interface DocumentStorage {

	/** Writes the object. Overwrites are not expected: keys carry a random suffix. */
	void store(String key, byte[] content, String contentType);

	/**
	 * A short-lived URL the browser can follow directly.
	 *
	 * <p>Empty when the backing store cannot sign URLs, in which case the caller
	 * streams the bytes itself via {@link #open}. Preferring the signed URL keeps
	 * every megabyte a student downloads off the application host.
	 *
	 * @param downloadName the filename the browser should save it as
	 */
	Optional<URI> signedUrl(String key, String downloadName);

	/** Reads the object back, for stores that cannot sign URLs. */
	InputStream open(String key);

	/** Best effort: a failure here must not fail the delete of the row. */
	void delete(String key);

	/** Written to the startup log, so the running configuration is never a mystery. */
	String describe();

}
