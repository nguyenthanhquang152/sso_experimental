package com.demo.sso.repository;

import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
    List<User> findAllByEmail(String email);
}
