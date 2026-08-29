package com.agriknowledge.quiz.dto;

/** Admin view of an option, where the answer key is exactly what is being edited. */
public record AdminOption(Long id, String text, boolean correct, int displayOrder) {
}
