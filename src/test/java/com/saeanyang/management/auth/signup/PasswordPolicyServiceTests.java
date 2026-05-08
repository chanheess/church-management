package com.saeanyang.management.auth.signup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyServiceTests {

    private final PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @Test
    void acceptsStrongPassword() {
        assertThat(passwordPolicyService.validate("admin@example.com", "SafePass!2026"))
            .isEmpty();
    }

    @Test
    void rejectsPasswordWithoutRequiredCharacterTypes() {
        assertThat(passwordPolicyService.validate("admin@example.com", "lowercaseonly"))
            .contains(
                "비밀번호에는 대문자가 1자 이상 포함되어야 합니다.",
                "비밀번호에는 숫자가 1자 이상 포함되어야 합니다.",
                "비밀번호에는 특수문자가 1자 이상 포함되어야 합니다."
            );
    }

    @Test
    void rejectsPasswordContainingEmailLocalPart() {
        assertThat(passwordPolicyService.validate("church@example.com", "SafeChurch!2026"))
            .contains("비밀번호에는 이메일 주소 일부를 포함할 수 없습니다.");
    }

    @Test
    void rejectsShortAndCommonPassword() {
        assertThat(passwordPolicyService.validate("admin@example.com", "admin123"))
            .contains(
                "비밀번호는 10자 이상이어야 합니다.",
                "너무 흔한 비밀번호는 사용할 수 없습니다."
            );
    }
}
