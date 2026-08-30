package com.agriknowledge.auth.dto;

import com.agriknowledge.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank @Email @Size(max = 255)
		String email,

		@NotBlank @StrongPassword
		String password,

		@NotBlank @Size(min = 2, max = 120)
		String name) {
}
