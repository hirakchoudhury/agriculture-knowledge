package com.agriknowledge.common;

/**
 * Maps to 400: the request was well-formed but the values do not make sense —
 * a link that is not a YouTube link, an id that names the wrong kind of material.
 *
 * <p>Services throw this rather than Spring's ResponseStatusException so the domain
 * layer stays free of web types, and so a single handler owns the mapping.
 */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

}
