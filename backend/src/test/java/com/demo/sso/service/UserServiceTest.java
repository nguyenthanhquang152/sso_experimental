package com.demo.sso.service;

import com.demo.sso.service.user.UserService;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.auth.NormalizedIdentity;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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
        when(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = userService.findOrCreateUser(
            NormalizedIdentity.google("google-123", "user@example.com", "John", "http://pic.url", AuthFlow.CLIENT_SIDE));

        assertEquals("google-123", result.getGoogleId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("John", result.getName());
        assertEquals("http://pic.url", result.getPictureUrl());
        assertEquals(AuthProvider.GOOGLE, result.getProvider());
        assertEquals("google-123", result.getProviderUserId());
        assertEquals(AuthFlow.CLIENT_SIDE, result.getLastLoginFlow());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("google-123", captor.getValue().getGoogleId());
        assertEquals(AuthProvider.GOOGLE, captor.getValue().getProvider());
        assertEquals("google-123", captor.getValue().getProviderUserId());
        assertEquals(AuthFlow.CLIENT_SIDE, captor.getValue().getLastLoginFlow());
    }

    @Test
    void findOrCreateUser_updatesExistingUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setGoogleId("google-123");
        existing.setEmail("user@example.com");
        existing.setName("Old Name");
        existing.setPictureUrl("http://old-pic.url");
        existing.setLastLoginFlow(AuthFlow.SERVER_SIDE);

        when(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreateUser(
            NormalizedIdentity.google("google-123", "user@example.com", "New Name", "http://new-pic.url", AuthFlow.CLIENT_SIDE));

        assertEquals("New Name", result.getName());
        assertEquals("http://new-pic.url", result.getPictureUrl());
        assertEquals(AuthProvider.GOOGLE, result.getProvider());
        assertEquals("google-123", result.getProviderUserId());
        assertEquals(AuthFlow.CLIENT_SIDE, result.getLastLoginFlow());
        assertNotNull(result.getLastLoginAt());
    }

    @Test
    void findOrCreateUser_updatesLastLoginAtForExistingUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setGoogleId("google-123");
        existing.setEmail("user@example.com");

        when(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreateUser(
            NormalizedIdentity.google("google-123", "user@example.com", "Name", "http://pic.url", AuthFlow.SERVER_SIDE));

        assertNotNull(result.getLastLoginAt());
    }

    @Test
    void findByEmail_delegatesToRepository() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findAllByEmail("user@example.com")).thenReturn(java.util.List.of(user));

        Optional<User> result = userService.findByEmail("user@example.com");

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_returnsEmptyForUnknownEmail() {
        when(userRepository.findAllByEmail("unknown@example.com")).thenReturn(java.util.List.of());

        Optional<User> result = userService.findByEmail("unknown@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    void findOrCreateUser_prefersProviderAwareLookupBeforeLegacyGoogleId() {
        User existing = new User();
        existing.setId(7L);
        existing.setGoogleId("legacy-google-id");
        existing.setProvider(AuthProvider.GOOGLE);
        existing.setProviderUserId("google-123");
        existing.setEmail("user@example.com");

        when(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreateUser(
            NormalizedIdentity.google("google-123", "user@example.com", "Updated Name", "http://new-pic.url", AuthFlow.SERVER_SIDE));

        assertEquals(7L, result.getId());
        verify(userRepository, never()).findByGoogleId("google-123");
    }

    @Test
    void findCurrentUser_resolvesV2IdentityByUserId() {
        User user = new User();
        user.setId(9L);
        user.setEmail("user@example.com");

        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findCurrentUser(
            AuthenticatedUserIdentity.v2(9L, "user@example.com", AuthProvider.GOOGLE, "google-123"));

        assertTrue(result.isPresent());
        assertEquals(9L, result.get().getId());
    }

    @Test
    void findByEmail_throwsWhenLegacyResolutionIsAmbiguous() {
        User first = new User();
        first.setId(1L);
        first.setEmail("user@example.com");
        User second = new User();
        second.setId(2L);
        second.setEmail("user@example.com");
        when(userRepository.findAllByEmail("user@example.com")).thenReturn(java.util.List.of(first, second));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> userService.findByEmail("user@example.com"));

        assertTrue(error.getMessage().contains("Ambiguous legacy email identity"));
    }

    @Test
    void findOrCreateUser_createsMicrosoftUserFromProviderIdentity() {
        when(userRepository.findByProviderAndProviderUserId(AuthProvider.MICROSOFT,
            "https://login.microsoftonline.com/tenant/v2.0|ms-subject-123"))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        User result = userService.findOrCreateUser(
            NormalizedIdentity.microsoft(
                "https://login.microsoftonline.com/tenant/v2.0|ms-subject-123",
                "employee@example.com",
                "Microsoft User",
                null,
                AuthFlow.CLIENT_SIDE));

        assertEquals(99L, result.getId());
        assertEquals(AuthProvider.MICROSOFT, result.getProvider());
        assertEquals("https://login.microsoftonline.com/tenant/v2.0|ms-subject-123", result.getProviderUserId());
        assertEquals(AuthFlow.CLIENT_SIDE, result.getLastLoginFlow());
        // googleId should not be set for non-Google providers
        assertNull(result.getGoogleId());
    }
}
