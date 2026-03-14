package com.demo.sso.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.config.MicrosoftAuthProperties;
import com.demo.sso.controller.dto.ProviderConfigResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ProviderConfigControllerTest {

    @Test
    void hardDisablesMicrosoftWhenFlagsAreOnButPublicConfigIsMissing() {
        AuthRolloutProperties rollout = new AuthRolloutProperties();
        rollout.getMicrosoft().setClientSideEnabled(true);
        rollout.getMicrosoft().setServerSideEnabled(true);

        ProviderConfigController controller = new ProviderConfigController(
            rollout,
            new MicrosoftAuthProperties(),
            "google-client-id.apps.googleusercontent.com",
            "http://localhost:8000");

        ResponseEntity<ProviderConfigResponse> response = controller.getProviders();
        ProviderConfigResponse.GoogleProviderConfig google = response.getBody().google();
        ProviderConfigResponse.MicrosoftProviderConfig microsoft = response.getBody().microsoft();

        assertEquals("google-client-id.apps.googleusercontent.com", google.clientId());
        assertTrue(google.serverSideEnabled());
        assertTrue(google.clientSideEnabled());
        assertFalse(microsoft.clientSideEnabled());
        assertFalse(microsoft.serverSideEnabled());
        assertEquals(List.of(), microsoft.scopes());
    }

    @Test
    void reportsMicrosoftWhenFlagsAndPublicConfigArePresent() {
        AuthRolloutProperties rollout = new AuthRolloutProperties();
        rollout.getMicrosoft().setClientSideEnabled(true);

        MicrosoftAuthProperties microsoft = new MicrosoftAuthProperties();
        microsoft.setClientId("microsoft-client-id");
        microsoft.setAuthority("https://login.microsoftonline.com/common/v2.0");
        microsoft.setScopes(List.of("openid", "profile", "email"));

        ProviderConfigController controller = new ProviderConfigController(
            rollout,
            microsoft,
            "google-client-id.apps.googleusercontent.com",
            "http://localhost:8000");

        ResponseEntity<ProviderConfigResponse> response = controller.getProviders();
        ProviderConfigResponse.GoogleProviderConfig google = response.getBody().google();
        ProviderConfigResponse.MicrosoftProviderConfig payload = response.getBody().microsoft();

        assertEquals("google-client-id.apps.googleusercontent.com", google.clientId());
        assertTrue(google.serverSideEnabled());
        assertTrue(google.clientSideEnabled());
        assertTrue(payload.clientSideEnabled());
        assertEquals("microsoft-client-id", payload.clientId());
        assertEquals("https://login.microsoftonline.com/common/v2.0", payload.authority());
        assertEquals("http://localhost:8000", payload.redirectUri());
    }

    @Test
    void hardDisablesMicrosoftServerSideWhenSecretIsMissing() {
        AuthRolloutProperties rollout = new AuthRolloutProperties();
        rollout.getMicrosoft().setClientSideEnabled(true);
        rollout.getMicrosoft().setServerSideEnabled(true);

        MicrosoftAuthProperties microsoft = new MicrosoftAuthProperties();
        microsoft.setClientId("microsoft-client-id");
        microsoft.setAuthority("https://login.microsoftonline.com/common/v2.0");
        microsoft.setScopes(List.of("openid", "profile", "email"));

        ProviderConfigController controller = new ProviderConfigController(
            rollout,
            microsoft,
            "google-client-id.apps.googleusercontent.com",
            "http://localhost:8000");

        ResponseEntity<ProviderConfigResponse> response = controller.getProviders();
        ProviderConfigResponse.MicrosoftProviderConfig payload = response.getBody().microsoft();

        assertFalse(payload.serverSideEnabled());
        assertTrue(payload.clientSideEnabled());
    }

    @Test
    void hardDisablesGoogleClientSideWhenPublicClientIdIsMissing() {
        ProviderConfigController controller = new ProviderConfigController(
            new AuthRolloutProperties(),
            new MicrosoftAuthProperties(),
            "",
            "http://localhost:8000");

        ResponseEntity<ProviderConfigResponse> response = controller.getProviders();
        ProviderConfigResponse.GoogleProviderConfig google = response.getBody().google();

        assertEquals("", google.clientId());
        assertFalse(google.serverSideEnabled());
        assertFalse(google.clientSideEnabled());
    }

    @Test
    void suppressesGoogleClientSideWhenRolloutFlagIsDisabled() {
        AuthRolloutProperties rollout = new AuthRolloutProperties();
        rollout.getGoogle().setClientSideEnabled(false);

        ProviderConfigController controller = new ProviderConfigController(
            rollout,
            new MicrosoftAuthProperties(),
            "google-client-id.apps.googleusercontent.com",
            "http://localhost:8000");

        ResponseEntity<ProviderConfigResponse> response = controller.getProviders();
        ProviderConfigResponse.GoogleProviderConfig google = response.getBody().google();

        assertEquals("google-client-id.apps.googleusercontent.com", google.clientId());
        assertTrue(google.serverSideEnabled());
        assertFalse(google.clientSideEnabled());
    }
}