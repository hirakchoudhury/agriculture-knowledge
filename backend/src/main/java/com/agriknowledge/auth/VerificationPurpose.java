package com.agriknowledge.auth;

/**
 * What a code is for. Kept distinct so a code issued to confirm an address can
 * never be spent changing a password, or the other way round.
 */
public enum VerificationPurpose {
	EMAIL_VERIFICATION,
	PASSWORD_RESET
}
