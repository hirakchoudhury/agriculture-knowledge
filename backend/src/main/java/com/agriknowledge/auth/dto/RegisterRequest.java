package com.agriknowledge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank @Email @Size(max = 255)
		String email,

		// Length is the only rule enforced. Composition rules push people towards
		// predictable substitutions; length is what actually resists guessing.
		@NotBlank @Size(min = 10, max = 100, message = "Password must be at least 10 characters")
		String password,

		@NotBlank @Size(min = 2, max = 120)
		String name) {
}
