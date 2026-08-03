package com.saeanyang.management.auth.login;

import com.saeanyang.management.account.AccountService;
import com.saeanyang.management.account.User;
import com.saeanyang.management.auth.email.EmailVerificationService;
import com.saeanyang.management.auth.trusteddevice.TrustedDeviceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EmailVerificationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AccountService accountService;
    private final TrustedDeviceService trustedDeviceService;
    private final EmailVerificationService emailVerificationService;

    public EmailVerificationSuccessHandler(
        AccountService accountService,
        TrustedDeviceService trustedDeviceService,
        EmailVerificationService emailVerificationService
    ) {
        this.accountService = accountService;
        this.trustedDeviceService = trustedDeviceService;
        this.emailVerificationService = emailVerificationService;
        setDefaultTargetUrl("/bulletin");
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        User user = accountService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));

        if (trustedDeviceService.isTrusted(user, request)) {
            request.getSession().removeAttribute(EmailVerificationSession.REQUIRED);
            request.getSession().removeAttribute(EmailVerificationSession.EMAIL);
            accountService.recordLoginSuccess(user.getEmail());
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        emailVerificationService.issueCode(user);
        request.getSession().setAttribute(EmailVerificationSession.REQUIRED, true);
        request.getSession().setAttribute(EmailVerificationSession.EMAIL, user.getEmail());
        response.sendRedirect("/login/email-verification");
    }
}
