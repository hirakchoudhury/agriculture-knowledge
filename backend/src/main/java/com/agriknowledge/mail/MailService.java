package com.agriknowledge.mail;

import com.agriknowledge.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * The two transactional emails this application sends.
 *
 * <p>How they leave is {@link MailConfig}'s decision. This only decides what they
 * say.
 */
@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final EmailTransport transport;
	private final AppProperties appProperties;
	private final Environment environment;

	public MailService(EmailTransport transport, AppProperties appProperties, Environment environment) {
		this.transport = transport;
		this.appProperties = appProperties;
		this.environment = environment;
	}

	@PostConstruct
	void reportTransport() {
		String description = transport.describe();

		if (!(transport instanceof MailConfig.LoggingEmailTransport)) {
			log.info("Mail transport: {}", description);
			return;
		}

		String warning = "No mail transport configured. Verification and reset codes "
				+ "will be written to this log instead of sent.";

		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile)) {
				// Loud, because it means anyone who can read logs can take over any
				// account, and because sign-up is silently broken for real users.
				log.error("{} This is unsafe in production: set app.mail.brevo-api-key.", warning);
				return;
			}
		}
		log.warn("{} Fine for local work.", warning);
	}

	public void sendVerificationCode(String toEmail, String name, String code, int minutesValid) {
		send(toEmail,
				"Your Agriculture Knowledge verification code",
				"""
				Hello %s,

				Your verification code is %s

				It expires in %d minutes. Enter it on the sign-up page to finish
				creating your account.

				If you did not sign up, you can ignore this email — no account can be
				used until this code is entered.

				%s
				""".formatted(name, code, minutesValid, appProperties.frontendUrl()));
	}

	public void sendPasswordResetCode(String toEmail, String name, String code, int minutesValid) {
		send(toEmail,
				"Reset your Agriculture Knowledge password",
				"""
				Hello %s,

				Your password reset code is %s

				It expires in %d minutes.

				If you did not ask to reset your password, ignore this email. Your
				password has not changed and nobody can change it without this code.

				%s
				""".formatted(name, code, minutesValid, appProperties.frontendUrl()));
	}

	private void send(String toEmail, String subject, String body) {
		// A failed send never fails the request that triggered it. The code is
		// already stored, so the person can simply ask for another one.
		if (transport.send(toEmail, subject, body)) {
			log.info("Sent '{}' to {}", subject, toEmail);
		}
	}

}
