package com.saeanyang.management.auth.web;

import com.saeanyang.management.account.AccountService;
import com.saeanyang.management.account.User;
import com.saeanyang.management.auth.email.EmailVerificationService;
import com.saeanyang.management.auth.login.EmailVerificationSession;
import com.saeanyang.management.auth.signup.PasswordPolicyService;
import com.saeanyang.management.auth.signup.SignupIpAllowlistService;
import com.saeanyang.management.auth.trusteddevice.TrustedDeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AuthenticationController {

    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SignupIpAllowlistService signupIpAllowlistService;
    private final EmailVerificationService emailVerificationService;
    private final TrustedDeviceService trustedDeviceService;

    public AuthenticationController(
        AccountService accountService,
        PasswordEncoder passwordEncoder,
        PasswordPolicyService passwordPolicyService,
        SignupIpAllowlistService signupIpAllowlistService,
        EmailVerificationService emailVerificationService,
        TrustedDeviceService trustedDeviceService
    ) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.signupIpAllowlistService = signupIpAllowlistService;
        this.emailVerificationService = emailVerificationService;
        this.trustedDeviceService = trustedDeviceService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(HttpServletRequest request, Model model) {
        if (!signupIpAllowlistService.isAllowed(request)) {
            model.addAttribute("blockedIp", signupIpAllowlistService.resolveClientIp(request));
            return "signup-blocked";
        }
        if (!model.containsAttribute("signupRequest")) {
            model.addAttribute("signupRequest", new SignupRequest("", "", "", ""));
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
        HttpServletRequest request,
        @Valid SignupRequest signupRequest,
        BindingResult bindingResult,
        Model model
    ) {
        requireSignupIp(request);
        if (!signupRequest.password().equals(signupRequest.passwordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "passwordConfirm.mismatch", "비밀번호가 일치하지 않습니다.");
        }
        for (String error : passwordPolicyService.validate(
            signupRequest.username(),
            signupRequest.email(),
            signupRequest.password()
        )) {
            bindingResult.rejectValue("password", "password.weak", error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("signupRequest", signupRequest);
            return "signup";
        }

        accountService.createAdmin(
            signupRequest.username(),
            passwordEncoder.encode(signupRequest.password()),
            signupRequest.email()
        );
        return "redirect:/login?signup";
    }

    @GetMapping("/login/email-verification")
    public String emailVerificationForm(HttpServletRequest request) {
        if (request.getSession(false) == null
            || !Boolean.TRUE.equals(request.getSession(false).getAttribute(EmailVerificationSession.REQUIRED))) {
            return "redirect:/login";
        }
        return "email-verification";
    }

    @PostMapping("/login/email-verification")
    public String verifyEmailCode(
        HttpServletRequest request,
        HttpServletResponse response,
        String code,
        Model model
    ) {
        Object username = request.getSession().getAttribute(EmailVerificationSession.USERNAME);
        if (username == null) {
            return "redirect:/login";
        }

        User user = accountService.findByUsername(username.toString())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!emailVerificationService.verify(user, code)) {
            model.addAttribute("error", "인증 코드가 올바르지 않거나 만료되었습니다.");
            return "email-verification";
        }

        trustedDeviceService.trustCurrentDevice(user, request, response);
        request.getSession().removeAttribute(EmailVerificationSession.REQUIRED);
        request.getSession().removeAttribute(EmailVerificationSession.USERNAME);
        accountService.recordLoginSuccess(user.getUsername());
        return "redirect:/bulletin";
    }

    private void requireSignupIp(HttpServletRequest request) {
        if (!signupIpAllowlistService.isAllowed(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "허용된 IP에서만 회원가입할 수 있습니다.");
        }
    }

    public record SignupRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String password,
        @NotBlank @Size(max = 100) String passwordConfirm
    ) {
    }
}
