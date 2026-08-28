package com.agriknowledge.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Our own pagination envelope.
 *
 * <p>Spring's Page serialises with a shape that is not stable across versions and
 * carries fields the client has no use for. Wrapping it means the frontend depends
 * on this contract instead.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

	public static <T> PageResponse<T> of(Page<T> page) {
		return of(page, Function.identity());
	}

}
