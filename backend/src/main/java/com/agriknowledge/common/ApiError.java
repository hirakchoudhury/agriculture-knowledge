package com.agriknowledge.common;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * The single error shape every failing endpoint returns, so the frontend has one
 * thing to parse rather than a different body per failure mode.
 */
public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path) {

	public static ApiError of(HttpStatus status, String message, String path) {
		return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
	}

}
