package com.agriknowledge.auth.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Access tokens are HMAC-signed with a symmetric key. Asymmetric signing would only
 * pay off if a second service had to verify tokens without being able to mint them,
 * which is not the case here.
 */
@Configuration
public class JwtConfig {

	private static final int MINIMUM_KEY_BYTES = 32;

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		if (properties.secret() == null || properties.secret().isBlank()) {
			throw new IllegalStateException(
					"app.jwt.secret is not set. Generate one with: openssl rand -base64 32");
		}

		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(properties.secret());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException("app.jwt.secret must be valid base64", ex);
		}

		if (keyBytes.length < MINIMUM_KEY_BYTES) {
			throw new IllegalStateException("app.jwt.secret must decode to at least %d bytes, got %d"
					.formatted(MINIMUM_KEY_BYTES, keyBytes.length));
		}

		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// Validates expiry and not-before as well as the issuer, so a token minted
		// by some other service sharing the key would still be rejected.
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

}
