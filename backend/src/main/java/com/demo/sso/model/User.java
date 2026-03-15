package com.demo.sso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Persistent user entity.
 *
 * <h3>Legacy column retention</h3>
 * <ul>
 *   <li>{@code google_id} — Superseded by the provider-neutral {@code provider} +
 *       {@code provider_user_id} pair. The field is retained for backward compatibility
 *       with existing database rows but is no longer written to (setter is a no-op).
 *       The column can be dropped in a future migration once all legacy data is cleaned up.
 *   <li>{@code login_method} (SQL column) — Renamed to {@code last_login_flow} in
 *       the V3 migration. The old column is retained in the database schema for
 *       rollback safety but is no longer mapped by JPA.
 * </ul>
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_provider_identity",
        columnNames = {"provider", "provider_user_id"}
    )
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @deprecated Legacy column — use {@code provider} + {@code providerUserId} instead.
     *             Retained read-only for backward compatibility with existing database rows.
     */
    @Deprecated
    @Column(name = "google_id", nullable = true)
    private String googleId;

    @Column(nullable = false)
    private String email;

    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_login_flow", length = 20)
    private AuthFlow lastLoginFlow;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastLoginAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Deprecated
    public String getGoogleId() { return googleId; }
    /** @deprecated No-op — google_id is no longer written. Field retained for existing data. */
    @Deprecated
    public void setGoogleId(String googleId) { /* no-op: legacy field, no longer written */ }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }

    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }

    public AuthFlow getLastLoginFlow() { return lastLoginFlow; }
    public void setLastLoginFlow(AuthFlow lastLoginFlow) { this.lastLoginFlow = lastLoginFlow; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
