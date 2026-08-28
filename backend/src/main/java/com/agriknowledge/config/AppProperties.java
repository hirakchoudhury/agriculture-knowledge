package com.agriknowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Application settings bound from the {@code app.*} keys in application.yml.
 *
 * @param frontendUrl        the deployed frontend origin, used for CORS and, from phase 2,
 *                           as the redirect target after a successful OAuth2 login
 * @param extraAllowedOrigins additional origins permitted through CORS, e.g. Vercel preview
 *                           deployments. Kept separate from frontendUrl so the OAuth redirect
 *                           always targets exactly one known origin.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl, List<String> extraAllowedOrigins) {

	public List<String> allowedOrigins() {
		if (extraAllowedOrigins == null || extraAllowedOrigins.isEmpty()) {
			return List.of(frontendUrl);
		}
		return java.util.stream.Stream
				.concat(java.util.stream.Stream.of(frontendUrl), extraAllowedOrigins.stream())
				.distinct()
				.toList();
	}

}
