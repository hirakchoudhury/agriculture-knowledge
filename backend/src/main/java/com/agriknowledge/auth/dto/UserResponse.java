package com.agriknowledge.auth.dto;

import com.agriknowledge.user.AuthProvider;
import com.agriknowledge.user.Role;
import com.agriknowledge.user.User;

import java.time.Instant;

/** The safe public view of an account. Never carries the password hash. */
public record UserResponse(
		Long id,
		String email,
		String name,
		String avatarUrl,
		Role role,
		AuthProvider provider,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getAvatarUrl(),
				user.getRole(),
				user.getProvider(),
				user.getCreatedAt());
	}
}
