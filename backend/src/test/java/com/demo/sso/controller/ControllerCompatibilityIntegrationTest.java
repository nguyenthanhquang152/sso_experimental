package com.demo.sso.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.sso.config.TestAuthCodeStoreConfig;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import com.demo.sso.service.token.GoogleTokenVerifier;
import com.demo.sso.service.token.JwtTokenService;
import com.demo.sso.service.model.VerifiedGoogleIdentity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "app.auth.identity-contract-mode=compatibility",
    "app.auth.jwt-mint-mode=v2"
})
@AutoConfigureMockMvc
@Import(TestAuthCodeStoreConfig.class)
class ControllerCompatibilityIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void userMeResolvesV2TokenByInternalUserId() throws Exception {
        User user = new User();
        user.setGoogleId("google-test-123");
        user.setEmail("test@example.com");
        user.setName("Compatibility User");
        user.setPictureUrl("http://example.com/pic.jpg");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-test-123");
        user.setLastLoginFlow(AuthFlow.SERVER_SIDE);
        user.setCreatedAt(Instant.now());
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);

        String token = jwtTokenService.generateToken(user);

        mockMvc.perform(get("/user/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.provider").value("GOOGLE"))
            .andExpect(jsonPath("$.providerUserId").value("google-test-123"))
            .andExpect(jsonPath("$.lastLoginFlow").value("SERVER_SIDE"));
    }

    @Test
    void googleVerifyReturnsV2TokenDuringCompatibilityMode() throws Exception {
        VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(
            "google-123", "user@example.com", true, "Google User", "http://example.com/google.png");
        when(googleTokenVerifier.verifyIdToken("valid-google-id-token")).thenReturn(identity);

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
        assertEquals(2, claims.get("ver", Integer.class));
        assertEquals("GOOGLE", claims.get("provider", String.class));
        assertEquals("google-123", claims.get("providerUserId", String.class));
        assertEquals("user@example.com", claims.get("email", String.class));
        assertFalse(claims.containsKey("googleId"));
    }
}