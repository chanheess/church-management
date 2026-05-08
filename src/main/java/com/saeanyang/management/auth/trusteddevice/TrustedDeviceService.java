package com.saeanyang.management.auth.trusteddevice;

import com.saeanyang.management.account.User;
import com.saeanyang.management.auth.email.EmailVerificationProperties;
import com.saeanyang.management.auth.support.SecurityTokenHasher;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class TrustedDeviceService {

    public static final String TRUSTED_DEVICE_COOKIE = "trusted_device";

    private final TrustedDeviceRepository trustedDeviceRepository;
    private final SecurityTokenHasher tokenHasher;
    private final EmailVerificationProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public TrustedDeviceService(
        TrustedDeviceRepository trustedDeviceRepository,
        SecurityTokenHasher tokenHasher,
        EmailVerificationProperties properties
    ) {
        this(trustedDeviceRepository, tokenHasher, properties, Clock.systemUTC());
    }

    TrustedDeviceService(
        TrustedDeviceRepository trustedDeviceRepository,
        SecurityTokenHasher tokenHasher,
        EmailVerificationProperties properties,
        Clock clock
    ) {
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.tokenHasher = tokenHasher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public boolean isTrusted(User user, HttpServletRequest request) {
        String token = readCookie(request);
        if (token == null || token.isBlank()) {
            return false;
        }

        Instant now = Instant.now(clock);
        return trustedDeviceRepository.findByUserAndTokenHashAndRevokedAtIsNull(user, tokenHasher.sha256(token))
            .filter(device -> device.isActive(now))
            .map(device -> {
                device.recordUse(resolveClientIp(request), now);
                return true;
            })
            .orElse(false);
    }

    @Transactional
    public void trustCurrentDevice(User user, HttpServletRequest request, HttpServletResponse response) {
        String token = createToken();
        Instant trustedUntil = Instant.now(clock).plus(Duration.ofDays(properties.getTrustedDeviceDays()));

        TrustedDevice trustedDevice = new TrustedDevice(
            user,
            tokenHasher.sha256(token),
            tokenHasher.sha256(nullToEmpty(request.getHeader("User-Agent"))),
            resolveClientIp(request),
            trustedUntil
        );
        trustedDeviceRepository.save(trustedDevice);

        ResponseCookie cookie = ResponseCookie.from(TRUSTED_DEVICE_COOKIE, token)
            .httpOnly(true)
            .secure(isSecureRequest(request))
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(properties.getTrustedDeviceDays()))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TRUSTED_DEVICE_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
