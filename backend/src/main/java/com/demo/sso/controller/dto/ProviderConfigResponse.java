package com.demo.sso.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Runtime provider configuration contract returned by {@code GET /auth/providers}.
 *
 * <p>This is the single source of truth the frontend uses to decide which SSO
 * providers and flows to render. The frontend {@code ProviderConfig} TypeScript
 * type in {@code frontend/src/types/auth.ts} must stay in sync with this record.
 *
 * <p>Responses are served with {@code Cache-Control: no-store} so the frontend
 * always reflects the current backend feature-flag state.
 */
public record ProviderConfigResponse(
        GoogleProviderConfig google,
        MicrosoftProviderConfig microsoft) {

    /** Google provider availability and public client ID for the browser. */
    public record GoogleProviderConfig(
            boolean serverSideEnabled,
            boolean clientSideEnabled,
            String clientId) {
    }

    /**
     * Microsoft provider availability and public MSAL configuration.
     *
     * <p>When {@code clientSideEnabled} is {@code false}, optional fields
     * ({@code clientId}, {@code authority}, {@code redirectUri}) are {@code null}
     * and {@code scopes} is empty. The frontend must not initialise MSAL in
     * that state.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MicrosoftProviderConfig(
            boolean serverSideEnabled,
            boolean clientSideEnabled,
            String clientId,
            String authority,
            List<String> scopes,
            String redirectUri) {
    }
}