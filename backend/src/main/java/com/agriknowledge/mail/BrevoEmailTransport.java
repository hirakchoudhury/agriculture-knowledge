package com.agriknowledge.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends through Brevo's HTTP API rather than SMTP.
 *
 * <p>This exists because Railway blocks outbound SMTP. An HTTP call to port 443
 * is indistinguishable from any other API request, so it goes straight out.
 *
 * <p>Brevo's free tier allows 300 messages a day and will deliver to any address
 * without owning a domain, which is what a public sign-up flow needs.
 */
public class BrevoEmailTransport implements EmailTransport {

	private static final Logger log = LoggerFactory.getLogger(BrevoEmailTransport.class);
	private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

	private final RestClient client;
	private final String fromEmail;
	private final String fromName;

	public BrevoEmailTransport(String apiKey, String fromEmail, String fromName) {
		this.fromEmail = fromEmail;
		this.fromName = fromName;
		this.client = RestClient.builder()
				.baseUrl(ENDPOINT)
				.defaultHeader("api-key", apiKey)
				.defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	public boolean send(String to, String subject, String body) {
		Map<String, Object> payload = Map.of(
				"sender", Map.of("email", fromEmail, "name", fromName),
				"to", List.of(Map.of("email", to)),
				"subject", subject,
				"textContent", body);

		try {
			client.post()
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
			return true;
		}
		catch (Exception ex) {
			// Deliberately not rethrown. The code is already stored, so the person
			// can ask for another one; failing their whole request would be worse.
			log.error("Brevo rejected the message to {}: {}", to, ex.getMessage());
			return false;
		}
	}

	@Override
	public String describe() {
		return "Brevo HTTP API, sending as " + fromEmail;
	}

	/** Kept for reference: the API is a plain POST, so nothing here needs a client library. */
	static Duration timeout() {
		return Duration.ofSeconds(15);
	}

}
