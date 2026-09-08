package com.agriknowledge.material.storage;

import com.agriknowledge.common.BadRequestException;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * Refuses to store anything, used in production when no object store is configured.
 *
 * <p>The alternative was falling back to local disk, which is worse than useless
 * here: this host replaces its filesystem on every deploy, so an admin would
 * upload a paper, see it succeed, and find it gone at the next push — with no
 * error anywhere to explain it. Failing at the point of upload costs one confused
 * moment; the silent version costs the file.
 */
public class UnconfiguredDocumentStorage implements DocumentStorage {

	private static final String MESSAGE =
			"File uploads are not available: no document storage is configured on this server.";

	@Override
	public void store(String key, byte[] content, String contentType) {
		throw new BadRequestException(MESSAGE);
	}

	@Override
	public Optional<URI> signedUrl(String key, String downloadName) {
		return Optional.empty();
	}

	@Override
	public InputStream open(String key) {
		throw new BadRequestException(MESSAGE);
	}

	@Override
	public void delete(String key) {
		// Nothing was ever stored.
	}

	@Override
	public String describe() {
		return "none (uploads are refused)";
	}

}
