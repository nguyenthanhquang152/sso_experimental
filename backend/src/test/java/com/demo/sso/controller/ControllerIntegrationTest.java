package com.demo.sso.controller;

import com.demo.sso.config.JwtConfig;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User createTestUser() {
        User user = new User();
        user.setGoogleId("google-test-123");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPictureUrl("http://example.com/pic.jpg");
        user.setLoginMethod("SERVER_SIDE");
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Nested
    class UserMeEndpoint {

        @Test
        void returns401WithoutToken() throws Exception {
            mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void returns401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/user/me")
                    .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void returnsUserProfileWithValidToken() throws Exception {
            User user = createTestUser();
            String token = jwtConfig.generateToken(user.getEmail(), user.getGoogleId());

            mockMvc.perform(get("/user/me")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.loginMethod").value("SERVER_SIDE"))
                .andExpect(jsonPath("$.pictureUrl").value("http://example.com/pic.jpg"));
        }

        @Test
        void returns404WhenUserNotInDatabase() throws Exception {
            // Token is valid but user doesn't exist in DB
            String token = jwtConfig.generateToken("nonexistent@example.com", "google-999");

            mockMvc.perform(get("/user/me")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class AuthGoogleVerifyEndpoint {

        @Test
        void returnsBadRequestWithMissingCredential() throws Exception {
            mockMvc.perform(post("/auth/google/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing credential"));
        }

        @Test
        void returnsBadRequestWithBlankCredential() throws Exception {
            mockMvc.perform(post("/auth/google/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"credential\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing credential"));
        }

        @Test
        void returnsBadRequestWithInvalidGoogleToken() throws Exception {
            mockMvc.perform(post("/auth/google/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"credential\": \"fake-google-id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid")));
        }
    }

    @Nested
    class AuthLogoutEndpoint {

        @Test
        void returnsSuccessMessage() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    @Nested
    class SecurityFilterChain {

        @Test
        void authEndpointsArePublic() throws Exception {
            // /auth/** should be accessible without authentication
            mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
        }

        @Test
        void userEndpointsRequireAuthentication() throws Exception {
            mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
        }
    }
}
