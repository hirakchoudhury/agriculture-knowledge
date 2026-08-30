package com.agriknowledge.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Turns exceptions into the single {@link ApiError} shape, so the frontend has one
 * thing to parse no matter what went wrong.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} rather than writing handlers
 * one at a time is deliberate, and was learned the hard way. Spring raises a family
 * of exceptions that already carry the right status — an unsupported method, an
 * unreadable body, a missing parameter — and they do not share a common base class
 * that is easy to catch. Handling them individually means every one that is
 * forgotten falls through to the catch-all and is reported as a 500, which is
 * exactly what happened twice while building this. The base class knows them all;
 * {@link #handleExceptionInternal} only has to reshape the body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ----- domain exceptions -------------------------------------------------

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<ApiError> onBadRequest(BadRequestException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	/**
	 * Distinct from a wrong password so the client can send the person to the
	 * verification screen instead of telling them their credentials were wrong.
	 */
	@ExceptionHandler(EmailNotVerifiedException.class)
	ResponseEntity<ApiError> onEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
		return respond(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ApiError> onConflict(ConflictException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ApiError> onNotFound(NotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	ResponseEntity<ApiError> onRateLimited(RateLimitExceededException ex, HttpServletRequest request) {
		return respond(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
	}

	/**
	 * Login and refresh failures. The message stays vague on purpose: saying "no
	 * such email" tells an attacker which addresses are registered.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ApiError> onBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	/**
	 * Covers both the AuthorizationDeniedException that @PreAuthorize raises and the
	 * plain AccessDeniedException a service throws for an ownership check.
	 *
	 * <p>Spring Security's filter would normally translate these into a 403 itself,
	 * but only for exceptions that escape the filter chain. Once this advice is in
	 * play it sees them first.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> onAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		String message = ex.getMessage() == null || ex instanceof AuthorizationDeniedException
				? "You do not have permission to perform this action"
				: ex.getMessage();
		return respond(HttpStatus.FORBIDDEN, message, request);
	}

	/** Genuinely unexpected. Logged in full, returned as a generic message. */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> onUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side", request);
	}

	// ----- Spring MVC exceptions ---------------------------------------------

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
				.collect(Collectors.joining("; "));
		return handleExceptionInternal(ex, detail.isBlank() ? "Request was not valid" : detail,
				headers, status, request);
	}

	/**
	 * Every exception the base class knows about funnels through here, so all of
	 * them come out in the {@link ApiError} shape rather than Spring's ProblemDetail.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

		HttpStatus status = HttpStatus.valueOf(statusCode.value());
		String message = body instanceof String text ? text : messageFor(ex, status);

		if (status.is5xxServerError()) {
			log.error("Server error on {}", pathOf(request), ex);
		}

		return new ResponseEntity<>(ApiError.of(status, message, pathOf(request)), headers, statusCode);
	}

	private String messageFor(Exception ex, HttpStatus status) {
		// Spring's own messages are already user-facing for the 4xx family; for 5xx
		// they can leak internals, so those get a generic line instead.
		return status.is4xxClientError() && ex.getMessage() != null
				? ex.getMessage()
				: status.getReasonPhrase();
	}

	private String pathOf(WebRequest request) {
		return request instanceof ServletWebRequest servletRequest
				? servletRequest.getRequest().getRequestURI()
				: request.getDescription(false);
	}

	private ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
	}

}
