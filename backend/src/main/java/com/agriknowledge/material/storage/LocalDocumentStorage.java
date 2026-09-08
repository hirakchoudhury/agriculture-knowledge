package com.agriknowledge.material.storage;

import com.agriknowledge.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Writes to a directory on disk, for local work without an object-store account.
 *
 * <p>Deliberately not a production option. Railway replaces the container on every
 * deploy, so anything written here is gone at the next push; {@code StorageConfig}
 * says so loudly when this is selected under the prod profile.
 */
public class LocalDocumentStorage implements DocumentStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorage.class);

	private final Path root;

	public LocalDocumentStorage(Path root) {
		// Absolute and normalised up front. The containment check below compares
		// against a normalised candidate, and a relative root like "./var/documents"
		// never prefixes one, so every legitimate key was being rejected.
		this.root = root.toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.root);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not create the document directory " + this.root, ex);
		}
	}

	/**
	 * Keys contain slashes, so this resolves and then verifies the result is still
	 * inside the root. Keys are generated server-side today, but a path check that
	 * only holds while that stays true is the kind that eventually stops holding.
	 */
	private Path resolve(String key) {
		Path candidate = root.resolve(key).normalize();
		if (!candidate.startsWith(root)) {
			throw new IllegalArgumentException("Key escapes the storage root: " + key);
		}
		return candidate;
	}

	@Override
	public void store(String key, byte[] content, String contentType) {
		Path target = resolve(key);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not write " + key, ex);
		}
	}

	@Override
	public Optional<URI> signedUrl(String key, String downloadName) {
		// Nothing to sign against: the caller streams via open() instead.
		return Optional.empty();
	}

	@Override
	public InputStream open(String key) {
		Path source = resolve(key);
		if (!Files.exists(source)) {
			throw new NotFoundException("That file is no longer available.");
		}
		try {
			return Files.newInputStream(source);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read " + key, ex);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		}
		catch (IOException ex) {
			log.warn("Could not delete {}: {}", key, ex.getMessage());
		}
	}

	@Override
	public String describe() {
		return "local disk at " + root.toAbsolutePath();
	}

}
