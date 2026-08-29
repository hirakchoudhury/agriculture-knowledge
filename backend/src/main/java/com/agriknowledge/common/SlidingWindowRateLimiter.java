package com.agriknowledge.common;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sliding window over recent attempts, keyed by whatever the caller chooses.
 *
 * <p>In-memory, and therefore per-instance: two API containers would allow twice
 * the rate, and a restart forgets everything. That is an acceptable trade for a
 * single-instance deployment and it is honest about what it is — the moment this
 * runs on more than one instance the counters belong in Redis.
 *
 * <p>Keys are never evicted while the process lives. With one entry per IP or
 * account that is bounded in practice; a public API with millions of distinct
 * callers would need eviction.
 */
public class SlidingWindowRateLimiter {

	private final int maxAttempts;
	private final Duration window;
	private final String message;

	private final Map<String, Deque<Instant>> history = new ConcurrentHashMap<>();

	public SlidingWindowRateLimiter(int maxAttempts, Duration window, String message) {
		this.maxAttempts = maxAttempts;
		this.window = window;
		this.message = message;
	}

	/** @throws RateLimitExceededException if this key has used up its allowance */
	public void check(String key) {
		Instant now = Instant.now();
		Instant cutoff = now.minus(window);

		Deque<Instant> attempts = history.computeIfAbsent(key, ignored -> new ArrayDeque<>());

		// ArrayDeque is not thread-safe and one key can have several requests in
		// flight, so all reads and writes for a key happen under its own lock.
		synchronized (attempts) {
			while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
				attempts.pollFirst();
			}

			if (attempts.size() >= maxAttempts) {
				throw new RateLimitExceededException(message);
			}

			attempts.addLast(now);
		}
	}

	/** Releases the slot a request reserved, so a rejected request is not charged. */
	public void refund(String key) {
		Deque<Instant> attempts = history.get(key);
		if (attempts == null) {
			return;
		}
		synchronized (attempts) {
			attempts.pollLast();
		}
	}

	/** Clears the allowance for a key, used after a successful sign-in. */
	public void reset(String key) {
		history.remove(key);
	}

}
