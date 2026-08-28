package com.agriknowledge.auth.dto;

/**
 * The refresh token is deliberately absent: it travels only as an HttpOnly cookie,
 * where page JavaScript cannot read it.
 *
 * @param expiresInSeconds lets the client schedule a refresh instead of waiting
 *     for a 401 round trip
 */
public record AuthResponse(
		String accessToken,
		long expiresInSeconds,
		UserResponse user) {
}
