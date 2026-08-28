package com.agriknowledge.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound from {@code app.jwt.*}.
 *
 * @param secret                 base64-encoded HMAC key, at least 32 bytes decoded
 * @param issuer                 the {@code iss} claim, checked on every incoming token
 * @param accessTokenTtl         short by design; a stolen access token expires quickly
 * @param refreshTokenTtl        how long a session survives without re-authenticating
 * @param refreshCookieName      name of the HttpOnly cookie carrying the refresh token
 * @param refreshCookieSameSite  "None" in production, because the frontend and API sit
 *                               on different sites. "Lax" is enough when both are on
 *                               localhost and avoids needing HTTPS locally.
 * @param refreshCookieSecure    must be true whenever SameSite is None
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		String secret,
		String issuer,
		Duration accessTokenTtl,
		Duration refreshTokenTtl,
		String refreshCookieName,
		String refreshCookieSameSite,
		boolean refreshCookieSecure) {
}
