package com.saeanyang.management.auth.config;

import com.saeanyang.management.account.AccountService;
import com.saeanyang.management.account.UserRepository;
import com.saeanyang.management.auth.email.EmailVerificationCodeRepository;
import com.saeanyang.management.auth.trusteddevice.TrustedDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Autowired
    private TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    void loginIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("로그인")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("회원가입")));
    }

    @Test
    void signupIsPublicFromAllowedIp() throws Exception {
        mockMvc.perform(get("/signup").with(request -> {
                request.setRemoteAddr("127.0.0.1");
                return request;
            }))
            .andExpect(status().isOk());
    }

    @Test
    void signupPageShowsBlockedMessageOutsideAllowedIp() throws Exception {
        mockMvc.perform(get("/signup").header("X-Forwarded-For", "203.0.113.10"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("회원가입은 허용된 IP에서만 가능합니다.")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("203.0.113.10")));
    }

    @Test
    void signupSubmitIsForbiddenOutsideAllowedIp() throws Exception {
        mockMvc.perform(post("/signup")
                .header("X-Forwarded-For", "203.0.113.10")
                .param("username", "blocked-signup")
                .param("email", "blocked-signup@example.com")
                .param("password", "SafePass!2026")
                .param("passwordConfirm", "SafePass!2026")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void signupRejectsWeakPassword() throws Exception {
        userRepository.findByUsername("weak-password").ifPresent(userRepository::delete);

        mockMvc.perform(post("/signup")
                .param("username", "weak-password")
                .param("email", "weak-password@example.com")
                .param("password", "password123")
                .param("passwordConfirm", "password123")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("signupRequest", "password"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("비밀번호에는 대문자가 1자 이상 포함되어야 합니다.")));

        assertThat(userRepository.existsByUsername("weak-password")).isFalse();
    }

    @Test
    void signupStoresStrongPasswordAsHash() throws Exception {
        trustedDeviceRepository.deleteAll();
        emailVerificationCodeRepository.deleteAll();
        userRepository.findByUsername("strong-password").ifPresent(userRepository::delete);

        mockMvc.perform(post("/signup")
                .param("username", "strong-password")
                .param("email", "strong-password@example.com")
                .param("password", "SafePass!2026")
                .param("passwordConfirm", "SafePass!2026")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?signup"));

        var user = userRepository.findByUsername("strong-password").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("SafePass!2026");
        assertThat(passwordEncoder.matches("SafePass!2026", user.getPasswordHash())).isTrue();
    }

    @Test
    void bulletinRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/bulletin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginWithValidUsernameAndPasswordRequiresEmailVerificationForNewDevice() throws Exception {
        createUser("login-test", "password123!", "login-test@example.com");

        mockMvc.perform(post("/login")
                .param("username", "login-test")
                .param("password", "password123!")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login/email-verification"));
    }

    @Test
    void loginWithInvalidPasswordReturnsToLoginWithError() throws Exception {
        createUser("wrong-password", "password123!", "wrong-password@example.com");

        mockMvc.perform(post("/login")
                .param("username", "wrong-password")
                .param("password", "bad-password")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }

    private void createUser(String username, String rawPassword, String email) {
        trustedDeviceRepository.deleteAll();
        emailVerificationCodeRepository.deleteAll();
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
        accountService.createAdmin(username, passwordEncoder.encode(rawPassword), email);
    }
}
