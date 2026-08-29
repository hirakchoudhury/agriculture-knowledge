package com.agriknowledge.common;

/** Maps to 429. */
public class RateLimitExceededException extends RuntimeException {

	public RateLimitExceededException(String message) {
		super(message);
	}

}
