package com.agriknowledge.common;

/** Maps to 409 — the request was valid but clashes with existing state. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
