package com.agriknowledge.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Returns 401 as JSON when a request carries no usable credentials.
 *
 * <p>Without this, Spring Security defaults to a 403 for every unauthenticated request
 * once the Basic and form-login entry points are disabled. That collapses two different
 * situations the frontend must tell apart: a 401 means "log in, or refresh your token",
 * a 403 means "you are logged in and still may not do this".
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(),
				ApiError.of(HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI()));
	}

}
