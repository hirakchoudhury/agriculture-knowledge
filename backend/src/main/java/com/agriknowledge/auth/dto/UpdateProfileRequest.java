package com.agriknowledge.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@Size(min = 2, max = 120) String name,
		@Size(max = 500) String avatarUrl) {
}
