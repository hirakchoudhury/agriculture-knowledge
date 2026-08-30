package com.agriknowledge.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrongPasswordValidatorTest {

	private final StrongPasswordValidator validator = new StrongPasswordValidator();

	private ConstraintValidatorContext context;
	private ConstraintValidatorContext.ConstraintViolationBuilder builder;

	@BeforeEach
	void setUp() {
		context = mock(ConstraintValidatorContext.class);
		builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
		when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Passw0rd!",
			"Soil#2026",
			"aB3$efgh",
			"Very-Long-Passphrase-9",
			"A1!bcdef",
	})
	@DisplayName("accepts a password with a capital, a number and a symbol")
	void acceptsStrongPasswords(String password) {
		assertThat(validator.isValid(password, context)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Ab1!",              // too short
			"password1!",        // no capital
			"Password!",         // no number
			"Password1",         // no symbol
			"PASSWORD1!",        // no lower case is fine, but this one is fine too
	})
	void rejectsWhatIsMissingARule(String password) {
		boolean valid = validator.isValid(password, context);

		// The last case is genuinely valid: lower case is not one of the rules.
		if (password.equals("PASSWORD1!")) {
			assertThat(valid).isTrue();
			return;
		}
		assertThat(valid).isFalse();
	}

	@Test
	@DisplayName("the message names every rule that failed, not just the first")
	void explainsWhatIsMissing() {
		validator.isValid("abcdefgh", context);

		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(context).buildConstraintViolationWithTemplate(message.capture());

		assertThat(message.getValue())
				.contains("a capital letter")
				.contains("a number")
				.contains("a symbol");
	}

	@Test
	@DisplayName("a short password is told it is short, not just wrong")
	void reportsLength() {
		validator.isValid("Ab1!", context);

		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(context).buildConstraintViolationWithTemplate(message.capture());

		assertThat(message.getValue()).contains("at least 8 characters");
	}

	@Test
	@DisplayName("emptiness is NotBlank's job, so this stays quiet about it")
	void ignoresNullAndEmpty() {
		assertThat(validator.isValid(null, context)).isTrue();
		assertThat(validator.isValid("", context)).isTrue();
	}

	@Test
	@DisplayName("any non-alphanumeric counts as a symbol, not a fixed short list")
	void acceptsUnusualSymbols() {
		assertThat(validator.isValid("Passw0rd£", context)).isTrue();
		assertThat(validator.isValid("Passw0rd€", context)).isTrue();
		assertThat(validator.isValid("Passw0rd~", context)).isTrue();
	}

	@Test
	@DisplayName("a space alone is not a symbol, or 'Password 1' would pass")
	void spaceIsNotASymbol() {
		assertThat(validator.isValid("Password 1", context)).isFalse();
	}

}
