package com.agriknowledge.common;

import jakarta.servlet.http.HttpServletRequest;

/** Works out who a request came from, for rate limiting. */
public final class ClientAddress {

	private ClientAddress() {
	}

	/**
	 * Behind Railway the socket address is the proxy, so the caller is the first
	 * entry in X-Forwarded-For. That header is client-controlled and trivially
	 * spoofed, which is fine here: the worst outcome is that an attacker rate-limits
	 * themselves less effectively, and nothing is authorised on the strength of it.
	 */
	public static String of(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			int comma = forwarded.indexOf(',');
			String first = comma == -1 ? forwarded : forwarded.substring(0, comma);
			return first.trim();
		}
		return request.getRemoteAddr();
	}

}
