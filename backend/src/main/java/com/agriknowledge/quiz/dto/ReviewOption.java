package com.agriknowledge.quiz.dto;

/** An option after submission, where showing which one was right is the point. */
public record ReviewOption(Long id, String text, boolean correct) {
}
