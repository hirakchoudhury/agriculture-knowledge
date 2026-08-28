package com.agriknowledge.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	/** Null for accounts that only ever sign in through Google. */
	@Column(name = "password_hash")
	private String passwordHash;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.USER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider provider = AuthProvider.LOCAL;

	/** The provider's own stable identifier — Google's `sub` claim. */
	@Column(name = "provider_id")
	private String providerId;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
		// for JPA
	}

	public static User localAccount(String email, String passwordHash, String name) {
		User user = new User();
		user.email = normaliseEmail(email);
		user.passwordHash = passwordHash;
		user.name = name;
		user.provider = AuthProvider.LOCAL;
		return user;
	}

	public static User googleAccount(String email, String name, String avatarUrl, String googleSubject) {
		User user = new User();
		user.email = normaliseEmail(email);
		user.name = name;
		user.avatarUrl = avatarUrl;
		user.provider = AuthProvider.GOOGLE;
		user.providerId = googleSubject;
		return user;
	}

	/**
	 * Sign-up and login must agree on casing, or Ada@example.com and ada@example.com
	 * become two accounts. Lower-casing here keeps the unique index sufficient.
	 */
	public static String normaliseEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public Role getRole() {
		return role;
	}

	/**
	 * Only reachable from {@link com.agriknowledge.config.AdminBootstrap}. There is
	 * deliberately no endpoint that changes a role.
	 */
	public void setRole(Role role) {
		this.role = role;
	}

	public AuthProvider getProvider() {
		return provider;
	}

	public String getProviderId() {
		return providerId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
