package com.agriknowledge.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Revokes sessions in a transaction of its own.
 *
 * <p>This exists because of a trap: the caller detects a replayed refresh token,
 * revokes every session, and then throws to reject the request. Throwing rolls the
 * caller's transaction back — taking the revocation with it — so the defence would
 * quietly do nothing. REQUIRES_NEW commits the revocation independently.
 *
 * <p>It is a separate bean rather than a method on AuthService because Spring's
 * transactional proxying does not apply to self-invocation.
 */
@Component
public class SessionRevoker {

	private static final Logger log = LoggerFactory.getLogger(SessionRevoker.class);

	private final RefreshTokenRepository refreshTokens;

	public SessionRevoker(RefreshTokenRepository refreshTokens) {
		this.refreshTokens = refreshTokens;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revokeAllSessionsFor(Long userId) {
		int revoked = refreshTokens.revokeAllForUserId(userId, Instant.now());
		log.warn("Revoked {} active session(s) for user {}", revoked, userId);
	}

}
