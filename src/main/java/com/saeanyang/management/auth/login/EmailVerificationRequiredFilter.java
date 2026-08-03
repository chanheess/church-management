package com.saeanyang.management.auth.login;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class EmailVerificationRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
        "/login",
        "/login/email-verification",
        "/logout",
        "/health",
        "/favicon.ico",
        "/weekly-bulletin.css",
        "/attendance.css",
        "/representative-prayer.css"
    );

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Object required = request.getSession(false) == null
            ? null
            : request.getSession(false).getAttribute(EmailVerificationSession.REQUIRED);

        if (Boolean.TRUE.equals(required) && !isAllowed(request.getRequestURI())) {
            response.sendRedirect("/login/email-verification");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String path) {
        return ALLOWED_PATHS.contains(path)
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.startsWith("/webjars/");
    }
}
