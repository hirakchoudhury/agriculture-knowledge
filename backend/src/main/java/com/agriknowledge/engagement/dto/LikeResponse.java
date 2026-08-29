package com.agriknowledge.engagement.dto;

/**
 * Returned by both the like and unlike endpoints so an optimistic UI can settle on
 * the server's number rather than keeping its own guess.
 */
public record LikeResponse(boolean liked, long likeCount) {
}
