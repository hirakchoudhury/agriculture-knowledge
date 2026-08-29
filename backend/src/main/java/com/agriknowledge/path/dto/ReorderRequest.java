package com.agriknowledge.path.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The complete list of item ids in their new order.
 *
 * <p>Sending the whole order rather than a move instruction keeps the operation
 * idempotent and avoids the client and server disagreeing about intermediate state
 * after a dropped request.
 */
public record ReorderRequest(@NotNull List<Long> itemIds) {
}
