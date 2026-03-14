package com.demo.sso.controller;

import com.demo.sso.config.TestAuthCodeStoreConfig;
import com.demo.sso.model.User;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.repository.UserRepository;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.token.GoogleTokenVerifier;
import com.demo.sso.service.token.JwtTokenService;
import com.demo.sso.service.token.MicrosoftTokenVerifier;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestAuthCodeStoreConfig.class)
class ControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthCodeStore authCodeStore;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockBean
    private MicrosoftTokenVerifier microsoftTokenVerifier;

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
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-test-123");
        user.setLastLoginFlow(AuthFlow.SERVER_SIDE);
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
            String token = jwtTokenService.generateToken(user);

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
            User phantom = new User();
            phantom.setId(99999L);
            phantom.setEmail("nonexistent@example.com");
            phantom.setProvider(AuthProvider.GOOGLE);
            phantom.setProviderUserId("google-999");
            String token = jwtTokenService.generateToken(phantom);

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
            when(googleTokenVerifier.verify("fake-google-id-token"))
                .thenThrow(new IllegalArgumentException("Invalid Google credential"));

            mockMvc.perform(post("/auth/google/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"credential\": \"fake-google-id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Google credential"));
        }

        @Test
        void returnsTokenAndDualWritesProviderFieldsForValidGoogleToken() throws Exception {
            VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(
                "google-123", "user@example.com", true, "Google User", "http://example.com/google.png");

            when(googleTokenVerifier.verify("valid-google-id-token")).thenReturn(identity);

            MvcResult result = mockMvc.perform(post("/auth/google/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"credential\": \"valid-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andReturn();

            Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {}
            );
            String token = response.get("token");
            assertNotNull(token);

            var claims = jwtTokenService.parseToken(token);
            assertEquals("user@example.com", claims.getSubject());
            assertEquals("google-123", claims.get("googleId", String.class));

            User savedUser = userRepository.findByGoogleId("google-123").orElseThrow();
            assertEquals(AuthProvider.GOOGLE, savedUser.getProvider());
            assertEquals("google-123", savedUser.getProviderUserId());
            assertEquals(AuthFlow.CLIENT_SIDE, savedUser.getLastLoginFlow());
            assertEquals("Google User", savedUser.getName());
            assertEquals("user@example.com", savedUser.getEmail());
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
            User user = createTestUser();
            String jwt = jwtTokenService.generateToken(user);
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
    class MicrosoftDisabledContracts {

        @Test
        void providerConfigReportsMicrosoftDisabledByDefault() throws Exception {
            mockMvc.perform(get("/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.google.serverSideEnabled").value(true))
                .andExpect(jsonPath("$.google.clientSideEnabled").value(true))
                .andExpect(jsonPath("$.microsoft.serverSideEnabled").value(false))
                .andExpect(jsonPath("$.microsoft.clientSideEnabled").value(false))
                .andExpect(jsonPath("$.microsoft.clientId").doesNotExist())
                .andExpect(jsonPath("$.microsoft.authority").doesNotExist())
                .andExpect(jsonPath("$.microsoft.scopes", hasSize(0)));
        }

        @Test
        void microsoftChallengeReturns503WhenClientSideFlowDisabled() throws Exception {
            mockMvc.perform(post("/auth/microsoft/challenge"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Microsoft client-side login is disabled"));
        }

        @Test
        void microsoftVerifyReturns503WhenClientSideFlowDisabled() throws Exception {
            mockMvc.perform(post("/auth/microsoft/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"credential\":\"token\",\"challengeId\":\"challenge\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Microsoft client-side login is disabled"));
        }

        @Test
        void microsoftAuthorizationEntryReturns503WhenServerSideFlowDisabled() throws Exception {
            mockMvc.perform(get("/oauth2/authorization/microsoft"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"error\":\"Microsoft server-side login is disabled\"}"));
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
