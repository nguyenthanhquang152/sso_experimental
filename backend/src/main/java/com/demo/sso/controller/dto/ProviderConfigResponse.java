package com.demo.sso.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ProviderConfigResponse(
        GoogleProviderConfig google,
        MicrosoftProviderConfig microsoft) {

    public record GoogleProviderConfig(
            boolean serverSideEnabled,
            boolean clientSideEnabled,
            String clientId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MicrosoftProviderConfig(
            boolean serverSideEnabled,
            boolean clientSideEnabled,
            String clientId,
            String authority,
            List<String> scopes) {
    }
}