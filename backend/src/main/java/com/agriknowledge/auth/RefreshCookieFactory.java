package com.agriknowledge.auth;

import com.agriknowledge.auth.jwt.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the refresh-token cookie.
 *
 * <p>Path is scoped to the auth endpoints so the cookie is not attached to every
 * request the browser makes — only the two that actually need it.
 */
@Component
public class RefreshCookieFactory {

	static final String COOKIE_PATH = "/api/v1/auth";

	private final JwtProperties properties;

	public RefreshCookieFactory(JwtProperties properties) {
		this.properties = properties;
	}

	public ResponseCookie issue(String rawToken) {
		return base(rawToken).maxAge(properties.refreshTokenTtl()).build();
	}

	/** A zero-age cookie with the same attributes is how a cookie gets deleted. */
	public ResponseCookie clear() {
		return base("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(properties.refreshCookieName(), value)
				.httpOnly(true)
				.secure(properties.refreshCookieSecure())
				.sameSite(properties.refreshCookieSameSite())
				.path(COOKIE_PATH);
	}

}
