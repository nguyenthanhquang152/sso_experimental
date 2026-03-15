package com.demo.sso.controller;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.config.properties.MicrosoftAuthProperties;
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
        ProviderConfigResponse.GoogleProviderConfig googleConfig = buildGoogleConfig();
        ProviderConfigResponse.MicrosoftProviderConfig microsoftConfig = buildMicrosoftConfig();

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
        var googleRollout = rolloutProperties.getGoogle();
        boolean serverSideEnabled = configured && googleRollout.isServerSideEnabled();
        boolean clientSideEnabled = configured && googleRollout.isClientSideEnabled();

        if (!configured) {
            logger.debug("Google provider not configured: client ID is missing");
        } else if (!clientSideEnabled && !serverSideEnabled) {
            logger.debug("Google login suppressed by rollout flags (both client-side and server-side)");
        }

        return new ProviderConfigResponse.GoogleProviderConfig(serverSideEnabled, clientSideEnabled, googleClientId);
    }

    private ProviderConfigResponse.MicrosoftProviderConfig buildMicrosoftConfig() {
        var microsoftRollout = rolloutProperties.getMicrosoft();
        boolean clientSideConfigured = microsoftAuthProperties.isClientSideConfigured();
        boolean serverSideConfigured = microsoftAuthProperties.isServerSideConfigured();
        boolean serverSideEnabled = microsoftRollout.isServerSideEnabled() && serverSideConfigured;
        boolean clientSideEnabled = microsoftRollout.isClientSideEnabled() && clientSideConfigured;

        if (!clientSideConfigured && microsoftRollout.isClientSideEnabled()) {
            logger.warn("Microsoft client-side config missing despite rollout flag being enabled");
        }
        if (!serverSideConfigured && microsoftRollout.isServerSideEnabled()) {
            logger.warn("Microsoft server-side config missing despite rollout flag being enabled");
        }

        return new ProviderConfigResponse.MicrosoftProviderConfig(
            serverSideEnabled,
            clientSideEnabled,
            clientSideEnabled ? microsoftAuthProperties.getClientId() : null,
            clientSideEnabled ? microsoftAuthProperties.getAuthority() : null,
            clientSideEnabled ? List.copyOf(microsoftAuthProperties.getScopes()) : List.of(),
            clientSideEnabled ? frontendUrl : null
        );
    }
}