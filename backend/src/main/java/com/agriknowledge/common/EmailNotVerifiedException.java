package com.agriknowledge.common;

/**
 * Maps to 403 with a distinguishable code, so the client can send the person to
 * the verification screen rather than showing a generic sign-in failure.
 */
public class EmailNotVerifiedException extends RuntimeException {

	private final String email;

	public EmailNotVerifiedException(String email) {
		super("Please verify your email address before signing in");
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

}
