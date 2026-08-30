package com.agriknowledge.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks each rule separately so the message can say what is actually missing.
 *
 * <p>One regex with lookaheads would be shorter, but it can only ever answer
 * "no". Telling someone their password needs a number is the difference between
 * fixing it and giving up on the sign-up form.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

	static final int MIN_LENGTH = 8;
	static final int MAX_LENGTH = 100;

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		// Absence is @NotBlank's job, not ours. Reporting both would show two
		// errors for one empty field.
		if (password == null || password.isEmpty()) {
			return true;
		}

		List<String> missing = new ArrayList<>();

		if (password.length() < MIN_LENGTH) {
			missing.add("at least %d characters".formatted(MIN_LENGTH));
		}
		if (password.length() > MAX_LENGTH) {
			missing.add("no more than %d characters".formatted(MAX_LENGTH));
		}
		if (password.chars().noneMatch(Character::isUpperCase)) {
			missing.add("a capital letter");
		}
		if (password.chars().noneMatch(Character::isDigit)) {
			missing.add("a number");
		}
		// Anything that is neither a letter nor a digit counts, rather than a fixed
		// list of symbols: a fixed list quietly rejects passwords people did choose.
		if (password.chars().noneMatch(StrongPasswordValidator::isSymbol)) {
			missing.add("a symbol such as ! ? # or @");
		}

		if (missing.isEmpty()) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("Password needs " + String.join(", ", missing))
				.addConstraintViolation();
		return false;
	}

	private static boolean isSymbol(int codePoint) {
		return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
	}

}
