package com.demo.sso.controller;

import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.config.MicrosoftAuthProperties;
import com.demo.sso.controller.dto.ProviderConfigResponse;
import java.util.List;
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
    public ResponseEntity<ProviderConfigResponse> getProviders() {
        boolean googleConfigured = !googleClientId.isBlank();
        boolean microsoftClientSideConfigured = microsoftAuthProperties.isClientSideConfigured();
        boolean microsoftServerSideConfigured = microsoftAuthProperties.isServerSideConfigured();
        boolean microsoftServerSideEnabled = rolloutProperties.getMicrosoft().isServerSideEnabled()
            && microsoftServerSideConfigured;
        boolean microsoftClientSideEnabled = rolloutProperties.getMicrosoft().isClientSideEnabled()
            && microsoftClientSideConfigured;

        ProviderConfigResponse response = new ProviderConfigResponse(
            new ProviderConfigResponse.GoogleProviderConfig(
                googleConfigured,
                googleConfigured,
                googleClientId
            ),
            new ProviderConfigResponse.MicrosoftProviderConfig(
                microsoftServerSideEnabled,
                microsoftClientSideEnabled,
                microsoftClientSideEnabled ? microsoftAuthProperties.getClientId() : null,
                microsoftClientSideEnabled ? microsoftAuthProperties.getAuthority() : null,
                microsoftClientSideEnabled ? List.copyOf(microsoftAuthProperties.getScopes()) : List.of()
            )
        );

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }
}