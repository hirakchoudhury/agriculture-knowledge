package com.agriknowledge.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The complete set of answers. Questions omitted from the list are treated as
 * unanswered, so a partial submission is valid rather than an error.
 */
public record SubmitAttemptRequest(@NotNull @Valid List<SubmittedAnswer> answers) {
}
