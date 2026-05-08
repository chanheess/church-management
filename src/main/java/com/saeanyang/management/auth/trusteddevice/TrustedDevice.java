package com.saeanyang.management.auth.trusteddevice;

import com.saeanyang.management.account.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "trusted_devices",
    indexes = {
        @Index(name = "idx_trusted_devices_token_hash", columnList = "token_hash"),
        @Index(name = "idx_trusted_devices_user_until", columnList = "user_id, trusted_until")
    }
)
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;

    @Column(name = "last_ip", length = 80)
    private String lastIp;

    @Column(name = "trusted_until", nullable = false)
    private Instant trustedUntil;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected TrustedDevice() {
    }

    public TrustedDevice(User user, String tokenHash, String userAgentHash, String lastIp, Instant trustedUntil) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.userAgentHash = userAgentHash;
        this.lastIp = lastIp;
        this.trustedUntil = trustedUntil;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public Instant getTrustedUntil() {
        return trustedUntil;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && trustedUntil.isAfter(now);
    }

    public void recordUse(String lastIp, Instant now) {
        this.lastIp = lastIp;
        this.lastUsedAt = now;
    }
}
