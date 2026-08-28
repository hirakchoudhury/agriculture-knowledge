package com.agriknowledge.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Public liveness endpoint. Deliberately separate from Actuator's /actuator/health:
 * this one exists to prove the browser can reach the API through CORS, which is the
 * single thing most likely to be broken on a first deploy.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

	private final String serviceName;
	private final String version;
	private final Environment environment;

	public HealthController(
			@Value("${spring.application.name:agriculture-knowledge-api}") String serviceName,
			@Value("${app.version:0.0.1-SNAPSHOT}") String version,
			Environment environment) {
		this.serviceName = serviceName;
		this.version = version;
		this.environment = environment;
	}

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse(
				"UP",
				serviceName,
				version,
				List.of(environment.getActiveProfiles()),
				Instant.now());
	}

	public record HealthResponse(
			String status,
			String service,
			String version,
			List<String> profiles,
			Instant timestamp) {
	}

}
