package com.saeanyang.management.auth.signup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "security.signup")
public class SignupProperties {

    private List<String> allowedNetworks = new ArrayList<>();

    public List<String> getAllowedNetworks() {
        return allowedNetworks;
    }

    public void setAllowedNetworks(List<String> allowedNetworks) {
        this.allowedNetworks = allowedNetworks;
    }
}
