package com.agriknowledge.auth.dto;

import com.agriknowledge.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
		@NotBlank @Email String email,
		@NotBlank String code,
		/** Held to the same rule as registration, from the same annotation. */
		@NotBlank @StrongPassword String newPassword) {
}
