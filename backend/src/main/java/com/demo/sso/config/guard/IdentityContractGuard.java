package com.demo.sso.config.guard;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.config.properties.MicrosoftAuthProperties;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class IdentityContractGuard {

    private final AuthRolloutProperties rolloutProperties;
    private final MicrosoftAuthProperties microsoftAuthProperties;

    public IdentityContractGuard(
            AuthRolloutProperties rolloutProperties,
            MicrosoftAuthProperties microsoftAuthProperties) {
        this.rolloutProperties = rolloutProperties;
        this.microsoftAuthProperties = microsoftAuthProperties;
    }

    @PostConstruct
    void validate() {
        AuthRolloutProperties.IdentityContractMode contractMode = rolloutProperties.getIdentityContractMode();
        AuthRolloutProperties.JwtMintMode mintMode = rolloutProperties.getJwtMintMode();

        if (contractMode == AuthRolloutProperties.IdentityContractMode.LEGACY_ONLY
                && mintMode == AuthRolloutProperties.JwtMintMode.V2) {
            throw new IllegalStateException("LEGACY_ONLY cannot mint V2 JWT/auth-code artifacts");
        }

        if (contractMode == AuthRolloutProperties.IdentityContractMode.V2_ONLY
                && mintMode == AuthRolloutProperties.JwtMintMode.LEGACY) {
            throw new IllegalStateException("V2_ONLY cannot mint legacy JWT/auth-code artifacts");
        }

        if ((rolloutProperties.getMicrosoft().isClientSideEnabled()
                || rolloutProperties.getMicrosoft().isServerSideEnabled())
                && contractMode != AuthRolloutProperties.IdentityContractMode.V2_ONLY) {
            throw new IllegalStateException("Microsoft SSO requires identity-contract-mode=V2_ONLY");
        }

        if (rolloutProperties.getMicrosoft().isClientSideEnabled()
                && !microsoftAuthProperties.isClientSideConfigured()) {
            throw new IllegalStateException("Microsoft SSO requires configured client-id and authority");
        }

        if (rolloutProperties.getMicrosoft().isServerSideEnabled()
                && !microsoftAuthProperties.isServerSideConfigured()) {
            throw new IllegalStateException("Microsoft server-side SSO requires configured client-id, client-secret, and authority");
        }
    }
}