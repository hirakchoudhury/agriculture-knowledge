package com.agriknowledge.auth.jwt;

import com.agriknowledge.user.Role;
import com.agriknowledge.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class JwtService {

	private static final String ROLE_CLAIM = "role";
	private static final String EMAIL_CLAIM = "email";
	private static final int REFRESH_TOKEN_BYTES = 32;

	private final JwtEncoder encoder;
	private final JwtDecoder decoder;
	private final JwtProperties properties;
	private final SecureRandom random = new SecureRandom();

	public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
		this.encoder = encoder;
		this.decoder = decoder;
		this.properties = properties;
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.issuedAt(now)
				.expiresAt(now.plus(properties.accessTokenTtl()))
				.subject(String.valueOf(user.getId()))
				.claim(EMAIL_CLAIM, user.getEmail())
				.claim(ROLE_CLAIM, user.getRole().name())
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/**
	 * @throws org.springframework.security.oauth2.jwt.JwtException if the token is
	 *     malformed, expired, or not signed by us
	 */
	public AuthPrincipal readAccessToken(String token) {
		Jwt jwt = decoder.decode(token);
		return new AuthPrincipal(
				Long.valueOf(jwt.getSubject()),
				jwt.getClaimAsString(EMAIL_CLAIM),
				Role.valueOf(jwt.getClaimAsString(ROLE_CLAIM)));
	}

	/**
	 * Refresh tokens are opaque random strings rather than JWTs. A JWT cannot be
	 * revoked before it expires; a random string checked against a database row can.
	 */
	public String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Stored in place of the token itself, so a leaked database grants no sessions. */
	public String hashRefreshToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required but unavailable", ex);
		}
	}

	public JwtProperties properties() {
		return properties;
	}

}
