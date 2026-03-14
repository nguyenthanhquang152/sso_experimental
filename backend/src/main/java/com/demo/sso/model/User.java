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

    @Column(name = "google_id", unique = true, nullable = true)
    private String googleId;

    @Column(nullable = false)
    private String email;

    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "login_method", length = 20)
    private String loginMethod;

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

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    /** @deprecated Use {@link #getLastLoginFlow()} instead. Kept for DB column backward compatibility. */
    @Deprecated
    public String getLoginMethod() { return loginMethod; }
    /** @deprecated Use {@link #setLastLoginFlow(AuthFlow)} instead. */
    @Deprecated
    public void setLoginMethod(String loginMethod) { this.loginMethod = loginMethod; }

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
