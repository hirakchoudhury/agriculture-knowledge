package com.agriknowledge.auth;

import com.agriknowledge.auth.dto.AuthResponse;
import com.agriknowledge.auth.dto.LoginRequest;
import com.agriknowledge.auth.dto.EmailOnlyRequest;
import com.agriknowledge.auth.dto.RegisterRequest;
import com.agriknowledge.auth.dto.RegisterResponse;
import com.agriknowledge.auth.dto.ResetPasswordRequest;
import com.agriknowledge.auth.dto.VerifyEmailRequest;
import com.agriknowledge.auth.dto.UserResponse;
import com.agriknowledge.auth.VerificationService;
import com.agriknowledge.auth.jwt.JwtProperties;
import com.agriknowledge.common.ClientAddress;
import com.agriknowledge.common.SlidingWindowRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshCookieFactory cookies;
	private final JwtProperties jwtProperties;
	private final SlidingWindowRateLimiter loginLimiter;
	private final SlidingWindowRateLimiter registrationLimiter;

	public AuthController(AuthService authService, RefreshCookieFactory cookies,
			JwtProperties jwtProperties,
			@Qualifier("loginRateLimiter") SlidingWindowRateLimiter loginLimiter,
			@Qualifier("registrationRateLimiter") SlidingWindowRateLimiter registrationLimiter) {
		this.authService = authService;
		this.cookies = cookies;
		this.jwtProperties = jwtProperties;
		this.loginLimiter = loginLimiter;
		this.registrationLimiter = registrationLimiter;
	}

	/** Creates the account and emails a code. No session until the code is entered. */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	RegisterResponse register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest) {
		registrationLimiter.check(ClientAddress.of(httpRequest));

		var user = authService.register(request);
		return new RegisterResponse(
				user.getEmail(),
				(int) VerificationService.CODE_LIFETIME.toMinutes(),
				"Check your email for a 6-digit code to finish signing up.");
	}

	@PostMapping("/verify-email")
	ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
			HttpServletRequest httpRequest) {
		// Rate limited like a sign-in: this endpoint accepts a guessable secret.
		loginLimiter.check(ClientAddress.of(httpRequest));

		var session = authService.verifyEmail(request, httpRequest.getHeader(HttpHeaders.USER_AGENT));
		return respondWithSession(session, HttpStatus.OK);
	}

	/**
	 * Always 204, whether or not the address exists. Anything else turns this into
	 * a way to find out who has an account.
	 */
	@PostMapping("/resend-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resendVerification(@Valid @RequestBody EmailOnlyRequest request,
			HttpServletRequest httpRequest) {
		registrationLimiter.check(ClientAddress.of(httpRequest));
		authService.resendVerification(request.email());
	}

	/** Always 204, for the same reason. */
	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void forgotPassword(@Valid @RequestBody EmailOnlyRequest request,
			HttpServletRequest httpRequest) {
		registrationLimiter.check(ClientAddress.of(httpRequest));
		authService.forgotPassword(request.email());
	}

	/**
	 * Returns no session on purpose. Making them sign in with the new password
	 * confirms it is the one they think they set.
	 */
	@PostMapping("/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resetPassword(@Valid @RequestBody ResetPasswordRequest request,
			HttpServletRequest httpRequest) {
		loginLimiter.check(ClientAddress.of(httpRequest));
		authService.resetPassword(request);
	}

	@PostMapping("/login")
	ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		String caller = ClientAddress.of(httpRequest);
		loginLimiter.check(caller);

		var session = authService.login(request, httpRequest.getHeader(HttpHeaders.USER_AGENT));

		// A correct password clears the allowance, so someone who mistypes a few
		// times and then succeeds is not left throttled.
		loginLimiter.reset(caller);
		return respondWithSession(session, HttpStatus.OK);
	}

	@PostMapping("/refresh")
	ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = "${app.jwt.refresh-cookie-name}", required = false) String refreshToken,
			HttpServletRequest httpRequest) {
		var session = authService.refresh(refreshToken, httpRequest.getHeader(HttpHeaders.USER_AGENT));
		return respondWithSession(session, HttpStatus.OK);
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(
			@CookieValue(name = "${app.jwt.refresh-cookie-name}", required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
				.build();
	}

	private ResponseEntity<AuthResponse> respondWithSession(AuthService.IssuedSession session, HttpStatus status) {
		ResponseCookie cookie = cookies.issue(session.refreshToken());
		AuthResponse body = new AuthResponse(
				session.accessToken(),
				jwtProperties.accessTokenTtl().toSeconds(),
				UserResponse.from(session.user()));

		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(body);
	}

}
