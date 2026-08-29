package com.agriknowledge.quiz.dto;

/**
 * An option as a learner sees it while taking a quiz.
 *
 * <p>There is deliberately no correct flag on this record. Stripping a field before
 * serialisation relies on remembering to do it; omitting the field entirely means
 * the answer key cannot leak through this path even by accident.
 */
public record AttemptOption(Long id, String text) {
}
