package com.agriknowledge.auth.oauth2;

import com.agriknowledge.auth.AuthService;
import com.agriknowledge.auth.RefreshCookieFactory;
import com.agriknowledge.config.AppProperties;
import com.agriknowledge.user.Role;
import com.agriknowledge.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Runs after Google has verified the user. Converts that into one of our own
 * sessions and hands control back to the frontend.
 *
 * <p>The access token is returned in the URL <em>fragment</em>, never the query
 * string: fragments are not sent to servers, so the token stays out of access logs,
 * proxy logs and {@code Referer} headers. The refresh token rides in its cookie as
 * usual and never appears in a URL at all.
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

	private final AuthService authService;
	private final RefreshCookieFactory cookies;
	private final AppProperties appProperties;

	public OAuth2SuccessHandler(AuthService authService, RefreshCookieFactory cookies,
			AppProperties appProperties) {
		this.authService = authService;
		this.cookies = cookies;
		this.appProperties = appProperties;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {

		OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
		String email = oauthUser.getAttribute("email");
		String subject = oauthUser.getAttribute("sub");

		if (email == null || subject == null) {
			log.warn("Google sign-in returned no email or subject; attributes were {}",
					oauthUser.getAttributes().keySet());
			response.sendRedirect(failureUrl("google_profile_incomplete"));
			return;
		}

		// Google marks unverified addresses; accepting one would let someone claim
		// an account belonging to an email they do not control.
		Boolean emailVerified = oauthUser.getAttribute("email_verified");
		if (Boolean.FALSE.equals(emailVerified)) {
			log.warn("Rejected Google sign-in for unverified address");
			response.sendRedirect(failureUrl("google_email_unverified"));
			return;
		}

		String name = oauthUser.getAttribute("name");
		User user = authService.findOrCreateGoogleUser(
				email,
				name != null ? name : email,
				oauthUser.getAttribute("picture"),
				subject);

		// The sharper half of the rule. findOrCreateGoogleUser matches on email, so
		// without this a Google sign-in using an admin's address would be handed
		// that admin's session -- federating the most privileged account in the
		// system to an identity provider we do not control.
		if (user.getRole() == Role.ADMIN) {
			log.warn("Refused Google sign-in for admin account {}; admins must use a password",
					user.getId());
			response.sendRedirect(failureUrl("admin_must_use_password"));
			return;
		}

		AuthService.IssuedSession session = authService.issueSession(
				user, request.getHeader(HttpHeaders.USER_AGENT));

		response.addHeader(HttpHeaders.SET_COOKIE, cookies.issue(session.refreshToken()).toString());
		response.sendRedirect(successUrl(session.accessToken()));
	}

	private String successUrl(String accessToken) {
		return UriComponentsBuilder.fromUriString(appProperties.frontendUrl())
				.path("/auth/callback")
				.build()
				.toUriString()
				+ "#token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
	}

	private String failureUrl(String reason) {
		return UriComponentsBuilder.fromUriString(appProperties.frontendUrl())
				.path("/login")
				.queryParam("error", reason)
				.build()
				.toUriString();
	}

}
