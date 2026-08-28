package com.agriknowledge.user;

/**
 * Coarse authorisation level. Deliberately only two values: everything an admin
 * can do is content management, and everything else is open to any signed-in user.
 */
public enum Role {
	USER,
	ADMIN;

	/** Spring Security expects the ROLE_ prefix for hasRole() checks. */
	public String authority() {
		return "ROLE_" + name();
	}
}
