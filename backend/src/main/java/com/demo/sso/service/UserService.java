package com.demo.sso.service;

import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core user lifecycle service: lookup, creation, and update.
 *
 * <h3>Legacy Google identity branches (migration artifact)</h3>
 * <p>Several methods contain {@code AuthProvider.GOOGLE}-specific branches that
 * fall back to the legacy {@code google_id} column. These exist because users
 * created before the V3 migration may only be findable via {@code google_id},
 * not yet via the provider-neutral {@code (provider, provider_user_id)} pair.
 *
 * <p>Affected methods:
 * <ul>
 *   <li>{@link #syncUser} — falls back to {@code findByGoogleId}
 *   <li>{@link #createNewUser} — populates {@code google_id} for new Google users
 *   <li>{@link #recoverFromConcurrentCreation} — recovers via {@code findByGoogleId}
 * </ul>
 *
 * <p><b>Removal:</b> once all Google users have logged in post-V3 migration and
 * the {@code google_id} column is dropped, these branches can be removed.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User syncUser(NormalizedIdentity identity) {
        Optional<User> existing = userRepository.findByProviderAndProviderUserId(
            identity.provider(),
            identity.providerUserId()
        );

        // Legacy migration fallback: pre-V3 Google users may only have google_id populated.
        // See class-level Javadoc for removal criteria.
        if (existing.isEmpty() && identity.provider() == AuthProvider.GOOGLE) {
            existing = userRepository.findByGoogleId(identity.providerUserId());
        }

        return existing.isPresent()
            ? updateExistingUser(existing.get(), identity)
            : createNewUser(identity);
    }

    private User updateExistingUser(User user, NormalizedIdentity identity) {
        user.setEmail(identity.email());
        user.setName(identity.name());
        user.setPictureUrl(identity.pictureUrl());
        applyProviderIdentityFields(user, identity);
        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    private User createNewUser(NormalizedIdentity identity) {
        User user = new User();
        if (identity.provider() == AuthProvider.GOOGLE) {
            user.setGoogleId(identity.providerUserId());
        }
        user.setEmail(identity.email());
        user.setName(identity.name());
        user.setPictureUrl(identity.pictureUrl());
        applyProviderIdentityFields(user, identity);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return recoverFromConcurrentCreation(identity, e);
        }
    }

    private User recoverFromConcurrentCreation(NormalizedIdentity identity, DataIntegrityViolationException cause) {
        if (identity.provider() != AuthProvider.GOOGLE) {
            return userRepository.findByProviderAndProviderUserId(identity.provider(), identity.providerUserId())
                .orElseThrow(() -> new IllegalStateException("Concurrent user creation failed", cause));
        }
        return userRepository.findByGoogleId(identity.providerUserId())
            .orElseThrow(() -> new IllegalStateException("Concurrent user creation failed", cause));
    }

    private static void applyProviderIdentityFields(User user, NormalizedIdentity identity) {
        user.setProvider(identity.provider());
        user.setProviderUserId(identity.providerUserId());
        user.setLastLoginFlow(identity.loginFlow());
    }

    /**
     * Finds a user by email address.
     *
     * @param email the email to search for
     * @return the matching user, or empty if none found
     * @throws IllegalStateException if multiple users share the same email (ambiguous legacy data)
     */
    public Optional<User> findByEmail(String email) {
        List<User> matches = userRepository.findAllByEmail(email);
        if (matches.size() > 1) {
            throw new IllegalStateException("Ambiguous legacy email identity for " + email);
        }
        return matches.stream().findFirst();
    }

    public Optional<User> findCurrentUser(AuthenticatedUserIdentity identity) {
        if (identity.isLegacy()) {
            return findByEmail(identity.email());
        }
        return userRepository.findById(identity.userId());
    }
}
