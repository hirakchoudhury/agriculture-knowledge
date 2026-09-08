package com.agriknowledge.material.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Cloudflare R2, through the S3 API it implements.
 *
 * <p>R2 rather than S3 proper because downloads are almost all of the traffic a
 * question-paper archive generates, and R2 does not charge for egress. The same
 * class works against S3 or any other S3-compatible store by changing the
 * endpoint.
 */
public class R2DocumentStorage implements DocumentStorage {

	private static final Logger log = LoggerFactory.getLogger(R2DocumentStorage.class);

	/** Long enough to start a download on a slow connection, short enough not to be worth sharing. */
	private static final Duration LINK_TTL = Duration.ofMinutes(10);

	private final S3Client client;
	private final S3Presigner presigner;
	private final String bucket;

	public R2DocumentStorage(String accountId, String accessKeyId, String secretAccessKey, String bucket) {
		this.bucket = bucket;
		URI endpoint = URI.create("https://%s.r2.cloudflarestorage.com".formatted(accountId));
		StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKeyId, secretAccessKey));

		// R2 ignores the region but the SDK insists on one being set.
		this.client = S3Client.builder()
				.endpointOverride(endpoint)
				.credentialsProvider(credentials)
				.region(Region.US_EAST_1)
				.build();

		this.presigner = S3Presigner.builder()
				.endpointOverride(endpoint)
				.credentialsProvider(credentials)
				.region(Region.US_EAST_1)
				.build();
	}

	@Override
	public void store(String key, byte[] content, String contentType) {
		client.putObject(
				PutObjectRequest.builder()
						.bucket(bucket)
						.key(key)
						.contentType(contentType)
						.contentLength((long) content.length)
						.build(),
				RequestBody.fromBytes(content));
	}

	@Override
	public Optional<URI> signedUrl(String key, String downloadName) {
		// Set on the signed request rather than on the stored object, so the same
		// object can be served under whatever name the row currently says.
		String disposition = "attachment; filename=\"%s\"".formatted(downloadName.replace("\"", ""));

		GetObjectRequest get = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.responseContentDisposition(disposition)
				.responseContentType("application/pdf")
				.build();

		return Optional.of(presigner.presignGetObject(
						GetObjectPresignRequest.builder()
								.signatureDuration(LINK_TTL)
								.getObjectRequest(get)
								.build())
				.url()
				.toString())
				.map(URI::create);
	}

	@Override
	public InputStream open(String key) {
		return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
	}

	@Override
	public void delete(String key) {
		try {
			client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
		}
		catch (Exception ex) {
			// The row is already gone by this point. An orphaned object costs a few
			// kilobytes; failing the request would leave the admin unable to delete.
			log.warn("Could not delete {} from R2: {}", key, ex.getMessage());
		}
	}

	@Override
	public String describe() {
		return "Cloudflare R2, bucket " + bucket;
	}

}
