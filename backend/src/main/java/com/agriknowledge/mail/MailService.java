package com.agriknowledge.mail;

import com.agriknowledge.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the two transactional emails this application has.
 *
 * <p>When SMTP is not configured the code is written to the log instead of sent.
 * That keeps local development working without credentials, but it would be a
 * serious hole in production — anyone with log access could take over an account —
 * so the application shouts about it on startup when the prod profile is active.
 */
@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final ObjectProvider<JavaMailSender> mailSender;
	private final AppProperties appProperties;
	private final Environment environment;
	private final String from;
	private final boolean configured;

	public MailService(ObjectProvider<JavaMailSender> mailSender, AppProperties appProperties,
			Environment environment,
			@Value("${spring.mail.host:}") String host,
			@Value("${spring.mail.username:}") String username) {
		this.mailSender = mailSender;
		this.appProperties = appProperties;
		this.environment = environment;
		this.from = username;
		this.configured = !host.isBlank() && !username.isBlank();
	}

	@PostConstruct
	void warnIfUnconfigured() {
		if (configured) {
			log.info("SMTP configured; verification codes will be emailed from {}", from);
			return;
		}

		String message = "SMTP is NOT configured. Verification and reset codes will be "
				+ "written to this log instead of emailed.";

		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile)) {
				// Loud, because in production this means anyone who can read logs
				// can take over any account.
				log.error("{} This is unsafe in production: set spring.mail.* now.", message);
				return;
			}
		}
		log.warn("{} Fine for local work.", message);
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
		JavaMailSender sender = configured ? mailSender.getIfAvailable() : null;

		if (sender == null) {
			// The code is deliberately on its own line so it is easy to find while
			// developing, and equally easy to spot in a log review as a problem.
			log.warn("SMTP not configured. Would have emailed {} the following:\n{}", toEmail, body);
			return;
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(toEmail);
		message.setSubject(subject);
		message.setText(body);

		try {
			sender.send(message);
			log.info("Sent '{}' to {}", subject, toEmail);
		}
		catch (Exception ex) {
			// A failed send must not fail the request that triggered it. The caller
			// has already created the code; the person can ask for another one.
			log.error("Could not send '{}' to {}: {}", subject, toEmail, ex.getMessage());
		}
	}

}
