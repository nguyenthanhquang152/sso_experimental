package com.demo.sso.service;

import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void findOrCreateUser_createsNewUserWhenNotFound() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = userService.findOrCreateUser(
            "google-123", "user@example.com", "John", "http://pic.url", "CLIENT_SIDE");

        assertEquals("google-123", result.getGoogleId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("John", result.getName());
        assertEquals("http://pic.url", result.getPictureUrl());
        assertEquals("CLIENT_SIDE", result.getLoginMethod());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("google-123", captor.getValue().getGoogleId());
    }

    @Test
    void findOrCreateUser_updatesExistingUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setGoogleId("google-123");
        existing.setEmail("user@example.com");
        existing.setName("Old Name");
        existing.setPictureUrl("http://old-pic.url");
        existing.setLoginMethod("SERVER_SIDE");

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreateUser(
            "google-123", "user@example.com", "New Name", "http://new-pic.url", "CLIENT_SIDE");

        assertEquals("New Name", result.getName());
        assertEquals("http://new-pic.url", result.getPictureUrl());
        assertEquals("CLIENT_SIDE", result.getLoginMethod());
        assertNotNull(result.getLastLoginAt());
    }

    @Test
    void findOrCreateUser_updatesLastLoginAtForExistingUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setGoogleId("google-123");
        existing.setEmail("user@example.com");

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreateUser(
            "google-123", "user@example.com", "Name", "http://pic.url", "SERVER_SIDE");

        assertNotNull(result.getLastLoginAt());
    }

    @Test
    void findByEmail_delegatesToRepository() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("user@example.com");

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_returnsEmptyForUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("unknown@example.com");

        assertFalse(result.isPresent());
    }
}
