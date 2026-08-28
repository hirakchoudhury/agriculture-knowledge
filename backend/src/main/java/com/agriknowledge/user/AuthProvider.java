package com.agriknowledge.user;

/** How an account proves who it is. */
public enum AuthProvider {
	/** Email and password, hashed with BCrypt. */
	LOCAL,
	/** Google sign-in; the account has no password of its own. */
	GOOGLE
}
