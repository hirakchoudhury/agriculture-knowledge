package com.agriknowledge.auth;

import com.agriknowledge.auth.dto.LoginRequest;
import com.agriknowledge.auth.dto.RegisterRequest;
import com.agriknowledge.auth.dto.ResetPasswordRequest;
import com.agriknowledge.auth.dto.VerifyEmailRequest;
import com.agriknowledge.auth.jwt.JwtService;
import com.agriknowledge.common.ConflictException;
import com.agriknowledge.common.EmailNotVerifiedException;
import com.agriknowledge.user.AuthProvider;
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
	private final VerificationService verification;

	public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
			PasswordEncoder passwordEncoder, JwtService jwtService, SessionRevoker sessionRevoker,
			VerificationService verification) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.sessionRevoker = sessionRevoker;
		this.verification = verification;
	}

	/** An access token, the raw refresh token to put in a cookie, and the account. */
	public record IssuedSession(String accessToken, String refreshToken, User user) {
	}

	/**
	 * Creates an unverified account and emails a code.
	 *
	 * <p>Deliberately returns no session. The account cannot be used until the
	 * address is proved, so signing them in here would hand out a token for an
	 * account that can do nothing.
	 */
	@Transactional
	public User register(RegisterRequest request) {
		String email = User.normaliseEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ConflictException("An account with that email already exists");
		}

		User user = User.localAccount(email, passwordEncoder.encode(request.password()), request.name().trim());
		users.save(user);
		log.info("Registered local account {}, pending verification", user.getId());

		verification.issueEmailVerification(user);
		return user;
	}

	/** Proves the address, then signs them straight in so they are not asked twice. */
	@Transactional
	public IssuedSession verifyEmail(VerifyEmailRequest request, String userAgent) {
		VerificationService.assertLooksLikeCode(request.code());

		User user = users.findByEmail(User.normaliseEmail(request.email()))
				.orElseThrow(() -> new BadCredentialsException("That code is not valid. Ask for a new one."));

		if (user.isEmailVerified()) {
			// Already done. Treated as success rather than an error: a second click
			// on the same link should not look like a failure.
			return issueSession(user, userAgent);
		}

		verification.consume(user, VerificationPurpose.EMAIL_VERIFICATION, request.code());
		user.markEmailVerified();
		log.info("Verified email for user {}", user.getId());

		return issueSession(user, userAgent);
	}

	/**
	 * Sends another verification code.
	 *
	 * <p>Returns silently whatever the address is. Saying "no such account" here
	 * would turn this endpoint into a way to discover who has registered.
	 */
	@Transactional
	public void resendVerification(String rawEmail) {
		users.findByEmail(User.normaliseEmail(rawEmail))
				.filter(user -> !user.isEmailVerified())
				.filter(user -> user.getProvider() == AuthProvider.LOCAL)
				.ifPresent(verification::issueEmailVerification);
	}

	/** Same silence, for the same reason. */
	@Transactional
	public void forgotPassword(String rawEmail) {
		users.findByEmail(User.normaliseEmail(rawEmail))
				// A Google account has no password here to reset; sending a code
				// would only confuse someone into thinking it changed their Google one.
				.filter(user -> user.getProvider() == AuthProvider.LOCAL)
				.ifPresent(verification::issuePasswordReset);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		VerificationService.assertLooksLikeCode(request.code());

		User user = users.findByEmail(User.normaliseEmail(request.email()))
				.orElseThrow(() -> new BadCredentialsException("That code is not valid. Ask for a new one."));

		verification.consume(user, VerificationPurpose.PASSWORD_RESET, request.code());
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

		// Someone resetting a password may be recovering from a compromise, so
		// every existing session goes. Committed separately because it must survive
		// even if something later in this request fails.
		sessionRevoker.revokeAllSessionsFor(user.getId());

		// Reaching the inbox proves the address as surely as the sign-up code does.
		user.markEmailVerified();
		log.info("Password reset for user {}; all sessions revoked", user.getId());
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

		// Checked only after the password matches. Doing it earlier would tell an
		// attacker which addresses are registered but unverified.
		if (!user.isEmailVerified()) {
			throw new EmailNotVerifiedException(user.getEmail());
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
