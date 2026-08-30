package com.agriknowledge.auth;

import com.agriknowledge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "verification_codes")
public class VerificationCode {

	/** Six digits is what people expect to type from an email. */
	public static final int MAX_ATTEMPTS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private VerificationPurpose purpose;

	/** SHA-256 of the code. The code itself only ever exists in the email. */
	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected VerificationCode() {
		// for JPA
	}

	public VerificationCode(User user, VerificationPurpose purpose, String codeHash, Instant expiresAt) {
		this.user = user;
		this.purpose = purpose;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public boolean isUsable(Instant now) {
		return consumedAt == null && attempts < MAX_ATTEMPTS && expiresAt.isAfter(now);
	}

	public boolean matches(String candidateHash) {
		return codeHash.equals(candidateHash);
	}

	/** Counted whether or not the guess was right, so guessing has a fixed budget. */
	public void recordAttempt() {
		this.attempts++;
	}

	public void consume() {
		this.consumedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public VerificationPurpose getPurpose() {
		return purpose;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}

}
