package com.saeanyang.management.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private static final int DEFAULT_MAX_LOGIN_FAILURE_COUNT = 5;
    private static final Duration DEFAULT_LOGIN_LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public AccountService(UserRepository userRepository) {
        this(userRepository, Clock.systemUTC());
    }

    AccountService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public User createAdmin(String email, String passwordHash) {
        String normalizedEmail = normalizeEmail(email);
        requirePasswordHash(passwordHash);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        User user = new User(passwordHash.trim(), normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Transactional
    public void recordLoginSuccess(String email) {
        User user = getByEmail(email);
        user.recordLoginSuccess(Instant.now(clock));
    }

    @Transactional
    public void recordLoginFailure(String email) {
        User user = getByEmail(email);
        Instant lockUntil = Instant.now(clock).plus(DEFAULT_LOGIN_LOCK_DURATION);
        user.recordLoginFailure(DEFAULT_MAX_LOGIN_FAILURE_COUNT, lockUntil);
    }

    @Transactional
    public void unlock(String email) {
        User user = getByEmail(email);
        user.unlock();
    }

    @Transactional
    public void setEnabled(String email, boolean enabled) {
        User user = getByEmail(email);
        user.setEnabled(enabled);
    }

    private User getByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new IllegalArgumentException("이메일 계정을 찾을 수 없습니다."));
    }

    private void requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호 해시가 필요합니다.");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일이 필요합니다.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
