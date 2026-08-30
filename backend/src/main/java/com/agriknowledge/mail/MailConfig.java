package com.agriknowledge.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Chooses how mail leaves, in order of preference.
 *
 * <ol>
 *   <li>Brevo's HTTP API, if an API key is set. The only option that works on a
 *       host that blocks outbound SMTP, which Railway does.</li>
 *   <li>SMTP, if a host and username are set. Fine locally, and fine on a host
 *       that permits it.</li>
 *   <li>Neither: write the message to the log. Keeps local development working
 *       with no credentials at all.</li>
 * </ol>
 */
@Configuration
public class MailConfig {

	private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

	@Bean
	EmailTransport emailTransport(
			ObjectProvider<JavaMailSender> mailSender,
			@Value("${app.mail.brevo-api-key:}") String brevoApiKey,
			@Value("${app.mail.from:}") String fromEmail,
			@Value("${app.mail.from-name:Agriculture Knowledge}") String fromName,
			@Value("${spring.mail.host:}") String smtpHost,
			@Value("${spring.mail.username:}") String smtpUsername) {

		if (!brevoApiKey.isBlank()) {
			String sender = fromEmail.isBlank() ? smtpUsername : fromEmail;
			if (sender.isBlank()) {
				log.error("app.mail.brevo-api-key is set but there is no sender address. "
						+ "Set app.mail.from. Falling back to the log.");
				return new LoggingEmailTransport();
			}
			return new BrevoEmailTransport(brevoApiKey, sender, fromName);
		}

		if (!smtpHost.isBlank() && !smtpUsername.isBlank()) {
			JavaMailSender sender = mailSender.getIfAvailable();
			if (sender != null) {
				return new SmtpEmailTransport(sender, smtpUsername);
			}
		}

		return new LoggingEmailTransport();
	}

	/** SMTP. Works locally; blocked on Railway, which is why Brevo takes priority. */
	static final class SmtpEmailTransport implements EmailTransport {

		private static final Logger log = LoggerFactory.getLogger(SmtpEmailTransport.class);

		private final JavaMailSender sender;
		private final String from;

		SmtpEmailTransport(JavaMailSender sender, String from) {
			this.sender = sender;
			this.from = from;
		}

		@Override
		public boolean send(String to, String subject, String body) {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			try {
				sender.send(message);
				return true;
			}
			catch (Exception ex) {
				log.error("SMTP could not send to {}: {}", to, ex.getMessage());
				return false;
			}
		}

		@Override
		public String describe() {
			return "SMTP, sending as " + from;
		}

	}

	/**
	 * No transport configured. The message goes to the log so local work needs no
	 * credentials — and so a misconfigured production is loud rather than silent.
	 */
	static final class LoggingEmailTransport implements EmailTransport {

		private static final Logger log = LoggerFactory.getLogger(LoggingEmailTransport.class);

		@Override
		public boolean send(String to, String subject, String body) {
			log.warn("No mail transport configured. Would have sent to {}:\n{}", to, body);
			return false;
		}

		@Override
		public String describe() {
			return "none (messages are written to this log)";
		}

	}

}
