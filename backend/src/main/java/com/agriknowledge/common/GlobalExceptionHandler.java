package com.agriknowledge.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Turns exceptions into the single {@link ApiError} shape, so the frontend has one
 * thing to parse no matter what went wrong.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> onValidationFailure(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
				.collect(Collectors.joining("; "));
		return respond(HttpStatus.BAD_REQUEST, detail.isBlank() ? "Request was not valid" : detail, request);
	}

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<ApiError> onBadRequest(BadRequestException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	/**
	 * Anything Spring itself raises with a status attached — an unreadable body, an
	 * unsupported media type. Without this the catch-all below would swallow the
	 * intended status and report 500.
	 */
	@ExceptionHandler(ErrorResponseException.class)
	ResponseEntity<ApiError> onErrorResponse(ErrorResponseException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		String detail = ex.getBody().getDetail();
		return respond(status, detail != null ? detail : status.getReasonPhrase(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ApiError> onConflict(ConflictException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ApiError> onNotFound(NotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	/**
	 * Login failures and refresh failures both land here. The message stays vague on
	 * purpose: saying "no such email" tells an attacker which addresses are registered.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ApiError> onBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	ResponseEntity<ApiError> onAuthorizationDenied(AuthorizationDeniedException ex, HttpServletRequest request) {
		return respond(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> onUnexpected(Exception ex, HttpServletRequest request) {
		// Logged in full, returned as a generic message: stack traces and internal
		// class names are useful to an attacker and meaningless to a learner.
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side", request);
	}

	private ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
	}

}
