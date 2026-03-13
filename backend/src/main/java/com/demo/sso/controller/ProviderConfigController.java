package com.demo.sso.controller;

import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.config.MicrosoftAuthProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class ProviderConfigController {

    private final AuthRolloutProperties rolloutProperties;
    private final MicrosoftAuthProperties microsoftAuthProperties;
    private final String googleClientId;

    public ProviderConfigController(
            AuthRolloutProperties rolloutProperties,
            MicrosoftAuthProperties microsoftAuthProperties,
            @Value("${app.google-client-id:}") String googleClientId) {
        this.rolloutProperties = rolloutProperties;
        this.microsoftAuthProperties = microsoftAuthProperties;
        this.googleClientId = googleClientId == null ? "" : googleClientId;
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        boolean googleConfigured = googleClientId != null && !googleClientId.isBlank();
        boolean microsoftConfigured = microsoftAuthProperties.isConfigured();
        boolean microsoftServerSideEnabled = rolloutProperties.getMicrosoft().isServerSideEnabled() && microsoftConfigured;
        boolean microsoftClientSideEnabled = rolloutProperties.getMicrosoft().isClientSideEnabled() && microsoftConfigured;

        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> google = new LinkedHashMap<>();
        google.put("serverSideEnabled", googleConfigured);
        google.put("clientSideEnabled", googleConfigured);
        google.put("clientId", googleClientId);
        response.put("google", google);

        Map<String, Object> microsoft = new LinkedHashMap<>();
        microsoft.put("serverSideEnabled", microsoftServerSideEnabled);
        microsoft.put("clientSideEnabled", microsoftClientSideEnabled);
        microsoft.put("scopes", microsoftClientSideEnabled
            ? List.copyOf(microsoftAuthProperties.getScopes())
            : List.of());

        if (microsoftClientSideEnabled) {
            microsoft.put("clientId", microsoftAuthProperties.getClientId());
            microsoft.put("authority", microsoftAuthProperties.getAuthority());
        }

        response.put("microsoft", microsoft);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }
}