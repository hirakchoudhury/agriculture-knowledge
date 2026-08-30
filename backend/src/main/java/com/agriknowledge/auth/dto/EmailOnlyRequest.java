package com.agriknowledge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Used by resend-code and forgot-password, which take nothing but an address. */
public record EmailOnlyRequest(@NotBlank @Email String email) {
}
