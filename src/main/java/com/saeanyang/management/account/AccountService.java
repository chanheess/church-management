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
    public User createAdmin(String username, String passwordHash, String email) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        requirePasswordHash(passwordHash);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        User user = new User(normalizedUsername, passwordHash.trim(), normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(normalizeUsername(username));
    }

    @Transactional
    public void recordLoginSuccess(String username) {
        User user = getByUsername(username);
        user.recordLoginSuccess(Instant.now(clock));
    }

    @Transactional
    public void recordLoginFailure(String username) {
        User user = getByUsername(username);
        Instant lockUntil = Instant.now(clock).plus(DEFAULT_LOGIN_LOCK_DURATION);
        user.recordLoginFailure(DEFAULT_MAX_LOGIN_FAILURE_COUNT, lockUntil);
    }

    @Transactional
    public void unlock(String username) {
        User user = getByUsername(username);
        user.unlock();
    }

    @Transactional
    public void setEnabled(String username, boolean enabled) {
        User user = getByUsername(username);
        user.setEnabled(enabled);
    }

    private User getByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username))
            .orElseThrow(() -> new IllegalArgumentException("아이디를 찾을 수 없습니다."));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디가 필요합니다.");
        }
        return username.trim().toLowerCase(Locale.ROOT);
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
