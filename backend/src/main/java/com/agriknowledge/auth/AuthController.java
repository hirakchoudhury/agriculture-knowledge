package com.agriknowledge.auth;

import com.agriknowledge.auth.dto.AuthResponse;
import com.agriknowledge.auth.dto.LoginRequest;
import com.agriknowledge.auth.dto.RegisterRequest;
import com.agriknowledge.auth.dto.UserResponse;
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

	@PostMapping("/register")
	ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest) {
		String caller = ClientAddress.of(httpRequest);
		registrationLimiter.check(caller);

		var session = authService.register(request, httpRequest.getHeader(HttpHeaders.USER_AGENT));
		return respondWithSession(session, HttpStatus.CREATED);
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
