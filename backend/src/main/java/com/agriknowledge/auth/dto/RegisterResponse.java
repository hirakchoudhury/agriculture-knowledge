package com.agriknowledge.auth.dto;

/**
 * Registration no longer hands back tokens.
 *
 * <p>An account is not usable until the address is verified, so signing someone
 * in at this point would create a session for an account that cannot do anything.
 * The client sends them to the code screen instead.
 */
public record RegisterResponse(String email, int codeValidMinutes, String message) {
}
