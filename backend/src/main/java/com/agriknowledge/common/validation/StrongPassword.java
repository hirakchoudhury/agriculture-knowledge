package com.agriknowledge.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * At least 8 characters, with an upper-case letter, a digit and a symbol.
 *
 * <p>Applied wherever a password is chosen — registration and reset — so the two
 * can never drift apart.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

	String message() default "Password is not strong enough";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
