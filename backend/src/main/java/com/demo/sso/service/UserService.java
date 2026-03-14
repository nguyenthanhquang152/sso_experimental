package com.demo.sso.service;

import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateUser(NormalizedIdentity identity) {
        Optional<User> existing = userRepository.findByProviderAndProviderUserId(
            identity.provider(),
            identity.providerUserId()
        );

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
        user.setGoogleId(legacyGoogleCompatibilityId(identity));
        user.setEmail(identity.email());
        user.setName(identity.name());
        user.setPictureUrl(identity.pictureUrl());
        applyProviderIdentityFields(user, identity);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            if (identity.provider() != AuthProvider.GOOGLE) {
                return userRepository.findByProviderAndProviderUserId(identity.provider(), identity.providerUserId())
                    .orElseThrow(() -> new IllegalStateException("Concurrent user creation failed", e));
            }
            return userRepository.findByGoogleId(identity.providerUserId())
                .orElseThrow(() -> new IllegalStateException("Concurrent user creation failed", e));
        }
    }

    private static void applyProviderIdentityFields(User user, NormalizedIdentity identity) {
        user.setProvider(identity.provider());
        user.setProviderUserId(identity.providerUserId());
        user.setLastLoginFlow(identity.loginFlow());
        user.setGoogleId(legacyGoogleCompatibilityId(identity));
    }

    private static String legacyGoogleCompatibilityId(NormalizedIdentity identity) {
        if (identity.provider() == AuthProvider.GOOGLE) {
            return identity.providerUserId();
        }
        return identity.provider().name().toLowerCase() + ":" + identity.providerUserId();
    }

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
