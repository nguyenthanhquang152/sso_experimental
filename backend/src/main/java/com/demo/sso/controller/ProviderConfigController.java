package com.demo.sso.controller;

import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.config.MicrosoftAuthProperties;
import com.demo.sso.controller.dto.ProviderConfigResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class ProviderConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderConfigController.class);

    private final AuthRolloutProperties rolloutProperties;
    private final MicrosoftAuthProperties microsoftAuthProperties;
    private final String googleClientId;
    private final String frontendUrl;

    public ProviderConfigController(
            AuthRolloutProperties rolloutProperties,
            MicrosoftAuthProperties microsoftAuthProperties,
            @Value("${app.google-client-id:}") String googleClientId,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.rolloutProperties = rolloutProperties;
        this.microsoftAuthProperties = microsoftAuthProperties;
        this.googleClientId = googleClientId == null ? "" : googleClientId;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/providers")
    public ResponseEntity<ProviderConfigResponse> getProviders() {
        ProviderConfigResponse.GoogleProviderConfig googleConfig;
        try {
            googleConfig = buildGoogleConfig();
        } catch (RuntimeException e) {
            logger.error("Failed to build Google provider config, returning disabled fallback", e);
            googleConfig = new ProviderConfigResponse.GoogleProviderConfig(false, false, "");
        }

        ProviderConfigResponse.MicrosoftProviderConfig microsoftConfig;
        try {
            microsoftConfig = buildMicrosoftConfig();
        } catch (RuntimeException e) {
            logger.error("Failed to build Microsoft provider config, returning disabled fallback", e);
            microsoftConfig = new ProviderConfigResponse.MicrosoftProviderConfig(false, false, null, null, List.of(), null);
        }

        if (!googleConfig.serverSideEnabled() && !googleConfig.clientSideEnabled()
                && !microsoftConfig.serverSideEnabled() && !microsoftConfig.clientSideEnabled()) {
            logger.warn("All provider authentication flows are disabled — no login method is available");
        }

        ProviderConfigResponse response = new ProviderConfigResponse(googleConfig, microsoftConfig);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }

    private ProviderConfigResponse.GoogleProviderConfig buildGoogleConfig() {
        boolean configured = !googleClientId.isBlank();
        boolean serverSideEnabled = configured && rolloutProperties.getGoogle().isServerSideEnabled();
        boolean clientSideEnabled = configured && rolloutProperties.getGoogle().isClientSideEnabled();

        if (configured && !clientSideEnabled) {
            logger.warn("Google client-side login suppressed by rollout flag");
        }
        if (configured && !serverSideEnabled) {
            logger.warn("Google server-side login suppressed by rollout flag");
        }
        if (!configured) {
            logger.warn("Google provider not configured: client ID is missing");
        }

        return new ProviderConfigResponse.GoogleProviderConfig(serverSideEnabled, clientSideEnabled, googleClientId);
    }

    private ProviderConfigResponse.MicrosoftProviderConfig buildMicrosoftConfig() {
        boolean clientSideConfigured = microsoftAuthProperties.isClientSideConfigured();
        boolean serverSideConfigured = microsoftAuthProperties.isServerSideConfigured();
        boolean serverSideEnabled = rolloutProperties.getMicrosoft().isServerSideEnabled() && serverSideConfigured;
        boolean clientSideEnabled = rolloutProperties.getMicrosoft().isClientSideEnabled() && clientSideConfigured;

        ProviderConfigResponse.MicrosoftProviderConfig config = new ProviderConfigResponse.MicrosoftProviderConfig(
            serverSideEnabled,
            clientSideEnabled,
            clientSideEnabled ? microsoftAuthProperties.getClientId() : null,
            clientSideEnabled ? microsoftAuthProperties.getAuthority() : null,
            clientSideEnabled ? List.copyOf(microsoftAuthProperties.getScopes()) : List.of(),
            clientSideEnabled ? frontendUrl : null
        );

        if (!clientSideConfigured && rolloutProperties.getMicrosoft().isClientSideEnabled()) {
            logger.warn("Microsoft client-side config missing despite rollout flag being enabled");
        }
        if (!serverSideConfigured && rolloutProperties.getMicrosoft().isServerSideEnabled()) {
            logger.warn("Microsoft server-side config missing despite rollout flag being enabled");
        }

        return config;
    }
}