package com.demo.sso.controller;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.sso.config.TestAuthCodeStoreConfig;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.repository.UserRepository;
import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.JwtTokenService;
import com.demo.sso.service.MicrosoftTokenVerifier;
import com.demo.sso.service.NormalizedIdentity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    "app.auth.identity-contract-mode=v2_only",
    "app.auth.jwt-mint-mode=v2",
    "app.auth.microsoft.client-side-enabled=true",
    "app.microsoft.client-id=test-microsoft-client-id",
    "app.microsoft.authority=https://login.microsoftonline.com/common/v2.0",
    "app.microsoft.scopes=openid,profile,email",
    "spring.datasource.url=jdbc:h2:mem:microsoftclientdb;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(TestAuthCodeStoreConfig.class)
class MicrosoftClientSideIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockBean
    private MicrosoftTokenVerifier microsoftTokenVerifier;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void providerConfigReportsEnabledMicrosoftClientFlow() throws Exception {
        mockMvc.perform(get("/auth/providers"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(jsonPath("$.microsoft.clientSideEnabled").value(true))
            .andExpect(jsonPath("$.microsoft.clientId").value("test-microsoft-client-id"))
            .andExpect(jsonPath("$.microsoft.authority").value("https://login.microsoftonline.com/common/v2.0"))
            .andExpect(jsonPath("$.microsoft.scopes[0]").value("openid"));
    }

    @Test
    void microsoftChallengeAndVerifyIssueV2TokenForMicrosoftIdentity() throws Exception {
        MvcResult challengeResult = mockMvc.perform(post("/auth/microsoft/challenge"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth/microsoft")))
            .andExpect(cookie().exists("ms_challenge_session"))
            .andExpect(jsonPath("$.challengeId", not(blankOrNullString())))
            .andExpect(jsonPath("$.nonce", not(blankOrNullString())))
            .andReturn();

        Map<String, String> challenge = objectMapper.readValue(
            challengeResult.getResponse().getContentAsByteArray(),
            new TypeReference<>() {}
        );
        String sessionCookie = challengeResult.getResponse().getCookie("ms_challenge_session").getValue();

        when(microsoftTokenVerifier.verifyIdToken(
            eq("valid-microsoft-id-token"),
            eq(challenge.get("nonce")),
            eq(AuthFlow.CLIENT_SIDE)))
            .thenReturn(NormalizedIdentity.microsoft(
                "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0|microsoft-subject",
                "employee@example.com",
                "Microsoft Employee",
                null,
                AuthFlow.CLIENT_SIDE));

        MvcResult verifyResult = mockMvc.perform(post("/auth/microsoft/verify")
                .cookie(challengeResult.getResponse().getCookie("ms_challenge_session"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"valid-microsoft-id-token\",\"challengeId\":\""
                    + challenge.get("challengeId") + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", not(blankOrNullString())))
            .andReturn();

        Map<String, String> response = objectMapper.readValue(
            verifyResult.getResponse().getContentAsByteArray(),
            new TypeReference<>() {}
        );
        String token = response.get("token");
        assertNotNull(token);

        var claims = jwtTokenService.parseToken(token);
        assertEquals(2, claims.get("ver", Integer.class));
        assertEquals("MICROSOFT", claims.get("provider", String.class));
        assertEquals(
            "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0|microsoft-subject",
            claims.get("providerUserId", String.class));
        assertEquals("employee@example.com", claims.get("email", String.class));

        var savedUser = userRepository.findByProviderAndProviderUserId(
            AuthProvider.MICROSOFT,
            "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0|microsoft-subject"
        ).orElseThrow();
        assertEquals("employee@example.com", savedUser.getEmail());
        assertEquals(AuthFlow.CLIENT_SIDE, savedUser.getLastLoginFlow());
        assertEquals(sessionCookie, challengeResult.getResponse().getCookie("ms_challenge_session").getValue());
    }
}