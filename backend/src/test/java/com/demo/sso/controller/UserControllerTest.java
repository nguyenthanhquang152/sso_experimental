package com.demo.sso.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.demo.sso.controller.dto.UserResponse;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.user.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService);
    }

    @Test
    void returnsUserWhenAuthenticatedWithV2Identity() {
        AuthenticatedUserIdentity identity = AuthenticatedUserIdentity.v2(
                42L, "alice@example.com", AuthProvider.GOOGLE, "google-sub-123");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(identity);

        User user = buildUser(42L, "alice@example.com", "Alice", AuthProvider.GOOGLE);
        when(userService.findCurrentUser(identity)).thenReturn(Optional.of(user));

        ResponseEntity<UserResponse> response = controller.getCurrentUser(auth);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("alice@example.com", response.getBody().email());
        assertEquals("Alice", response.getBody().name());
    }

    @Test
    void returns404WhenUserNotFound() {
        AuthenticatedUserIdentity identity = AuthenticatedUserIdentity.v2(
                99L, "unknown@example.com", AuthProvider.GOOGLE, "google-sub-999");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(identity);
        when(userService.findCurrentUser(identity)).thenReturn(Optional.empty());

        ResponseEntity<UserResponse> response = controller.getCurrentUser(auth);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void fallsBackToLegacyIdentityForNonStandardPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("legacy-principal");
        when(auth.getName()).thenReturn("legacy@example.com");

        User user = buildUser(1L, "legacy@example.com", "Legacy User", AuthProvider.GOOGLE);
        // Legacy identity matches by email, so capture any AuthenticatedUserIdentity
        when(userService.findCurrentUser(org.mockito.ArgumentMatchers.any(AuthenticatedUserIdentity.class)))
                .thenReturn(Optional.of(user));

        ResponseEntity<UserResponse> response = controller.getCurrentUser(auth);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("legacy@example.com", response.getBody().email());
    }

    @Test
    void legacyFallbackReturns404WhenUserNotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("some-string-principal");
        when(auth.getName()).thenReturn("missing@example.com");

        when(userService.findCurrentUser(org.mockito.ArgumentMatchers.any(AuthenticatedUserIdentity.class)))
                .thenReturn(Optional.empty());

        ResponseEntity<UserResponse> response = controller.getCurrentUser(auth);

        assertEquals(404, response.getStatusCode().value());
    }

    private static User buildUser(Long id, String email, String name, AuthProvider provider) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setProvider(provider);
        user.setProviderUserId("provider-user-id");
        user.setLastLoginFlow(AuthFlow.CLIENT_SIDE);
        user.setCreatedAt(Instant.parse("2024-01-15T10:30:00Z"));
        user.setLastLoginAt(Instant.parse("2024-06-01T08:00:00Z"));
        return user;
    }
}
