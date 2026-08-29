package com.agriknowledge.auth;

import com.agriknowledge.common.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Limits on the endpoints worth attacking.
 *
 * <p>Login is the one that matters: BCrypt at cost 12 makes each guess expensive
 * for us as well as the attacker, so without a limit a credential-stuffing run
 * both risks accounts and exhausts the connection pool.
 *
 * <p>Both are keyed by IP, and the ceilings are deliberately not tight. The
 * audience for this site is students who are often behind one shared connection --
 * a college lab, a coaching centre, a mobile carrier NAT -- where a handful of
 * sign-ups an hour is normal traffic rather than an attack. Configurable so a
 * deployment that knows its own traffic can tighten them.
 */
@Configuration
public class AuthRateLimiters {

	@Bean
	SlidingWindowRateLimiter loginRateLimiter(
			@Value("${app.rate-limit.login-attempts:15}") int attempts,
			@Value("${app.rate-limit.login-window:PT5M}") Duration window) {
		return new SlidingWindowRateLimiter(attempts, window,
				"Too many sign-in attempts. Wait a few minutes and try again.");
	}

	@Bean
	SlidingWindowRateLimiter registrationRateLimiter(
			@Value("${app.rate-limit.registrations:25}") int attempts,
			@Value("${app.rate-limit.registration-window:PT1H}") Duration window) {
		return new SlidingWindowRateLimiter(attempts, window,
				"Too many accounts created from here. Try again later.");
	}

}
