package com.demo.sso.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.sso.config.TestAuthCodeStoreConfig;
import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.MicrosoftTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.auth.identity-contract-mode=v2_only",
    "app.auth.jwt-mint-mode=v2",
    "app.auth.microsoft.server-side-enabled=true",
    "app.microsoft.client-id=test-microsoft-client-id",
    "app.microsoft.client-secret=test-microsoft-client-secret",
    "app.microsoft.authority=https://login.microsoftonline.com/common/v2.0",
    "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-client-id",
    "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-client-secret",
    "spring.datasource.url=jdbc:h2:mem:microsoftserversidedb;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(TestAuthCodeStoreConfig.class)
class MicrosoftServerSideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockBean
    private MicrosoftTokenVerifier microsoftTokenVerifier;

    @Test
    void providerConfigReportsEnabledMicrosoftServerSideFlow() throws Exception {
        mockMvc.perform(get("/auth/providers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.microsoft.serverSideEnabled").value(true));
    }

    @Test
    void microsoftAuthorizationEndpointRedirectsToMicrosoftIdentityPlatformWhenConfigured() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/microsoft"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("https://login.microsoftonline.com/")))
            .andExpect(header().string("Location", containsString("client_id=test-microsoft-client-id")));
    }
}