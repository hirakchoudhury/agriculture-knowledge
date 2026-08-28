package com.agriknowledge.auth.jwt;

import com.agriknowledge.user.Role;
import com.agriknowledge.user.User;

/**
 * What the application knows about the caller, taken straight from a verified
 * access token. Holding the id and role here means the common case — who is this,
 * and may they do it — costs no database round trip.
 */
public record AuthPrincipal(Long userId, String email, Role role) {

	public static AuthPrincipal of(User user) {
		return new AuthPrincipal(user.getId(), user.getEmail(), user.getRole());
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

}
