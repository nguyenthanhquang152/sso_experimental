package com.demo.sso.service.auth;

import com.demo.sso.exception.InvalidIdentityException;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.model.NormalizedIdentity;
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
 * <p>All providers use the provider-neutral {@code (provider, provider_user_id)} pair
 * for user lookup. The legacy {@code google_id} column is retained in the database
 * for backward compatibility but is no longer queried in production code paths.
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
     */
    private User recoverFromConcurrentCreation(NormalizedIdentity identity, DataIntegrityViolationException cause) {
        return userRepository.findByProviderAndProviderUserId(identity.provider(), identity.providerUserId())
            .orElseThrow(() -> new InvalidIdentityException(
                "Concurrent user creation recovery failed for provider: " + identity.provider(), cause));
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
