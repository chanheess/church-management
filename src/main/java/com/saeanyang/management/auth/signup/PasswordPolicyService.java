package com.saeanyang.management.auth.signup;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 10;
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password",
        "password1",
        "password123",
        "qwerty123",
        "admin123",
        "letmein",
        "welcome1",
        "iloveyou",
        "church123"
    );

    public List<String> validate(String email, String password) {
        List<String> errors = new java.util.ArrayList<>();
        String value = password == null ? "" : password;

        if (value.length() < MIN_LENGTH) {
            errors.add("비밀번호는 10자 이상이어야 합니다.");
        }
        if (value.chars().noneMatch(Character::isUpperCase)) {
            errors.add("비밀번호에는 대문자가 1자 이상 포함되어야 합니다.");
        }
        if (value.chars().noneMatch(Character::isLowerCase)) {
            errors.add("비밀번호에는 소문자가 1자 이상 포함되어야 합니다.");
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            errors.add("비밀번호에는 숫자가 1자 이상 포함되어야 합니다.");
        }
        if (value.chars().noneMatch(this::isSpecialCharacter)) {
            errors.add("비밀번호에는 특수문자가 1자 이상 포함되어야 합니다.");
        }
        if (containsEmailLocalPart(value, email)) {
            errors.add("비밀번호에는 이메일 주소 일부를 포함할 수 없습니다.");
        }
        if (COMMON_PASSWORDS.contains(value.toLowerCase(Locale.ROOT))) {
            errors.add("너무 흔한 비밀번호는 사용할 수 없습니다.");
        }

        return errors;
    }

    private boolean isSpecialCharacter(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }

    private boolean containsIgnoreCase(String value, String token) {
        if (token == null || token.trim().length() < 3) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(token.trim().toLowerCase(Locale.ROOT));
    }

    private boolean containsEmailLocalPart(String value, String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String localPart = email.substring(0, email.indexOf('@'));
        return containsIgnoreCase(value, localPart);
    }
}
