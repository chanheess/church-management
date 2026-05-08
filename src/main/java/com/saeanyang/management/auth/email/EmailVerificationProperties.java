package com.saeanyang.management.auth.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.email-verification")
public class EmailVerificationProperties {

    private int codeTtlMinutes = 10;
    private int maxAttempts = 5;
    private int trustedDeviceDays = 30;
    private boolean logCodeWhenMailFails = true;
    private String from = "no-reply@church-management.local";

    public int getCodeTtlMinutes() {
        return codeTtlMinutes;
    }

    public void setCodeTtlMinutes(int codeTtlMinutes) {
        this.codeTtlMinutes = codeTtlMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getTrustedDeviceDays() {
        return trustedDeviceDays;
    }

    public void setTrustedDeviceDays(int trustedDeviceDays) {
        this.trustedDeviceDays = trustedDeviceDays;
    }

    public boolean isLogCodeWhenMailFails() {
        return logCodeWhenMailFails;
    }

    public void setLogCodeWhenMailFails(boolean logCodeWhenMailFails) {
        this.logCodeWhenMailFails = logCodeWhenMailFails;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
