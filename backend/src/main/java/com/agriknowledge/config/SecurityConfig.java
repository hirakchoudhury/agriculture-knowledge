package com.agriknowledge.config;

import com.agriknowledge.auth.jwt.JwtAuthenticationFilter;
import com.agriknowledge.auth.jwt.JwtService;
import com.agriknowledge.auth.oauth2.OAuth2SuccessHandler;
import com.agriknowledge.common.RestAccessDeniedHandler;
import com.agriknowledge.common.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * Cost 12 rather than the default 10. Roughly four times slower to verify, which
	 * is imperceptible on a login but meaningful against an offline cracking attempt.
	 */
	private static final int BCRYPT_STRENGTH = 12;

	private final AppProperties appProperties;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;

	public SecurityConfig(AppProperties appProperties,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) {
		this.appProperties = appProperties;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
	}

	/**
	 * Built here rather than annotated as a component: a bean of type Filter is
	 * auto-registered with the servlet container as well, which would run it twice
	 * and on paths the security chain never sees.
	 */
	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
		return new JwtAuthenticationFilter(jwtService);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations,
			ObjectProvider<OAuth2SuccessHandler> oauth2SuccessHandler) throws Exception {

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				// The API is stateless and token-authenticated, so there is no session
				// cookie for CSRF to protect. The refresh cookie is scoped to
				// /api/v1/auth and only ever read by the refresh and logout endpoints.
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/v1/health").permitAll()
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers("/api/v1/auth/**").permitAll()
						// Browsing the syllabus is the shop window: readable signed out.
						// Note these are GET-only, and /api/v1/admin/** is not included.
						.requestMatchers(HttpMethod.GET, "/api/v1/exams", "/api/v1/exams/**",
								"/api/v1/topics", "/api/v1/topics/**",
								"/api/v1/materials", "/api/v1/materials/**",
								"/api/v1/quizzes/**").permitAll()
						// Google redirects land on these two, before any session exists.
						.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
						.anyRequest().authenticated())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		// Google sign-in switches itself on only when credentials are configured.
		// Calling oauth2Login() without a client registration fails at startup, so an
		// unconfigured environment would otherwise refuse to boot at all.
		if (clientRegistrations.getIfAvailable() != null) {
			OAuth2SuccessHandler successHandler = oauth2SuccessHandler.getObject();
			http.oauth2Login(oauth -> oauth
					.successHandler(successHandler)
					.failureHandler((request, response, exception) ->
							response.sendRedirect(appProperties.frontendUrl() + "/login?error=google_sign_in_failed")));
		}

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(appProperties.allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		// Required for the refresh-token cookie. Note that a wildcard origin is
		// illegal once credentials are allowed, which is why origins are explicit.
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
