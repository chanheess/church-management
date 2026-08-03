package com.saeanyang.management.auth.trusteddevice;

import com.saeanyang.management.account.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {

    Optional<TrustedDevice> findByUserAndTokenHashAndRevokedAtIsNull(User user, String tokenHash);
}
