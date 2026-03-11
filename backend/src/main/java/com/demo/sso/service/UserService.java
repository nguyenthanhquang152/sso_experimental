package com.demo.sso.service;

import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateUser(String googleId, String email, String name,
                                  String pictureUrl, String loginMethod) {
        Optional<User> existing = userRepository.findByGoogleId(googleId);

        if (existing.isPresent()) {
            User user = existing.get();
            user.setName(name);
            user.setPictureUrl(pictureUrl);
            user.setLoginMethod(loginMethod);
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl(pictureUrl);
        user.setLoginMethod(loginMethod);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
