package com.saeanyang.management.auth.config;

import com.saeanyang.management.auth.email.EmailVerificationProperties;
import com.saeanyang.management.auth.login.EmailVerificationRequiredFilter;
import com.saeanyang.management.auth.login.EmailVerificationSuccessHandler;
import com.saeanyang.management.auth.signup.SignupProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties({EmailVerificationProperties.class, SignupProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        EmailVerificationSuccessHandler emailVerificationSuccessHandler,
        EmailVerificationRequiredFilter emailVerificationRequiredFilter
    ) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/login",
                    "/signup",
                    "/health",
                    "/favicon.ico",
                    "/weekly-bulletin.css",
                    "/attendance.css",
                    "/representative-prayer.css",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.successHandler(emailVerificationSuccessHandler))
            .logout(Customizer.withDefaults())
            .addFilterAfter(emailVerificationRequiredFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
