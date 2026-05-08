package com.saeanyang.management.auth.signup;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class SignupIpAllowlistService {

    private final SignupProperties signupProperties;

    public SignupIpAllowlistService(SignupProperties signupProperties) {
        this.signupProperties = signupProperties;
    }

    public boolean isAllowed(HttpServletRequest request) {
        String clientIp = normalizeIp(resolveClientIp(request));
        return signupProperties.getAllowedNetworks().stream()
            .filter(network -> network != null && !network.isBlank())
            .anyMatch(network -> matches(clientIp, network.trim()));
    }

    public String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "::1";
        }
        return ip;
    }

    private boolean matches(String clientIp, String network) {
        if (!network.contains("/")) {
            return clientIp.equals(network);
        }

        try {
            String[] parts = network.split("/", 2);
            InetAddress address = InetAddress.getByName(clientIp);
            InetAddress base = InetAddress.getByName(parts[0]);
            if (address.getAddress().length != base.getAddress().length) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            byte[] addressBytes = address.getAddress();
            byte[] baseBytes = base.getAddress();
            int bitLength = addressBytes.length * 8;
            if (prefixLength < 0 || prefixLength > bitLength) {
                return false;
            }

            BigInteger addressValue = new BigInteger(1, addressBytes);
            BigInteger baseValue = new BigInteger(1, baseBytes);
            BigInteger mask = BigInteger.ONE.shiftLeft(bitLength).subtract(BigInteger.ONE)
                .shiftRight(prefixLength)
                .not()
                .and(BigInteger.ONE.shiftLeft(bitLength).subtract(BigInteger.ONE));

            return addressValue.and(mask).equals(baseValue.and(mask));
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }
}
