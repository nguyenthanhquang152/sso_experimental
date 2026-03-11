package com.demo.sso.controller;

import com.demo.sso.config.TestAuthCodeStoreConfig;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import com.demo.sso.service.AuthCodeStore;
import com.demo.sso.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestAuthCodeStoreConfig.class)
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthCodeStore authCodeStore;

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
        user.setCreatedAt(Instant.now());
        user.setLastLoginAt(Instant.now());
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
            String token = jwtTokenService.generateToken(user.getEmail(), user.getGoogleId());

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
            String token = jwtTokenService.generateToken("nonexistent@example.com", "google-999");

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
                .andExpect(jsonPath("$.error").value("Invalid Google credential"));
        }
    }

    @Nested
    class AuthExchangeEndpoint {

        @Test
        void returnsBadRequestWithMissingCode() throws Exception {
            mockMvc.perform(post("/auth/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing code"));
        }

        @Test
        void returnsBadRequestWithInvalidCode() throws Exception {
            mockMvc.perform(post("/auth/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"nonexistent-code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired code"));
        }

        @Test
        void returnsTokenForValidCode() throws Exception {
            String jwt = jwtTokenService.generateToken("test@example.com", "google-123");
            String code = authCodeStore.storeJwt(jwt);

            mockMvc.perform(post("/auth/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(jwt));
        }

        @Test
        void codeCannotBeReused() throws Exception {
            String code = authCodeStore.storeJwt("some.jwt");

            // First use succeeds
            mockMvc.perform(post("/auth/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isOk());

            // Second use fails
            mockMvc.perform(post("/auth/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired code"));
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
    class SecurityFilterChainTests {

        @Test
        void authEndpointsArePublic() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
        }

        @Test
        void userEndpointsRequireAuthentication() throws Exception {
            mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void bearerWithoutTokenIsIgnored() throws Exception {
            mockMvc.perform(get("/user/me")
                    .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void basicAuthHeaderIsIgnored() throws Exception {
            mockMvc.perform(get("/user/me")
                    .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
        }
    }
}
