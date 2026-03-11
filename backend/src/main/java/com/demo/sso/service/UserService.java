package com.demo.sso.service;

import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateUser(String googleId, String email, String name,
                                  String pictureUrl, String loginMethod) {
        Optional<User> existing = userRepository.findByGoogleId(googleId);

        if (existing.isPresent()) {
            User user = existing.get();
            user.setName(name);
            user.setPictureUrl(pictureUrl);
            user.setLoginMethod(loginMethod);
            user.setLastLoginAt(Instant.now());
            return userRepository.save(user);
        }

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl(pictureUrl);
        user.setLoginMethod(loginMethod);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new IllegalStateException("Concurrent user creation failed", e));
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
