package com.agriknowledge.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OptionRequest(
		@NotBlank @Size(max = 1000) String text,
		boolean correct) {
}
