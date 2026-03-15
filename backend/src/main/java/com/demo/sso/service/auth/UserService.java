package com.demo.sso.service.auth;

import com.demo.sso.exception.InvalidIdentityException;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *   <li>{@link #recoverFromConcurrentCreation} — recovers via {@code findByGoogleId}
 * </ul>
 *
 * <p><b>Removal criteria:</b>
 * <ol>
 *   <li>{@code JwtMintMode.V2} deployed for &gt;= token expiration period
 *   <li>No legacy {@code findByGoogleId} fallback hits logged
 *   <li>{@code google_id} column can be dropped from database
 * </ol>
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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
            if (existing.isPresent()) {
                logger.info("Legacy findByGoogleId fallback triggered for provider={}", identity.provider());
            }
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
        // Migration cleanup: google_id no longer populated for new users.
        // Existing users retain google_id via updateExistingUser until column is dropped.
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

    /**
     * Recovers from a concurrent user creation race condition by re-querying the user.
     *
     * <p><b>Provider divergence:</b> Google recovery uses {@code findByGoogleId} because
     * pre-V3 users may only have the legacy {@code google_id} column populated, making
     * the provider-neutral {@code (provider, provider_user_id)} lookup unreliable for
     * Google users until all legacy rows are migrated. Non-Google providers always use
     * the provider-neutral lookup.
     */
    private User recoverFromConcurrentCreation(NormalizedIdentity identity, DataIntegrityViolationException cause) {
        if (identity.provider() != AuthProvider.GOOGLE) {
            return userRepository.findByProviderAndProviderUserId(identity.provider(), identity.providerUserId())
                .orElseThrow(() -> new InvalidIdentityException(
                    "Concurrent user creation recovery failed for provider: " + identity.provider()));
        }
        // Legacy fallback: pre-V3 Google users may only have google_id populated.
        return userRepository.findByGoogleId(identity.providerUserId())
            .orElseThrow(() -> new InvalidIdentityException(
                "Concurrent user creation recovery failed for provider: " + identity.provider()));
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
