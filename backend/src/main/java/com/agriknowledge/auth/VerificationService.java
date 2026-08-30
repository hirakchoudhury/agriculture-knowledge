package com.agriknowledge.auth;

import com.agriknowledge.common.BadRequestException;
import com.agriknowledge.mail.MailService;
import com.agriknowledge.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/** Issues and checks the six-digit codes used for verification and reset. */
@Service
public class VerificationService {

	static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
	private static final int CODE_DIGITS = 6;

	private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

	private final VerificationCodeRepository codes;
	private final MailService mail;
	private final SecureRandom random = new SecureRandom();

	public VerificationService(VerificationCodeRepository codes, MailService mail) {
		this.codes = codes;
		this.mail = mail;
	}

	@Transactional
	public void issueEmailVerification(User user) {
		String code = issue(user, VerificationPurpose.EMAIL_VERIFICATION);
		mail.sendVerificationCode(user.getEmail(), user.getName(), code,
				(int) CODE_LIFETIME.toMinutes());
	}

	@Transactional
	public void issuePasswordReset(User user) {
		String code = issue(user, VerificationPurpose.PASSWORD_RESET);
		mail.sendPasswordResetCode(user.getEmail(), user.getName(), code,
				(int) CODE_LIFETIME.toMinutes());
	}

	/**
	 * Checks a code and spends it.
	 *
	 * @throws BadCredentialsException if the code is wrong, expired, already used,
	 *     or too many guesses have been made
	 */
	@Transactional
	public void consume(User user, VerificationPurpose purpose, String submittedCode) {
		VerificationCode stored = codes.findNewest(user.getId(), purpose)
				.orElseThrow(() -> new BadCredentialsException(
						"That code is not valid. Ask for a new one."));

		Instant now = Instant.now();

		if (!stored.isUsable(now)) {
			throw new BadCredentialsException(stored.getAttempts() >= VerificationCode.MAX_ATTEMPTS
					? "Too many incorrect attempts. Ask for a new code."
					: "That code has expired. Ask for a new one.");
		}

		// Counted before comparing, so a wrong guess costs an attempt whatever
		// happens next.
		stored.recordAttempt();

		if (!stored.matches(hash(submittedCode))) {
			int left = VerificationCode.MAX_ATTEMPTS - stored.getAttempts();
			throw new BadCredentialsException(left > 0
					? "That code is not correct. %d attempt%s left.".formatted(left, left == 1 ? "" : "s")
					: "Too many incorrect attempts. Ask for a new code.");
		}

		stored.consume();
	}

	private String issue(User user, VerificationPurpose purpose) {
		// Anything outstanding stops working the moment a new code is sent,
		// so only one code per purpose is ever live.
		codes.voidOutstanding(user.getId(), purpose, Instant.now());

		String code = generateCode();
		codes.save(new VerificationCode(user, purpose, hash(code), Instant.now().plus(CODE_LIFETIME)));
		log.info("Issued a {} code for user {}", purpose, user.getId());
		return code;
	}

	/**
	 * Six digits, zero-padded, from a cryptographic source. Not
	 * {@code Math.random}: a predictable code is no barrier at all.
	 */
	private String generateCode() {
		int bound = (int) Math.pow(10, CODE_DIGITS);
		return String.format("%0" + CODE_DIGITS + "d", random.nextInt(bound));
	}

	String hash(String code) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(code.trim().getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required but unavailable", ex);
		}
	}

	/** Guards against a caller passing something that is obviously not a code. */
	public static void assertLooksLikeCode(String code) {
		if (code == null || code.trim().length() != CODE_DIGITS
				|| !code.trim().chars().allMatch(Character::isDigit)) {
			throw new BadRequestException("Enter the %d-digit code from your email".formatted(CODE_DIGITS));
		}
	}

}
