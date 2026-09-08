package com.agriknowledge.material.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

/**
 * Chooses where uploaded PDFs go, in the same shape as {@code MailConfig}.
 *
 * <ol>
 *   <li>Cloudflare R2, if credentials are set. The only durable option.</li>
 *   <li>A local directory otherwise, so local work needs no account.</li>
 * </ol>
 */
@Configuration
public class StorageConfig {

	private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

	@Bean
	DocumentStorage documentStorage(
			Environment environment,
			@Value("${app.storage.r2.account-id:}") String accountId,
			@Value("${app.storage.r2.access-key-id:}") String accessKeyId,
			@Value("${app.storage.r2.secret-access-key:}") String secretKey,
			@Value("${app.storage.r2.bucket:}") String bucket,
			@Value("${app.storage.local-dir:./var/documents}") String localDir) {

		boolean configured = !accountId.isBlank() && !accessKeyId.isBlank()
				&& !secretKey.isBlank() && !bucket.isBlank();

		if (configured) {
			return new R2DocumentStorage(accountId, accessKeyId, secretKey, bucket);
		}

		// Local disk is a development convenience, never a production one. Under
		// prod without R2 the upload is refused outright rather than written to a
		// filesystem that the next deploy discards.
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile)) {
				return new UnconfiguredDocumentStorage();
			}
		}
		return new LocalDocumentStorage(Path.of(localDir));
	}

	/**
	 * Separate bean so the warning fires once at startup rather than per upload,
	 * and so it can see the active profiles.
	 */
	@Bean
	StorageReporter storageReporter(DocumentStorage storage, Environment environment) {
		return new StorageReporter(storage, environment);
	}

	static final class StorageReporter {

		private final DocumentStorage storage;
		private final Environment environment;

		StorageReporter(DocumentStorage storage, Environment environment) {
			this.storage = storage;
			this.environment = environment;
		}

		@PostConstruct
		void report() {
			String description = storage.describe();

			if (storage instanceof UnconfiguredDocumentStorage) {
				log.error("No document storage configured, so PDF uploads will be refused. "
						+ "Set R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY and "
						+ "R2_BUCKET to enable them.");
				return;
			}
			if (storage instanceof LocalDocumentStorage) {
				log.info("Document storage: {}. Fine for local work; this is never "
						+ "selected under the prod profile.", description);
				return;
			}
			log.info("Document storage: {}", description);
		}

	}

}
