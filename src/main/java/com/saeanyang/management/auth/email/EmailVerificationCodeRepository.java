package com.saeanyang.management.auth.email;

import com.saeanyang.management.account.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findFirstByUserAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
        User user,
        Instant now
    );
}
