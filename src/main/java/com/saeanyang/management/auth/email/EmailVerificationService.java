package com.saeanyang.management.auth.email;

import com.saeanyang.management.account.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final EmailVerificationProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public EmailVerificationService(
        EmailVerificationCodeRepository codeRepository,
        PasswordEncoder passwordEncoder,
        JavaMailSender mailSender,
        EmailVerificationProperties properties
    ) {
        this(codeRepository, passwordEncoder, mailSender, properties, Clock.systemUTC());
    }

    EmailVerificationService(
        EmailVerificationCodeRepository codeRepository,
        PasswordEncoder passwordEncoder,
        JavaMailSender mailSender,
        EmailVerificationProperties properties,
        Clock clock
    ) {
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void issueCode(User user) {
        String code = createCode();
        Instant expiresAt = Instant.now(clock).plus(Duration.ofMinutes(properties.getCodeTtlMinutes()));
        codeRepository.save(new EmailVerificationCode(user, passwordEncoder.encode(code), expiresAt));
        sendCode(user, code);
    }

    @Transactional
    public boolean verify(User user, String submittedCode) {
        if (submittedCode == null || submittedCode.trim().isEmpty()) {
            return false;
        }

        Instant now = Instant.now(clock);
        return codeRepository
            .findFirstByUserAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(user, now)
            .map(code -> verifyCode(code, submittedCode.trim(), now))
            .orElse(false);
    }

    private boolean verifyCode(EmailVerificationCode code, String submittedCode, Instant now) {
        if (code.isConsumed() || code.isExpired(now) || code.getAttempts() >= properties.getMaxAttempts()) {
            return false;
        }

        code.incrementAttempts();
        boolean matches = passwordEncoder.matches(submittedCode, code.getCodeHash());
        if (matches) {
            code.consume(now);
        }
        return matches;
    }

    private void sendCode(User user, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(user.getEmail());
        message.setSubject("[Church Management] 로그인 인증 코드");
        message.setText("로그인 인증 코드는 " + code + " 입니다. "
            + properties.getCodeTtlMinutes() + "분 안에 입력해 주세요.");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            if (properties.isLogCodeWhenMailFails()) {
                log.warn(
                    "이메일 발송 실패. 개발용 인증 코드 user={} code={} cause={}",
                    user.getUsername(),
                    code,
                    e.getMessage()
                );
            } else {
                log.warn("이메일 발송 실패 user={}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

    private String createCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
