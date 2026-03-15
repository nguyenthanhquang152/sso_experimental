package com.demo.sso.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Must stay in sync with the frontend {@code ProviderConfig} TypeScript type.
 */
public record ProviderConfigResponse(
        GoogleProviderConfig google,
        MicrosoftProviderConfig microsoft) {

    public record GoogleProviderConfig(
            boolean serverSideEnabled,
            boolean clientSideEnabled,
            @Nullable String clientId) {
    }

    /**
     * When {@code clientSideEnabled} is false, optional MSAL fields are null/empty
     * and the frontend must not initialise MSAL.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MicrosoftProviderConfig(
            boolean serverSideEnabled,
            boolean clientSideEnabled,
            @Nullable String clientId,
            @Nullable String authority,
            List<String> scopes,
            @Nullable String redirectUri) {
    }
}