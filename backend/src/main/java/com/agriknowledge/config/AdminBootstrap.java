package com.agriknowledge.config;

import com.agriknowledge.user.AuthProvider;
import com.agriknowledge.user.Role;
import com.agriknowledge.user.User;
import com.agriknowledge.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes the accounts listed in {@code app.bootstrap-admin-emails} to ADMIN.
 *
 * <p>There is deliberately no public route to becoming an admin. A Flyway seed
 * cannot do this either, because it would have to hard-code an email address into
 * version control. Configuration keeps the list per-environment and out of git.
 *
 * <p>The account must already exist: sign up normally first, then restart.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository users;
	private final AppProperties properties;

	public AdminBootstrap(UserRepository users, AppProperties properties) {
		this.users = users;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (properties.bootstrapAdminEmails() == null) {
			return;
		}

		for (String rawEmail : properties.bootstrapAdminEmails()) {
			String email = User.normaliseEmail(rawEmail);
			if (email == null || email.isBlank()) {
				continue;
			}

			users.findByEmail(email).ifPresentOrElse(user -> {
				// Admins authenticate with a password, never through Google.
				// A federated account puts control of the admin role in the hands
				// of whoever controls the Google account, and a Google session
				// cannot be revoked from here.
				if (user.getProvider() != AuthProvider.LOCAL) {
					log.warn("Refusing to promote {} to ADMIN: it is a {} account, and "
							+ "admins must sign in with a password.", email, user.getProvider());
					return;
				}

				if (user.getRole() != Role.ADMIN) {
					user.setRole(Role.ADMIN);
					log.info("Promoted {} to ADMIN", email);
				}
			}, () -> log.warn("Cannot promote {} to ADMIN: no such account yet. "
					+ "Sign up with that address, then restart.", email));
		}
	}

}
