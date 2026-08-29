package com.agriknowledge.engagement;

import com.agriknowledge.common.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sliding window on comment creation, per account.
 *
 * <p>This is in-memory and therefore per-instance: two API containers would allow
 * twice the rate, and a restart forgets everything. That is an acceptable trade for
 * a single-instance deployment, and it is honest about what it is — the moment this
 * runs on more than one instance the counter belongs in Redis instead.
 */
@Component
public class CommentRateLimiter {

	private static final int MAX_PER_WINDOW = 5;
	private static final Duration WINDOW = Duration.ofMinutes(1);

	private final Map<Long, Deque<Instant>> history = new ConcurrentHashMap<>();

	public void checkAllowed(Long userId) {
		Instant now = Instant.now();
		Instant cutoff = now.minus(WINDOW);

		Deque<Instant> timestamps = history.computeIfAbsent(userId, key -> new ArrayDeque<>());

		// Synchronised on the deque itself: ArrayDeque is not thread-safe, and one
		// account can have several requests in flight.
		synchronized (timestamps) {
			while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
				timestamps.pollFirst();
			}

			if (timestamps.size() >= MAX_PER_WINDOW) {
				throw new RateLimitExceededException(
						"That is a lot of comments in a short time. Try again in a minute.");
			}

			timestamps.addLast(now);
		}
	}

	/** Lets a failed create release the slot it reserved, so a rejection is not charged. */
	public void refund(Long userId) {
		Deque<Instant> timestamps = history.get(userId);
		if (timestamps == null) {
			return;
		}
		synchronized (timestamps) {
			timestamps.pollLast();
		}
	}

}
