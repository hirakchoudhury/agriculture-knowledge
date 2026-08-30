package com.agriknowledge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

	/** The newest code for this person and purpose; older ones are already void. */
	@Query("""
			select c from VerificationCode c
			where c.user.id = :userId and c.purpose = :purpose
			order by c.createdAt desc
			limit 1
			""")
	Optional<VerificationCode> findNewest(@Param("userId") Long userId,
			@Param("purpose") VerificationPurpose purpose);

	/**
	 * Issuing a new code voids the old ones. Without this, every code ever sent
	 * stays live until it expires, which multiplies the guessing surface.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update VerificationCode c set c.consumedAt = :now
			where c.user.id = :userId and c.purpose = :purpose and c.consumedAt is null
			""")
	int voidOutstanding(@Param("userId") Long userId, @Param("purpose") VerificationPurpose purpose,
			@Param("now") Instant now);

	@Modifying
	@Query("delete from VerificationCode c where c.expiresAt < :cutoff")
	int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

}
