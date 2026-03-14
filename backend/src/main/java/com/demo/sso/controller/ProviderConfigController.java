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
        boolean googleConfigured = !googleClientId.isBlank();
        boolean googleServerSideEnabled = googleConfigured
            && rolloutProperties.getGoogle().isServerSideEnabled();
        boolean googleClientSideEnabled = googleConfigured
            && rolloutProperties.getGoogle().isClientSideEnabled();
        boolean microsoftClientSideConfigured = microsoftAuthProperties.isClientSideConfigured();
        boolean microsoftServerSideConfigured = microsoftAuthProperties.isServerSideConfigured();
        boolean microsoftServerSideEnabled = rolloutProperties.getMicrosoft().isServerSideEnabled()
            && microsoftServerSideConfigured;
        boolean microsoftClientSideEnabled = rolloutProperties.getMicrosoft().isClientSideEnabled()
            && microsoftClientSideConfigured;

        if (googleConfigured && !googleClientSideEnabled) {
            logger.warn("Google client-side login suppressed by rollout flag");
        }
        if (googleConfigured && !googleServerSideEnabled) {
            logger.warn("Google server-side login suppressed by rollout flag");
        }
        if (!googleConfigured) {
            logger.warn("Google provider not configured: client ID is missing");
        }
        if (!microsoftClientSideConfigured && rolloutProperties.getMicrosoft().isClientSideEnabled()) {
            logger.warn("Microsoft client-side config missing despite rollout flag being enabled");
        }
        if (!microsoftServerSideConfigured && rolloutProperties.getMicrosoft().isServerSideEnabled()) {
            logger.warn("Microsoft server-side config missing despite rollout flag being enabled");
        }

        ProviderConfigResponse response = new ProviderConfigResponse(
            new ProviderConfigResponse.GoogleProviderConfig(
                googleServerSideEnabled,
                googleClientSideEnabled,
                googleClientId
            ),
            new ProviderConfigResponse.MicrosoftProviderConfig(
                microsoftServerSideEnabled,
                microsoftClientSideEnabled,
                microsoftClientSideEnabled ? microsoftAuthProperties.getClientId() : null,
                microsoftClientSideEnabled ? microsoftAuthProperties.getAuthority() : null,
                microsoftClientSideEnabled ? List.copyOf(microsoftAuthProperties.getScopes()) : List.of(),
                microsoftClientSideEnabled ? frontendUrl : null
            )
        );

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }
}