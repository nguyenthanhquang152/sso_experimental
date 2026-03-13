package com.demo.sso.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IdentityContractGuardTest {

    @Test
    void rejectsLegacyOnlyWithV2MintMode() {
        AuthRolloutProperties properties = rolloutProperties(
            AuthRolloutProperties.IdentityContractMode.LEGACY_ONLY,
            AuthRolloutProperties.JwtMintMode.V2,
            false,
            false
        );

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> new IdentityContractGuard(properties, microsoftProperties(true)).validate());

        assertTrue(error.getMessage().contains("LEGACY_ONLY cannot mint V2 JWT/auth-code artifacts"));
    }

    @Test
    void rejectsV2OnlyWithLegacyMintMode() {
        AuthRolloutProperties properties = rolloutProperties(
            AuthRolloutProperties.IdentityContractMode.V2_ONLY,
            AuthRolloutProperties.JwtMintMode.LEGACY,
            false,
            false
        );

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> new IdentityContractGuard(properties, microsoftProperties(true)).validate());

        assertTrue(error.getMessage().contains("V2_ONLY cannot mint legacy JWT/auth-code artifacts"));
    }

    @Test
    void rejectsMicrosoftEnablementUntilV2Only() {
        AuthRolloutProperties properties = rolloutProperties(
            AuthRolloutProperties.IdentityContractMode.COMPATIBILITY,
            AuthRolloutProperties.JwtMintMode.V2,
            true,
            false
        );

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> new IdentityContractGuard(properties, microsoftProperties(true)).validate());

        assertTrue(error.getMessage().contains("Microsoft SSO requires identity-contract-mode=V2_ONLY"));
    }

    @Test
    void allowsCompatibilityModesAndDisabledMicrosoft() {
        AuthRolloutProperties properties = rolloutProperties(
            AuthRolloutProperties.IdentityContractMode.COMPATIBILITY,
            AuthRolloutProperties.JwtMintMode.V2,
            false,
            false
        );

        assertDoesNotThrow(() -> new IdentityContractGuard(properties, microsoftProperties(true)).validate());
    }

    @Test
    void rejectsMicrosoftEnablementWhenClientConfigIsMissing() {
        AuthRolloutProperties properties = rolloutProperties(
            AuthRolloutProperties.IdentityContractMode.V2_ONLY,
            AuthRolloutProperties.JwtMintMode.V2,
            true,
            false
        );

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> new IdentityContractGuard(properties, new MicrosoftAuthProperties()).validate());

        assertTrue(error.getMessage().contains("Microsoft SSO requires configured client-id and authority"));
    }

    private static AuthRolloutProperties rolloutProperties(
            AuthRolloutProperties.IdentityContractMode contractMode,
            AuthRolloutProperties.JwtMintMode mintMode,
            boolean microsoftClientSideEnabled,
            boolean microsoftServerSideEnabled) {
        AuthRolloutProperties properties = new AuthRolloutProperties();
        properties.setIdentityContractMode(contractMode);
        properties.setJwtMintMode(mintMode);
        properties.getMicrosoft().setClientSideEnabled(microsoftClientSideEnabled);
        properties.getMicrosoft().setServerSideEnabled(microsoftServerSideEnabled);
        return properties;
    }

    private static MicrosoftAuthProperties microsoftProperties(boolean configured) {
        MicrosoftAuthProperties properties = new MicrosoftAuthProperties();
        if (configured) {
            properties.setClientId("microsoft-client-id");
            properties.setAuthority("https://login.microsoftonline.com/common/v2.0");
        }
        return properties;
    }
}