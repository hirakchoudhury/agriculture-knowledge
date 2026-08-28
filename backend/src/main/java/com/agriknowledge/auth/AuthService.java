package com.agriknowledge.auth;

import com.agriknowledge.auth.dto.LoginRequest;
import com.agriknowledge.auth.dto.RegisterRequest;
import com.agriknowledge.auth.jwt.JwtService;
import com.agriknowledge.common.ConflictException;
import com.agriknowledge.user.User;
import com.agriknowledge.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	/** Deliberately identical for every failure mode, so it leaks nothing. */
	private static final String GENERIC_LOGIN_FAILURE = "Email or password is incorrect";

	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final SessionRevoker sessionRevoker;

	public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
			PasswordEncoder passwordEncoder, JwtService jwtService, SessionRevoker sessionRevoker) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.sessionRevoker = sessionRevoker;
	}

	/** An access token, the raw refresh token to put in a cookie, and the account. */
	public record IssuedSession(String accessToken, String refreshToken, User user) {
	}

	@Transactional
	public IssuedSession register(RegisterRequest request, String userAgent) {
		String email = User.normaliseEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ConflictException("An account with that email already exists");
		}

		User user = User.localAccount(email, passwordEncoder.encode(request.password()), request.name().trim());
		users.save(user);
		log.info("Registered local account {}", user.getId());

		return issueSession(user, userAgent);
	}

	@Transactional
	public IssuedSession login(LoginRequest request, String userAgent) {
		String email = User.normaliseEmail(request.email());
		User user = users.findByEmail(email).orElse(null);

		// A Google-only account has no hash, so there is nothing to compare against.
		if (user == null || user.getPasswordHash() == null) {
			// Still run a hash so that a missing account and a wrong password take
			// roughly the same time, and cannot be told apart by timing.
			passwordEncoder.encode(request.password());
			throw new BadCredentialsException(GENERIC_LOGIN_FAILURE);
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsException(GENERIC_LOGIN_FAILURE);
		}

		if (!user.isEnabled()) {
			throw new BadCredentialsException("This account has been disabled");
		}

		return issueSession(user, userAgent);
	}

	/**
	 * Rotates the refresh token: the presented one is revoked and a fresh one issued.
	 *
	 * <p>If a token that was already revoked comes back, it has been replayed — either
	 * a stolen copy or a client bug. Every session for that account is killed, because
	 * we cannot tell which holder is the legitimate one.
	 */
	@Transactional
	public IssuedSession refresh(String rawToken, String userAgent) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new BadCredentialsException("No refresh token was supplied");
		}

		RefreshToken stored = refreshTokens.findByTokenHash(jwtService.hashRefreshToken(rawToken))
				.orElseThrow(() -> new BadCredentialsException("Refresh token is not recognised"));

		if (stored.getRevokedAt() != null) {
			log.warn("Replayed refresh token for user {} — revoking all sessions", stored.getUser().getId());
			// Committed in its own transaction: the throw below rolls this one back.
			sessionRevoker.revokeAllSessionsFor(stored.getUser().getId());
			throw new BadCredentialsException("Session is no longer valid, please sign in again");
		}

		if (!stored.isUsable(Instant.now())) {
			throw new BadCredentialsException("Session has expired, please sign in again");
		}

		stored.revoke();
		return issueSession(stored.getUser(), userAgent);
	}

	@Transactional
	public void logout(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		// Logging out is idempotent: an unknown token is already "logged out".
		Optional<RefreshToken> stored = refreshTokens.findByTokenHash(jwtService.hashRefreshToken(rawToken));
		stored.ifPresent(RefreshToken::revoke);
	}

	/** Used by the Google sign-in handler, which has already proved who the user is. */
	@Transactional
	public IssuedSession issueSession(User user, String userAgent) {
		String rawRefreshToken = jwtService.generateRefreshToken();
		Instant expiresAt = Instant.now().plus(jwtService.properties().refreshTokenTtl());

		refreshTokens.save(new RefreshToken(
				user,
				jwtService.hashRefreshToken(rawRefreshToken),
				expiresAt,
				truncate(userAgent)));

		return new IssuedSession(jwtService.createAccessToken(user), rawRefreshToken, user);
	}

	@Transactional
	public User findOrCreateGoogleUser(String email, String name, String avatarUrl, String googleSubject) {
		String normalised = User.normaliseEmail(email);
		return users.findByEmail(normalised).orElseGet(() -> {
			User created = User.googleAccount(normalised, name, avatarUrl, googleSubject);
			log.info("Created account from Google sign-in for {}", normalised);
			return users.save(created);
		});
	}

	private String truncate(String userAgent) {
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 400 ? userAgent.substring(0, 400) : userAgent;
	}


}
