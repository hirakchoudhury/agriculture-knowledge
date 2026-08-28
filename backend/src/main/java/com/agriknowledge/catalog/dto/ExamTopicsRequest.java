package com.agriknowledge.catalog.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The complete set of topics for an exam, not a delta. Sending the whole set makes
 * the operation idempotent and avoids add/remove endpoints that can drift apart.
 */
public record ExamTopicsRequest(@NotNull List<Long> topicIds) {
}
