package com.agriknowledge.material.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentStorageTest {

	private static final byte[] CONTENT = "%PDF-1.7 body".getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("an un-normalised root still accepts ordinary keys")
	void acceptsAnOrdinaryKeyUnderAnUnnormalisedRoot(@TempDir Path temp) throws IOException {
		// Regression. The root was stored exactly as supplied, and the default is
		// the un-normalised "./var/documents". resolve().normalize() collapses the
		// candidate, so it never started with the un-normalised root and every
		// legitimate upload was rejected as a traversal attempt. "temp/sub/.."
		// stands in for "./var/documents": same shape, portable.
		LocalDocumentStorage storage = new LocalDocumentStorage(temp.resolve("sub").resolve(".."));

		storage.store("papers/2024/icar-jrf-a1b2c3.pdf", CONTENT, "application/pdf");

		try (InputStream in = storage.open("papers/2024/icar-jrf-a1b2c3.pdf")) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
	}

	@Test
	void storesAndReadsBack(@TempDir Path temp) throws IOException {
		LocalDocumentStorage storage = new LocalDocumentStorage(temp);
		storage.store("papers/2024/paper.pdf", CONTENT, "application/pdf");

		try (InputStream in = storage.open("papers/2024/paper.pdf")) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
	}

	@Test
	@DisplayName("a key that climbs out of the root is still refused")
	void rejectsTraversal(@TempDir Path temp) {
		LocalDocumentStorage storage = new LocalDocumentStorage(temp);

		assertThatThrownBy(() -> storage.store("../escaped.pdf", CONTENT, "application/pdf"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("escapes");
	}

	@Test
	void cannotSignUrls(@TempDir Path temp) {
		assertThat(new LocalDocumentStorage(temp).signedUrl("k", "n.pdf")).isEmpty();
	}

	@Test
	void deleteIsSafeOnSomethingThatIsNotThere(@TempDir Path temp) {
		new LocalDocumentStorage(temp).delete("papers/nothing.pdf");
	}

}
