package com.demo.sso.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.config.MicrosoftAuthProperties;
import java.util.List;
import java.util.Map;
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
            "google-client-id.apps.googleusercontent.com");

        ResponseEntity<Map<String, Object>> response = controller.getProviders();
        @SuppressWarnings("unchecked")
        Map<String, Object> google = (Map<String, Object>) response.getBody().get("google");
        @SuppressWarnings("unchecked")
        Map<String, Object> microsoft = (Map<String, Object>) response.getBody().get("microsoft");

        assertEquals("google-client-id.apps.googleusercontent.com", google.get("clientId"));
        assertTrue((Boolean) google.get("serverSideEnabled"));
        assertTrue((Boolean) google.get("clientSideEnabled"));
        assertFalse((Boolean) microsoft.get("clientSideEnabled"));
        assertFalse((Boolean) microsoft.get("serverSideEnabled"));
        assertEquals(List.of(), microsoft.get("scopes"));
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
            "google-client-id.apps.googleusercontent.com");

        ResponseEntity<Map<String, Object>> response = controller.getProviders();
        @SuppressWarnings("unchecked")
        Map<String, Object> google = (Map<String, Object>) response.getBody().get("google");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) response.getBody().get("microsoft");

        assertEquals("google-client-id.apps.googleusercontent.com", google.get("clientId"));
        assertTrue((Boolean) google.get("serverSideEnabled"));
        assertTrue((Boolean) google.get("clientSideEnabled"));
        assertTrue((Boolean) payload.get("clientSideEnabled"));
        assertEquals("microsoft-client-id", payload.get("clientId"));
        assertEquals("https://login.microsoftonline.com/common/v2.0", payload.get("authority"));
    }

    @Test
    void hardDisablesGoogleClientSideWhenPublicClientIdIsMissing() {
        ProviderConfigController controller = new ProviderConfigController(
            new AuthRolloutProperties(),
            new MicrosoftAuthProperties(),
            "");

        ResponseEntity<Map<String, Object>> response = controller.getProviders();
        @SuppressWarnings("unchecked")
        Map<String, Object> google = (Map<String, Object>) response.getBody().get("google");

        assertEquals("", google.get("clientId"));
        assertFalse((Boolean) google.get("serverSideEnabled"));
        assertFalse((Boolean) google.get("clientSideEnabled"));
    }
}